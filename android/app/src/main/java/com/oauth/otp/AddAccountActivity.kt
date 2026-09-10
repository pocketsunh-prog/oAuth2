package com.oauth.otp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.oauth.otp.databinding.ActivityAddAccountBinding
import java.util.regex.Pattern

/**
 * Activity for adding a new TOTP account.
 *
 * Supports:
 * - Scanning a QR code with the camera
 * - Pasting an otpauth:// URI manually
 * - Manual entry of secret key
 * - Generating a new secret
 */
class AddAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAccountBinding
    private lateinit var accountStorage: AccountStorage
    private lateinit var totpGenerator: TotpGenerator

    // Launches the ZXing barcode scanner and receives the result.
    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Successfully scanned a QR code
            parseOtpauthUri(result.contents)
        } else {
            // User cancelled the scan
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Requests the CAMERA permission; proceeds to scan if granted.
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startQrScan()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to scan QR codes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Add Account"

        accountStorage = AccountStorage(this)
        totpGenerator = TotpGenerator()

        // Scan QR code button — checks camera permission then opens scanner
        binding.btnScanQr.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        // Paste URI button — shows a dialog to paste an otpauth:// URI
        binding.btnPasteUri.setOnClickListener {
            showUriInputDialog()
        }

        // Generate secret button — creates a new random secret
        binding.btnGenerateSecret.setOnClickListener {
            val secret = totpGenerator.generateSecret()
            binding.editSecret.setText(secret)
        }

        // Save button — validates and saves the account
        binding.btnSave.setOnClickListener {
            saveAccount()
        }
    }

    /**
     * Check camera permission and start the QR scanner.
     * If permission is not granted, request it first.
     */
    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted — start scanning
                startQrScan()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale before requesting
                AlertDialog.Builder(this)
                    .setTitle("Camera Permission")
                    .setMessage("The camera is needed to scan QR codes for TOTP setup. No images are stored.")
                    .setPositiveButton("Grant") { _, _ ->
                        requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                // Request permission directly
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Launch the ZXing QR code scanner with TOTP-optimized settings.
     */
    private fun startQrScan() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan the QR code from your OAuth2 server")
            setCameraId(0) // Use the back camera
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
            setTimeout(30000) // 30-second timeout
        }
        scanLauncher.launch(options)
    }

    /**
     * Show a dialog to paste an otpauth:// URI manually.
     */
    private fun showUriInputDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "otpauth://totp/..."
        }

        AlertDialog.Builder(this)
            .setTitle("Paste otpauth:// URI")
            .setMessage("Paste the otpauth:// URI from your QR code:")
            .setView(input)
            .setPositiveButton("Parse") { _, _ ->
                val uri = input.text.toString()
                parseOtpauthUri(uri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Parse an otpauth:// URI and populate the form.
     */
    private fun parseOtpauthUri(uri: String) {
        val account = totpGenerator.parseOtpauthUri(uri)

        if (account != null) {
            binding.editAccountName.setText(account.account)
            binding.editSecret.setText(account.secret)
            Toast.makeText(this, "QR code scanned successfully", Toast.LENGTH_SHORT).show()
        } else {
            // If it's not a valid otpauth URI, show an error
            if (uri.startsWith("otpauth://")) {
                Toast.makeText(this, "Invalid otpauth:// URI format", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Not a valid TOTP QR code", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Save the new account to storage.
     */
    private fun saveAccount() {
        val accountName = binding.editAccountName.text.toString().trim()
        val secret = binding.editSecret.text.toString().trim().replace(" ", "").uppercase()

        // Validate inputs
        if (accountName.isEmpty()) {
            binding.editAccountName.error = "Account name is required"
            return
        }

        if (secret.isEmpty()) {
            binding.editSecret.error = "Secret key is required"
            return
        }

        // Validate the secret is valid Base32
        if (!isValidBase32(secret)) {
            binding.editSecret.error = "Invalid secret key (must be Base32)"
            return
        }

        // Verify the secret generates a valid code
        try {
            val code = totpGenerator.generateCode(secret)
            if (code.length != 6) {
                binding.editSecret.error = "Invalid secret key"
                return
            }
        } catch (e: Exception) {
            binding.editSecret.error = "Invalid secret key: ${e.message}"
            return
        }

        // Save the account
        val account = TotpAccount(
            account = accountName,
            secret = secret,
            issuer = "OAuth2Server"
        )
        accountStorage.addAccount(account)

        Toast.makeText(this, "Account added: $accountName", Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * Check if a string is valid Base32.
     */
    private fun isValidBase32(input: String): Boolean {
        val base32Pattern = Pattern.compile("^[A-Z2-7]+=*$")
        return base32Pattern.matcher(input).matches() && input.isNotEmpty()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
