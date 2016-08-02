package net.yusukezzz.ssmtc.util.gson

import com.google.gson.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.lang.reflect.Type
import java.util.*

class DateTimeTypeConverter: JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
    companion object {
        val DATETIME_PATTERN = "EEE MMM dd HH:mm:ss Z yyyy"
    }

    private val fmt = DateTimeFormat.forPattern(DATETIME_PATTERN).withLocale(Locale.ENGLISH)
    // No need for an InstanceCreator since DateTime provides a no-args constructor
    override fun serialize(src: DateTime, srcType: Type, context: JsonSerializationContext): JsonElement =
        JsonPrimitive(fmt.print(src))

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): DateTime =
        fmt.parseDateTime(json.asString);
}

