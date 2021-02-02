package net.yusukezzz.ssmtc.util.gson

import com.google.gson.*
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DateTimeTypeConverter : JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {
    companion object {
        const val DATETIME_PATTERN = "EEE MMM dd HH:mm:ss Z yyyy"
    }

    private val fmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN).withLocale(Locale.US)
    override fun serialize(
        src: OffsetDateTime,
        srcType: Type,
        context: JsonSerializationContext
    ): JsonElement =
        JsonPrimitive(fmt.format(src))

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): OffsetDateTime =
        fmt.parse(json.asString, OffsetDateTime::from)
}
