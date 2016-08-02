package net.yusukezzz.ssmtc.util.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.joda.time.DateTime

object GsonHolder {
    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(DateTime::class.java, DateTimeTypeConverter())
            .create()
    }
}
