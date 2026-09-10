package com.oauth.otp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.oauth.otp.databinding.ActivityMainBinding

/**
 * Main activity for the OAuth2 TOTP Authenticator app.
 *
 * Displays a list of TOTP accounts with their current codes.
 * Codes refresh automatically every second.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var accountStorage: AccountStorage
    private lateinit var totpGenerator: TotpGenerator
    private lateinit var adapter: AccountAdapter
    private val handler = Handler(Looper.getMainLooper())

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
        // Start the refresh timer
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Stop the refresh timer to save battery
        handler.removeCallbacks(refreshRunnable)
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
     */
    private fun refreshCodes() {
        adapter.notifyDataSetChanged()
    }

    /**
     * Show or hide the empty state message.
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textEmpty.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerAccounts.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
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
            else -> super.onOptionsItemSelected(item)
        }
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
