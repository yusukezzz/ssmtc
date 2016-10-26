package net.yusukezzz.ssmtc.ui.status.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import android.widget.Toast

class FailureReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancelAll()
        Toast.makeText(context, "tweet failed...", Toast.LENGTH_SHORT).show()
    }

}
