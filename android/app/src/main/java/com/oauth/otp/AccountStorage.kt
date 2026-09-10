package com.oauth.otp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent storage for TOTP accounts using SharedPreferences.
 * Accounts are stored as a JSON array.
 */
class AccountStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "totp_accounts"
        private const val KEY_ACCOUNTS = "accounts"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Load all stored TOTP accounts.
     */
    fun loadAccounts(): List<TotpAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, "[]") ?: "[]"
        val jsonArray = JSONArray(json)
        val accounts = mutableListOf<TotpAccount>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            accounts.add(
                TotpAccount(
                    account = obj.getString("account"),
                    secret = obj.getString("secret"),
                    issuer = obj.optString("issuer", "OAuth2Server")
                )
            )
        }

        return accounts
    }

    /**
     * Save all TOTP accounts.
     */
    fun saveAccounts(accounts: List<TotpAccount>) {
        val jsonArray = JSONArray()

        for (account in accounts) {
            val obj = JSONObject()
            obj.put("account", account.account)
            obj.put("secret", account.secret)
            obj.put("issuer", account.issuer)
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString(KEY_ACCOUNTS, jsonArray.toString())
            .apply()
    }

    /**
     * Add a new TOTP account.
     */
    fun addAccount(account: TotpAccount) {
        val accounts = loadAccounts().toMutableList()
        accounts.add(account)
        saveAccounts(accounts)
    }

    /**
     * Remove a TOTP account by index.
     */
    fun removeAccount(index: Int) {
        val accounts = loadAccounts().toMutableList()
        if (index in accounts.indices) {
            accounts.removeAt(index)
            saveAccounts(accounts)
        }
    }

    /**
     * Clear all accounts.
     */
    fun clearAll() {
        prefs.edit().remove(KEY_ACCOUNTS).apply()
    }
}
