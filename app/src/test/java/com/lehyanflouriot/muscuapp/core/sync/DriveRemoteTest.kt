package com.lehyanflouriot.muscuapp.core.sync

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class DriveRemoteTest {
    private class Tokens : AccessTokens {
        var next = 1
        val invalidated = mutableListOf<String>()
        override suspend fun token(): String = "token-$next"
        override suspend fun invalidate(token: String) {
            invalidated += token
            next++
        }
    }

    private lateinit var server: MockWebServer
    private val tokens = Tokens()
    private lateinit var drive: DriveRemote

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        drive = DriveRemote(tokens, appVersion = 15, device = "Pixel 9", baseUrl = server.url("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun reply(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body.replace('\'', '"')))
    }

    private fun RecordedRequest.query(name: String): String? = requestUrl?.queryParameter(name)

    @Test
    fun theSharedCopyIsFoundInTheFolderOfTheApp() = runTest {
        reply(
            """{'files': [
                {'id': 'late', 'md5Checksum': 'b', 'createdTime': '2026-10-27T10:00:00.000Z'},
                {'id': 'first', 'md5Checksum': 'a', 'createdTime': '2026-10-26T09:00:00.000Z',
                 'modifiedTime': '2026-10-27T08:30:00.000Z', 'appProperties': {'appVersion': '14', 'device': 'Pixel 8'}}
            ]}""",
        )
        val document = drive.find()!!
        assertEquals(RemoteDocument("first", "a", 14, "Pixel 8", 1_793_089_800_000L), document)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/drive/v3/files", request.requestUrl?.encodedPath)
        assertEquals("appDataFolder", request.query("spaces"))
        assertEquals("name = 'muscuapp-data.json' and trashed = false", request.query("q"))
        assertEquals("files(id,md5Checksum,createdTime,modifiedTime,appProperties)", request.query("fields"))
        assertEquals("Bearer token-1", request.getHeader("Authorization"))

        reply("{'files': []}")
        assertNull(drive.find())
    }

    @Test
    fun theContentIsDownloaded() = runTest {
        reply("{'schemaVersion': 2}")
        assertEquals("{\"schemaVersion\": 2}", drive.download(RemoteDocument("abc", "x")))
        val request = server.takeRequest()
        assertEquals("/drive/v3/files/abc", request.requestUrl?.encodedPath)
        assertEquals("media", request.query("alt"))
    }

    @Test
    fun theFirstUploadCreatesTheFileInTheFolderOfTheApp() = runTest {
        reply("{'id': 'new', 'md5Checksum': 'c', 'appProperties': {'appVersion': '15', 'device': 'Pixel 9'}}")
        val document = drive.upload(null, "{\"exercises\": []}")
        assertEquals(RemoteDocument("new", "c", 15, "Pixel 9"), document)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/upload/drive/v3/files", request.requestUrl?.encodedPath)
        assertEquals("multipart", request.query("uploadType"))
        assertEquals("id,md5Checksum,createdTime,modifiedTime,appProperties", request.query("fields"))
        val (metadata, content) = parts(request)
        val json = Json.parseToJsonElement(metadata).jsonObject
        assertEquals("muscuapp-data.json", json.getValue("name").jsonPrimitive.content)
        assertEquals("appDataFolder", json.getValue("parents").jsonArray.single().jsonPrimitive.content)
        assertEquals("15", json.getValue("appProperties").jsonObject.getValue("appVersion").jsonPrimitive.content)
        assertEquals("Pixel 9", json.getValue("appProperties").jsonObject.getValue("device").jsonPrimitive.content)
        assertEquals("{\"exercises\": []}", content)
    }

    @Test
    fun laterUploadsReplaceTheContent() = runTest {
        reply("{'id': 'abc', 'md5Checksum': 'd'}")
        assertEquals("d", drive.upload(RemoteDocument("abc", "c"), "{}").revision)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/upload/drive/v3/files/abc", request.requestUrl?.encodedPath)
        val (metadata, content) = parts(request)
        val json = Json.parseToJsonElement(metadata).jsonObject
        assertEquals(setOf("appProperties"), json.keys)
        assertEquals("{}", content)
    }

    @Test
    fun aRefusedTokenIsReplacedOnce() = runTest {
        reply("{'error': {'code': 401, 'message': 'Invalid Credentials'}}", code = 401)
        reply("{'files': []}")
        assertNull(drive.find())
        assertEquals(listOf("token-1"), tokens.invalidated)
        assertEquals("Bearer token-1", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-2", server.takeRequest().getHeader("Authorization"))

        reply("{'error': {'code': 401, 'message': 'Invalid Credentials'}}", code = 401)
        reply("{'error': {'code': 401, 'message': 'Invalid Credentials'}}", code = 401)
        assertProblem(SyncProblem.SIGN_IN) { drive.find() }
    }

    @Test
    fun errorsSayWhatToDo() = runTest {
        reply(
            """{'error': {'code': 403, 'message': 'Google Drive API has not been used in project 123 before or it is disabled.',
                'errors': [{'reason': 'accessNotConfigured'}], 'status': 'PERMISSION_DENIED',
                'details': [{'@type': 'type.googleapis.com/google.rpc.ErrorInfo', 'reason': 'SERVICE_DISABLED'}]}}""",
            code = 403,
        )
        val setup = assertProblem(SyncProblem.SETUP) { drive.find() }
        assertTrue(setup.message!!.startsWith("Google Drive API has not been used"))

        reply("{'error': {'code': 403, 'message': 'Insufficient Permission', 'errors': [{'reason': 'insufficientPermissions'}]}}", code = 403)
        assertProblem(SyncProblem.SIGN_IN) { drive.find() }

        reply("{'error': {'code': 429, 'message': 'Rate Limit Exceeded', 'errors': [{'reason': 'userRateLimitExceeded'}]}}", code = 429)
        assertProblem(SyncProblem.SERVER) { drive.find() }

        reply("oops", code = 500)
        assertProblem(SyncProblem.SERVER) { drive.find() }

        // Nothing answers on this port.
        val unreachable = DriveRemote(tokens, appVersion = 15, device = "Pixel 9", baseUrl = "http://127.0.0.1:1/".toHttpUrl())
        assertProblem(SyncProblem.OFFLINE) { unreachable.find() }
    }

    @Test
    fun theAddressOfTheAccountIsRead() = runTest {
        reply("{'user': {'emailAddress': 'lehyan@example.com', 'displayName': 'Lehyan'}}")
        assertEquals("lehyan@example.com", drive.accountEmail())
        val request = server.takeRequest()
        assertEquals("/drive/v3/about", request.requestUrl?.encodedPath)
        assertEquals("user(emailAddress)", request.query("fields"))
    }

    /** The metadata and the content of a multipart upload. */
    private fun parts(request: RecordedRequest): Pair<String, String> {
        val contentType = request.getHeader("Content-Type")!!
        assertTrue(contentType, contentType.startsWith("multipart/related; boundary="))
        val boundary = contentType.substringAfter("boundary=")
        val parts = request.body.readUtf8()
            .split("--$boundary")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "--" }
        assertEquals(2, parts.size)
        return parts.map { part ->
            val (headers, body) = part.split("\r\n\r\n", limit = 2)
            assertTrue(headers, headers.contains("Content-Type: application/json; charset=UTF-8"))
            body
        }.let { it[0] to it[1] }
    }

    private inline fun assertProblem(problem: SyncProblem, block: () -> Unit): SyncException {
        try {
            block()
        } catch (e: SyncException) {
            assertEquals(problem, e.problem)
            return e
        }
        fail("Expected $problem")
        error("unreachable")
    }
}
