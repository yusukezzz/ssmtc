package net.yusukezzz.ssmtc.ui.status.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import net.yusukezzz.ssmtc.util.toast

class SuccessReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancelAll()
        context.toast("tweet success")
    }
}
