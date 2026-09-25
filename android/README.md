# OAuth2 TOTP Authenticator - Android App

An Android authenticator app that generates TOTP (Time-based One-Time Password)
codes compatible with the OAuth2 Authorization Server and Google Authenticator.

## Features

- **TOTP Code Generation** — Generates 6-digit codes using RFC 6238 (TOTP)
- **Account Management** — Add, view, and remove TOTP accounts
- **QR Code Support** — Parse otpauth:// URIs for easy setup
- **Countdown Timer** — Visual countdown showing seconds until code refresh
- **Manual Entry** — Enter secret keys manually or generate new ones
- **Signed Release APK** — Pre-built, signed, and ready to install

## Download & Install

The signed release APK is available at:

`
app/build/outputs/apk/release/app-release.apk
`

**To install on a device:**
1. Transfer the APK to your Android device
2. Enable "Install from unknown sources" in Settings → Security
3. Open the APK file and tap Install

The APK is signed with APK Signature Scheme v2 (verified).

## Project Structure

`
android/
├── app/
│   ├── build.gradle.kts          # App-level build configuration (includes signing)
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest
│       ├── java/com/oauth/otp/
│       │   ├── MainActivity.kt           # Main screen with account list
│       │   ├── AddAccountActivity.kt     # Add new TOTP account
│       │   ├── TotpGenerator.kt          # TOTP generation logic (RFC 6238)
│       │   ├── AccountStorage.kt         # SharedPreferences storage
│       │   └── AccountAdapter.kt         # RecyclerView adapter
│       └── res/
│           ├── layout/           # XML layouts
│           ├── drawable/         # Vector drawables (launcher icon)
│           ├── values/           # Strings, colors, themes
│           └── menu/             # Options menu
├── build.gradle.kts              # Project-level build configuration
├── settings.gradle.kts           # Gradle settings
├── gradle.properties             # Project properties (AndroidX flags)
├── oauth2-release.keystore       # Signing keystore (demo use only)
└── gradle/wrapper/               # Gradle wrapper
`

## Signing Key

A development keystore was generated for signing the release APK:

| Property | Value |
|----------|-------|
| Keystore file | oauth2-release.keystore |
| Key alias | oauth2-release |
| Store password | oauth2pass |
| Key password | oauth2pass |
| Algorithm | RSA 2048-bit |
| Validity | ~27 years |
| DN | CN=OAuth2 Server, OU=Development, O=Dev |

**Generating your own keystore for production:**

`ash
keytool -genkeypair -v -keystore my-release-key.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
`

Then update pp/build.gradle.kts:

`kotlin
signingConfigs {
    create("release") {
        storeFile = file("my-release-key.keystore")
        storePassword = "your-store-password"
        keyAlias = "my-key-alias"
        keyPassword = "your-key-password"
    }
}
`

**Best practice for production:** Use environment variables or a local.properties
file (git-ignored) to supply passwords instead of hardcoding them:

`kotlin
signingConfigs {
    create("release") {
        storeFile = file("../oauth2-release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
        keyAlias = System.getenv("KEY_ALIAS") ?: ""
        keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
}
`

## Build Instructions

### Build from Command Line

```powershell
# Set JDK 17 (required)
$env:JAVA_HOME = "C:\path\to\jdk-17"

### Set `JAVA_HOME` to your JDK 17 install:
   - **Windows (PowerShell):**
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
     ```
   - **Windows (cmd):**
     ```bat
     set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
     ```
   - **macOS / Linux:**
     ```bash
     export JAVA_HOME=$(/usr/libexec/java_home -v 17)
     ```
# Build
.\gradlew assembleRelease
```

# Output: app/build/outputs/apk/release/app-release.apk
`

## Install

```bash
adb install app/build/outputs/apk/release/app-release.apk
```
### Build from Android Studio

1. Open the ndroid/ folder in Android Studio
2. Select **Build → Generate Signed Bundle / APK**
3. Choose **APK**
4. Select the keystore file and enter credentials
5. Choose **release** build variant and click **Finish**

### Verify the Signature

`ash
/build-tools/34.0.0/apksigner verify --verbose app-release.apk
`

Expected output:
`
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
`

## Setup

1. Open the ndroid/ folder in Android Studio
2. Sync the project with Gradle
3. Run on an emulator or device (API 24+)

## How to Add an Account

### Method 1: Paste otpauth:// URI (recommended)
1. Open the web app → **Two-Factor Auth** → **Set up 2FA**
2. Copy the otpauth://totp/... URI
3. In the Android app, tap **Paste otpauth:// URI** and paste it

### Method 2: Manual Secret Entry
1. Open the web app → **Two-Factor Auth** → **Set up 2FA**
2. Copy the secret key (Base32 string shown below the QR code)
3. In the Android app, enter the account name and secret key

### Method 3: Generate New Secret
1. Tap **Generate New Secret** to create a new TOTP secret
2. Copy the secret to the web app's 2FA setup
3. Or use it standalone

## TOTP Algorithm

The app implements RFC 6238 (TOTP) with:

| Parameter | Value |
|-----------|-------|
| Algorithm | HMAC-SHA1 |
| Code Length | 6 digits |
| Time Step | 30 seconds |
| Secret Encoding | Base32 (RFC 4648) |
| Clock Drift Tolerance | ±1 time step (±30 seconds) |

This is fully compatible with Google Authenticator, Authy, and Microsoft Authenticator.

## Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| ndroidx.appcompat | 1.7.0 | AppCompat library |
| com.google.android.material | 1.12.0 | Material Design components |
| ndroidx.recyclerview | 1.3.2 | RecyclerView for account list |
| ndroidx.constraintlayout | 2.1.4 | ConstraintLayout |
| com.google.zxing:core | 3.5.3 | QR code parsing |
| commons-codec | 1.17.1 | Base32 encoding/decoding |

## Architecture

- **TotpGenerator** — Pure Kotlin implementation of TOTP/HOTP (RFC 6238/4226)
- **AccountStorage** — Persists accounts as JSON in SharedPreferences
- **MainActivity** — RecyclerView with auto-refreshing codes (1-second timer)
- **AddAccountActivity** — Form for manual entry, URI parsing, or secret generation
- **AccountAdapter** — Binds TOTP accounts to card views with live countdown
