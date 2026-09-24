package com.oauth.otp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.oauth.otp.databinding.ActivityMainBinding

/**
 * Main activity for the OAuth2 TOTP Authenticator app.
 *
 * Displays a list of TOTP accounts with their current codes.
 * Codes refresh automatically every second.
 * Fingerprint authentication is required to view codes.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var accountStorage: AccountStorage
    private lateinit var totpGenerator: TotpGenerator
    private lateinit var adapter: AccountAdapter
    private val handler = Handler(Looper.getMainLooper())

    /** Whether the user has authenticated with fingerprint in this session. */
    private var isAuthenticated = false;

    /** Runnable that refreshes the TOTP codes every second. */
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshCodes()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components
        accountStorage = AccountStorage(this)
        totpGenerator = TotpGenerator()

        // Set up the RecyclerView
        setupRecyclerView()

        // Set up the FAB (add account button)
        binding.fabAddAccount.setOnClickListener {
            startActivity(Intent(this, AddAccountActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload accounts when returning from add account screen
        loadAccounts()

        // Require fingerprint authentication to view codes
        if (!isAuthenticated) {
            showFingerprintPrompt()
        } else {
            // Already authenticated — start the refresh timer
            handler.post(refreshRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop the refresh timer to save battery
        handler.removeCallbacks(refreshRunnable)
    }

    /**
     * Show the fingerprint authentication prompt.
     */
    private fun showFingerprintPrompt() {
        if (!FingerprintHelper.isAnyBiometricAvailable(this)) {
            // No biometric available — show warning but allow access
            AlertDialog.Builder(this)
                .setTitle("No Biometric Authentication")
                .setMessage(
                    "No fingerprint or device credential is enrolled. " +
                    "Please secure your device in Settings > Security."
                )
                .setPositiveButton("Continue") { _, _ ->
                    isAuthenticated = true
                    handler.post(refreshRunnable)
                }
                .setCancelable(false)
                .show()
            return
        }

        // Show the fingerprint prompt
        FingerprintHelper.authenticate(
            activity = this,
            title = "OAuth2 Authenticator",
            subtitle = "Verify your identity to view TOTP codes",
            onSuccess = {
                isAuthenticated = true
                adapter.isAuthenticated = true
                adapter.notifyDataSetChanged()
                handler.post(refreshRunnable)
                binding.textEmpty.text = "No accounts yet.\nTap + to add a TOTP account."
            },
            onError = { error ->
                // Error (e.g., too many attempts, user cancelled)
                binding.textEmpty.text = "Authentication required.\nTap to retry."
                binding.textEmpty.setOnClickListener {
                    showFingerprintPrompt()
                }
            },
            onFailed = {
                // Failed (wrong fingerprint) — can retry
                binding.textEmpty.text = "Authentication failed.\nTap to retry."
                binding.textEmpty.setOnClickListener {
                    showFingerprintPrompt()
                }
            }
        )
    }

    /**
     * Set up the RecyclerView with the account adapter.
     */
    private fun setupRecyclerView() {
        val accounts = accountStorage.loadAccounts().toMutableList()

        adapter = AccountAdapter(
            accounts = accounts,
            totpGenerator = totpGenerator,
            onDeleteClick = { position -> confirmDelete(position) }
        )

        binding.recyclerAccounts.layoutManager = LinearLayoutManager(this)
        binding.recyclerAccounts.adapter = adapter

        // Show empty state if no accounts
        updateEmptyState(accounts.isEmpty())
    }

    /**
     * Load accounts from storage and update the adapter.
     */
    private fun loadAccounts() {
        val accounts = accountStorage.loadAccounts()
        adapter.updateAccounts(accounts)
        updateEmptyState(accounts.isEmpty())
    }

    /**
     * Refresh the displayed TOTP codes.
     * Only works if the user is authenticated.
     */
    private fun refreshCodes() {
        if (isAuthenticated) {
            adapter.notifyDataSetChanged()
        }
    }

    /**
     * Show or hide the empty state message.
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerAccounts.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /**
     * Show a confirmation dialog before deleting an account.
     */
    private fun confirmDelete(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Remove Account")
            .setMessage("Remove this account from the authenticator?")
            .setPositiveButton("Remove") { _, _ ->
                accountStorage.removeAccount(position)
                loadAccounts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Create the options menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Handle options menu item clicks.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add_manual -> {
                startActivity(Intent(this, AddAccountActivity::class.java))
                true
            }
            R.id.menu_clear_all -> {
                confirmClearAll()
                true
            }
            R.id.menu_lock -> {
                lockApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Lock the app — requires fingerprint again on next resume.
     */
    private fun lockApp() {
        isAuthenticated = false
        adapter.isAuthenticated = false
        adapter.notifyDataSetChanged()
        handler.removeCallbacks(refreshRunnable)
        showFingerprintPrompt()
    }

    /**
     * Confirm clearing all accounts.
     */
    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Accounts")
            .setMessage("Remove all accounts? This cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                accountStorage.clearAll()
                loadAccounts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
