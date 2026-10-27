package com.lehyanflouriot.muscuapp.core.sync

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Fields that identify a record in a list, in the order they are looked for. */
private val RECORD_KEYS = listOf("id", "date", "weekStart", "day", "slotId", "itemId", "from")

/**
 * Three-way merge of JSON documents: what changed on one side since [base] is kept, and when both
 * sides changed the same value differently, [local] wins. Lists of records are merged record by
 * record (found by their "id", "date"...), and lists of names as sets. A record changed on one
 * side and removed on the other is kept. Returns null when the value is removed.
 */
fun mergeJson(base: JsonElement?, local: JsonElement?, remote: JsonElement?): JsonElement? = when {
    local == remote -> local
    local == base -> remote
    remote == base -> local
    local is JsonObject && remote is JsonObject -> mergeObjects(base as? JsonObject, local, remote)
    local is JsonArray && remote is JsonArray -> mergeArrays(base as? JsonArray, local, remote)
    local == null -> remote
    else -> local
}

private fun mergeObjects(base: JsonObject?, local: JsonObject, remote: JsonObject): JsonObject {
    val merged = LinkedHashMap<String, JsonElement>()
    for (key in local.keys + remote.keys) {
        mergeJson(base?.get(key), local[key], remote[key])?.let { merged[key] = it }
    }
    return JsonObject(merged)
}

private fun mergeArrays(base: JsonArray?, local: JsonArray, remote: JsonArray): JsonArray {
    val versions = listOfNotNull(base, local, remote)
    val key = RECORD_KEYS.firstOrNull { key -> versions.all { it.recordsBy(key) != null } }
    return when {
        key != null -> mergeRecords(base?.recordsBy(key).orEmpty(), local.recordsBy(key)!!, remote.recordsBy(key)!!)
        versions.all { it.isSetOfNames() } -> mergeSets(base ?: JsonArray(emptyList()), local, remote)
        // A list whose items cannot be told apart (the sets of an exercise...) changed on both sides.
        else -> local
    }
}

/** The records of the list by their [key], or null when they cannot all be found by it. */
private fun JsonArray.recordsBy(key: String): Map<String, JsonElement>? {
    val records = LinkedHashMap<String, JsonElement>()
    for (element in this) {
        val id = ((element as? JsonObject)?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
        if (records.put(id, element) != null) return null
    }
    return records
}

private fun mergeRecords(
    base: Map<String, JsonElement>,
    local: Map<String, JsonElement>,
    remote: Map<String, JsonElement>,
): JsonArray {
    val merged = LinkedHashMap<String, JsonElement>()
    for (id in local.keys + remote.keys) {
        mergeJson(base[id], local[id], remote[id])?.let { merged[id] = it }
    }
    // The order of the side that reordered the list, or the local one, with what only the other has.
    val remoteFirst = sameOrder(base.keys, local.keys) && !sameOrder(base.keys, remote.keys)
    val (first, second) = if (remoteFirst) remote.keys to local.keys else local.keys to remote.keys
    return JsonArray(arrange(first, second, merged.keys).map(merged::getValue))
}

/** Whether the ids both lists have are in the same order in both. */
private fun sameOrder(a: Set<String>, b: Set<String>): Boolean = a.filter { it in b } == b.filter { it in a }

/** The ids of [first] that are [kept], with those only in [second] placed after the id they follow there. */
private fun arrange(first: Set<String>, second: Set<String>, kept: Set<String>): List<String> {
    val result = first.filterTo(ArrayList()) { it in kept }
    var previous: String? = null
    for (id in second) {
        if (id in kept && id !in first) result.add(previous?.let { result.indexOf(it) + 1 } ?: 0, id)
        if (id in kept) previous = id
    }
    return result
}

private fun JsonArray.isSetOfNames(): Boolean =
    all { it is JsonPrimitive && it.isString } && toSet().size == size

private fun mergeSets(base: JsonArray, local: JsonArray, remote: JsonArray): JsonArray {
    val removedRemotely = base.toSet() - remote.toSet()
    val addedRemotely = remote.filter { it !in base && it !in local }
    return JsonArray(local.filter { it !in removedRemotely } + addedRemotely)
}
