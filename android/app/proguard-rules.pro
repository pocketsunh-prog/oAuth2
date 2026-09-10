# ProGuard rules for OAuth2 TOTP app
# Keep TOTP-related classes
-keep class com.oauth.otp.TotpAccount { *; }
-keep class com.oauth.otp.TotpGenerator { *; }
