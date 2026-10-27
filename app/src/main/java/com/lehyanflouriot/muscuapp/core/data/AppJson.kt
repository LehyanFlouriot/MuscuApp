package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.AppData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** JSON format of the data file and of the backups. */
object AppJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val prettyJson: Json = Json(from = json) { prettyPrint = true }

    fun encode(data: AppData, pretty: Boolean = false): String =
        (if (pretty) prettyJson else json).encodeToString(AppData.serializer(), data)

    fun decode(text: String): AppData = json.decodeFromString(AppData.serializer(), text)

    fun tree(data: AppData): JsonElement = json.encodeToJsonElement(AppData.serializer(), data)

    fun fromTree(tree: JsonElement): AppData = json.decodeFromJsonElement(AppData.serializer(), tree)
}
