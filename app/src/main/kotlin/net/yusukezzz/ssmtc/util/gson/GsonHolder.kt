package net.yusukezzz.ssmtc.util.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.threeten.bp.OffsetDateTime

object GsonHolder {
    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(OffsetDateTime::class.java, DateTimeTypeConverter())
            .create()
    }
}
