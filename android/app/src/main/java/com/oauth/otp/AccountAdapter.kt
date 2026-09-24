package com.oauth.otp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying TOTP accounts with their current codes.
 * When not authenticated, codes are masked with asterisks.
 */
class AccountAdapter(
    private val accounts: MutableList<TotpAccount>,
    private val totpGenerator: TotpGenerator,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    /** Whether the user has authenticated — controls code visibility. */
    var isAuthenticated: Boolean = false

    /**
     * ViewHolder for a single TOTP account row.
     */
    class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val accountName: TextView = view.findViewById(R.id.account_name)
        val totpCode: TextView = view.findViewById(R.id.totp_code)
        val countdownBar: ProgressBar = view.findViewById(R.id.countdown_bar)
        val textRemaining: TextView = view.findViewById(R.id.text_remaining)
        val deleteButton: View = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts[position]

        // Display the account name
        holder.accountName.text = account.account

        if (isAuthenticated) {
            // Generate and display the current TOTP code
            val code = totpGenerator.generateCode(account.secret)
            // Format as "123 456" for readability
            holder.totpCode.text = "${code.substring(0, 3)} ${code.substring(3)}"

            // Update the countdown bar
            val remaining = totpGenerator.getRemainingSeconds()
            holder.countdownBar.progress = remaining
            holder.textRemaining.text = "${remaining}s remaining"

            // Change color when code is about to expire (less than 5 seconds)
            if (remaining <= 5) {
                holder.totpCode.setTextColor(0xFFDC2626.toInt()) // red
                holder.countdownBar.progressTintList =
                    android.content.res.ColorStateList.valueOf(0xFFDC2626.toInt())
            } else {
                holder.totpCode.setTextColor(0xFF1E40AF.toInt()) // blue
                holder.countdownBar.progressTintList =
                    android.content.res.ColorStateList.valueOf(0xFF2563EB.toInt())
            }
        } else {
            // Mask the code — user hasn't authenticated yet
            holder.totpCode.text = "••• •••"
            holder.totpCode.setTextColor(0xFF94A3B8.toInt()) // gray
            holder.countdownBar.progress = 0
            holder.textRemaining.text = "Locked"
        }

        // Delete button
        holder.deleteButton.setOnClickListener {
            onDeleteClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = accounts.size

    /**
     * Update the accounts list and refresh the display.
     */
    fun updateAccounts(newAccounts: List<TotpAccount>) {
        accounts.clear()
        accounts.addAll(newAccounts)
        notifyDataSetChanged()
    }
}
