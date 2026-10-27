package com.lehyanflouriot.muscuapp.core.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonMergeTest {
    private fun json(text: String): JsonElement = Json.parseToJsonElement(text.replace('\'', '"'))

    private fun merge(base: String?, local: String, remote: String): JsonElement? =
        mergeJson(base?.let(::json), json(local), json(remote))

    @Test
    fun eachSideKeepsWhatItChanged() {
        assertEquals(
            json("{'a': 2, 'b': 3, 'c': 1}"),
            merge("{'a': 1, 'b': 1, 'c': 1}", "{'a': 2, 'b': 1, 'c': 1}", "{'a': 1, 'b': 3, 'c': 1}"),
        )
        // The same value changed on both sides: this device wins.
        assertEquals(json("{'a': 2}"), merge("{'a': 1}", "{'a': 2}", "{'a': 3}"))
        assertEquals(json("{'a': {'x': 2, 'y': 3}}"), merge("{'a': {'x': 1, 'y': 1}}", "{'a': {'x': 2, 'y': 1}}", "{'a': {'x': 1, 'y': 3}}"))
        assertEquals(json("{'a': null}"), merge("{'a': 1}", "{'a': 1}", "{'a': null}"))
    }

    @Test
    fun recordsAreMergedOneByOne() {
        val base = "[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 1}, {'id': 'c', 'v': 1}]"
        // Here: "b" changed and "d" added. Elsewhere: "c" removed, "e" added after "a", "a" changed.
        val local = "[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 2}, {'id': 'c', 'v': 1}, {'id': 'd', 'v': 1}]"
        val remote = "[{'id': 'a', 'v': 3}, {'id': 'e', 'v': 1}, {'id': 'b', 'v': 1}]"
        assertEquals(
            json("[{'id': 'a', 'v': 3}, {'id': 'e', 'v': 1}, {'id': 'b', 'v': 2}, {'id': 'd', 'v': 1}]"),
            merge(base, local, remote),
        )
    }

    @Test
    fun aRecordChangedOnOneSideAndRemovedOnTheOtherIsKept() {
        val base = "[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 1}]"
        assertEquals(json("[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 2}]"), merge(base, "[{'id': 'a', 'v': 1}]", "[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 2}]"))
        assertEquals(json("[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 2}]"), merge(base, "[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 2}]", "[{'id': 'a', 'v': 1}]"))
        assertEquals(json("[{'id': 'a', 'v': 2}, {'id': 'b', 'v': 1}]"), merge(base, "[{'id': 'b', 'v': 1}]", "[{'id': 'a', 'v': 2}, {'id': 'b', 'v': 1}]"))
        // Removed on one side and not changed on the other: removed.
        assertEquals(json("[{'id': 'b', 'v': 2}]"), merge(base, "[{'id': 'b', 'v': 2}]", base))
    }

    @Test
    fun recordsAreFoundByDateOrDayToo() {
        val base = "[{'date': '2026-10-26', 'eaten': ['x'], 'water': 2}]"
        val local = "[{'date': '2026-10-26', 'eaten': ['x', 'y'], 'water': 2}]"
        val remote = "[{'date': '2026-10-26', 'eaten': ['x', 'z'], 'water': 5}, {'date': '2026-10-27', 'eaten': [], 'water': 1}]"
        assertEquals(
            json("[{'date': '2026-10-26', 'eaten': ['x', 'y', 'z'], 'water': 5}, {'date': '2026-10-27', 'eaten': [], 'water': 1}]"),
            merge(base, local, remote),
        )
        val days = merge(
            "[{'day': 'MONDAY', 'label': ''}, {'day': 'TUESDAY', 'label': ''}]",
            "[{'day': 'MONDAY', 'label': 'Dos'}, {'day': 'TUESDAY', 'label': ''}]",
            "[{'day': 'MONDAY', 'label': ''}, {'day': 'TUESDAY', 'label': 'Jambes'}]",
        )
        assertEquals(json("[{'day': 'MONDAY', 'label': 'Dos'}, {'day': 'TUESDAY', 'label': 'Jambes'}]"), days)
    }

    @Test
    fun namesAreMergedAsSets() {
        assertEquals(json("['b', 'c', 'd']"), merge("['a', 'b']", "['b', 'c']", "['a', 'b', 'd']"))
        // Without a base, everything from both sides is kept.
        assertEquals(json("['a', 'b', 'c']"), merge(null, "['a', 'b']", "['b', 'c']"))
    }

    @Test
    fun listsWithoutIdentityChangedOnBothSidesKeepThisDevice() {
        // The sets of an exercise: 10 reps twice is not one set.
        val sets = "[{'reps': 10, 'weight': 50.0}, {'reps': 10, 'weight': 50.0}]"
        assertEquals(json(sets), merge("[{'reps': 8, 'weight': 50.0}]", sets, "[{'reps': 8, 'weight': 60.0}]"))
        assertEquals(json("[3, 3]"), merge("[1]", "[3, 3]", "[2]"))
        assertEquals(json("[[1, 2]]"), merge("[[1]]", "[[1, 2]]", "[[1, 3]]"))
    }

    @Test
    fun recordsAddedOnBothSidesWithoutBaseAreAllKept() {
        assertEquals(
            json("[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 2}, {'id': 'c', 'v': 1}]"),
            merge(null, "[{'id': 'a', 'v': 1}, {'id': 'c', 'v': 1}]", "[{'id': 'a', 'v': 1}, {'id': 'b', 'v': 2}]"),
        )
    }

    @Test
    fun theOrderOfTheSideThatReorderedIsKept() {
        val base = "[{'id': 'a'}, {'id': 'b'}, {'id': 'c'}]"
        assertEquals(json("[{'id': 'c'}, {'id': 'a'}, {'id': 'b'}]"), merge(base, base, "[{'id': 'c'}, {'id': 'a'}, {'id': 'b'}]"))
        assertEquals(json("[{'id': 'b'}, {'id': 'a'}, {'id': 'c'}, {'id': 'd'}]"), merge(base, "[{'id': 'b'}, {'id': 'a'}, {'id': 'c'}]", "[{'id': 'a'}, {'id': 'b'}, {'id': 'c'}, {'id': 'd'}]"))
    }

    @Test
    fun removedOnBothSides() {
        assertNull(mergeJson(json("{'a': 1}"), null, null))
        assertEquals(json("[]"), merge("[{'id': 'a'}]", "[]", "[]"))
    }
}
