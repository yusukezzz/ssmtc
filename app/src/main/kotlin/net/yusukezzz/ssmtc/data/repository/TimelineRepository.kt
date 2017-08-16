package net.yusukezzz.ssmtc.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.yusukezzz.ssmtc.data.api.Timeline
import java.io.File

class TimelineRepository(filesDir: File, private val gson: Gson) {
    companion object {
        const val REPOSITORY_DIR_NAME = "timelines"
    }

    private val repoDir = File(filesDir, REPOSITORY_DIR_NAME)
    private val paramsType = object : TypeToken<List<Timeline>>() {}.type

    init {
        repoDir.mkdirs()
    }

    fun initialize(userId: Long): List<Timeline> = findAll(userId).let {
        if (it.isEmpty()) {
            val params = listOf(Timeline.home())
            save(userId, params)
            params
        } else {
            it
        }
    }

    fun findAll(userId: Long): List<Timeline> = jsonFile(userId).let {
        if (it.exists()) {
            gson.fromJson<List<Timeline>>(it.readText(), paramsType)
        } else {
            listOf()
        }
    }

    fun add(userId: Long, timeline: Timeline) = save(userId, findAll(userId) + timeline)

    fun delete(userId: Long, timeline: Timeline) = save(userId, findAll(userId).filterNot { it.uuid == timeline.uuid })

    fun deleteAll(userId: Long): Boolean = jsonFile(userId).delete()

    fun save(userId: Long, timelines: List<Timeline>) = jsonFile(userId).writeText(gson.toJson(timelines.sorted()))

    private fun jsonFile(userId: Long): File = File(repoDir, "$userId.json")
}
