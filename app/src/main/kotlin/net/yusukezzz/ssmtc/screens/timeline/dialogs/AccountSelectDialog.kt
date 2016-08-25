package net.yusukezzz.ssmtc.screens.timeline.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.Account


class AccountSelectDialog: AppCompatDialogFragment() {
    companion object {
        const val ARG_ACCOUNTS = "accounts"

        fun newInstance(accounts: List<Account>): AccountSelectDialog = AccountSelectDialog().apply {
            arguments = Bundle().apply {
                putParcelableArray(ARG_ACCOUNTS, accounts.toTypedArray())
            }
        }
    }

    interface AccountSelectListener {
        fun onAccountSelect(account: Account)
        fun onAccountAdd()
    }

    private lateinit var listener: AccountSelectListener

    fun setAccountSelectListener(listener: AccountSelectListener): AccountSelectDialog {
        this.listener = listener

        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val accounts: List<Account> = arguments.getParcelableArrayList(ARG_ACCOUNTS)
        val items = accounts.map { "@" + it.user.screenName }.toTypedArray() +
            resources.getString(R.string.account_selector_add)
        val addItemPos = items.lastIndex

        return AlertDialog.Builder(activity).apply {
            setTitle(R.string.account_selector_title)
            setItems(items) { dialog, which ->
                if (which == addItemPos) {
                    listener.onAccountAdd()
                } else {
                    listener.onAccountSelect(accounts[which])
                }
            }
            setNegativeButton(R.string.account_selector_cancel) { d, w -> /* do nothing */ }
        }.create()
    }
}

