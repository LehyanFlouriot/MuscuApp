package com.lehyanflouriot.muscuapp.core.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Gives the access tokens of the Google account to sync with. */
interface AccessTokens {
    /** A token to call Google Drive. Throws a [SyncException] when the user has to connect again. */
    suspend fun token(): String

    /** Forgets a token Google refused, so that the next one is a new one. */
    suspend fun invalidate(token: String)
}

/**
 * The shared copy of the data, kept in the folder of the app in the user's Google Drive
 * ("appDataFolder"): it is hidden from the user's files, and only this app can read it.
 */
class DriveRemote(
    private val tokens: AccessTokens,
    private val appVersion: Int,
    private val device: String,
    private val baseUrl: HttpUrl = GOOGLE_APIS,
    private val client: OkHttpClient = defaultClient,
) : SyncRemote {

    override suspend fun find(): RemoteDocument? {
        val url = url("drive/v3/files") {
            addQueryParameter("spaces", APP_FOLDER)
            addQueryParameter("q", "name = '$FILE_NAME' and trashed = false")
            addQueryParameter("fields", "files($FILE_FIELDS)")
            addQueryParameter("pageSize", "100")
        }
        val files = json.decodeFromString(FileList.serializer(), call(Request.Builder().url(url))).files
        // Two devices may have created it at the same time: the first one created is the shared one.
        return files.minWithOrNull(compareBy({ it.createdTime.orEmpty() }, { it.id }))?.toDocument()
    }

    override suspend fun download(document: RemoteDocument): String =
        call(Request.Builder().url(url("drive/v3/files/${document.id}") { addQueryParameter("alt", "media") }))

    override suspend fun upload(document: RemoteDocument?, content: String): RemoteDocument {
        val metadata = buildJsonObject {
            if (document == null) {
                put("name", FILE_NAME)
                put("mimeType", "application/json")
                putJsonArray("parents") { add(APP_FOLDER) }
            }
            putJsonObject("appProperties") {
                put(APP_VERSION, appVersion.toString())
                put(DEVICE, device.limitBytes(DEVICE_MAX_BYTES))
            }
        }
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toString().toRequestBody(JSON))
            .addPart(content.toRequestBody(JSON))
            .build()
        val request = if (document == null) {
            Request.Builder().url(uploadUrl("drive/v3/files")).post(body)
        } else {
            Request.Builder().url(uploadUrl("drive/v3/files/${document.id}")).patch(body)
        }
        return json.decodeFromString(DriveFile.serializer(), call(request)).toDocument()
    }

    /** The address of the Google account the tokens give access to, when Google tells it. */
    suspend fun accountEmail(): String? {
        val url = url("drive/v3/about") { addQueryParameter("fields", "user(emailAddress)") }
        return json.decodeFromString(About.serializer(), call(Request.Builder().url(url))).user?.emailAddress
    }

    private fun url(path: String, query: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        baseUrl.newBuilder().addPathSegments(path).apply(query).build()

    private fun uploadUrl(path: String): HttpUrl = url("upload/$path") {
        addQueryParameter("uploadType", "multipart")
        addQueryParameter("fields", FILE_FIELDS)
    }

    /** Sends the request with a token, and once more with a new one if Google refused it. */
    private suspend fun call(request: Request.Builder): String {
        val token = tokens.token()
        val first = execute(request.header("Authorization", "Bearer $token").build())
        if (first.code != HTTP_UNAUTHORIZED) return first.bodyOrThrow()
        tokens.invalidate(token)
        return execute(request.header("Authorization", "Bearer ${tokens.token()}").build()).bodyOrThrow()
    }

    private class Reply(val code: Int, val body: String)

    private suspend fun execute(request: Request): Reply = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { Reply(it.code, it.body?.string().orEmpty()) }
        } catch (e: IOException) {
            throw SyncException(SyncProblem.OFFLINE, e.message, e)
        }
    }

    private fun Reply.bodyOrThrow(): String {
        if (code in 200..299) return body
        val error = runCatching { json.decodeFromString(ErrorReply.serializer(), body).error }.getOrNull()
        val reasons = error?.let { it.errors + it.details }.orEmpty().mapNotNull { it.reason }
        val problem = when {
            code == HTTP_UNAUTHORIZED -> SyncProblem.SIGN_IN
            reasons.any { it in SETUP_REASONS } -> SyncProblem.SETUP
            reasons.any { it in ACCESS_REASONS } -> SyncProblem.SIGN_IN
            else -> SyncProblem.SERVER
        }
        throw SyncException(problem, error?.message ?: "HTTP $code")
    }

    @Serializable
    private class DriveFile(
        val id: String,
        val md5Checksum: String? = null,
        val createdTime: String? = null,
        val modifiedTime: String? = null,
        val appProperties: Map<String, String> = emptyMap(),
    ) {
        fun toDocument() = RemoteDocument(
            id = id,
            revision = md5Checksum ?: modifiedTime ?: id,
            appVersion = appProperties[APP_VERSION]?.toIntOrNull() ?: 0,
            device = appProperties[DEVICE],
            modifiedAt = modifiedTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
        )
    }

    @Serializable
    private class FileList(val files: List<DriveFile> = emptyList())

    @Serializable
    private class About(val user: User? = null) {
        @Serializable
        class User(val emailAddress: String? = null)
    }

    @Serializable
    private class ErrorReply(val error: ApiError? = null)

    @Serializable
    private class ApiError(
        val message: String? = null,
        val errors: List<Reason> = emptyList(),
        val details: List<Reason> = emptyList(),
    )

    @Serializable
    private class Reason(val reason: String? = null)

    companion object {
        /** Access to the folder of the app in Google Drive, and nothing else of the user's Drive. */
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"

        const val FILE_NAME = "muscuapp-data.json"
        private const val APP_FOLDER = "appDataFolder"
        private const val FILE_FIELDS = "id,md5Checksum,createdTime,modifiedTime,appProperties"
        private const val APP_VERSION = "appVersion"
        private const val DEVICE = "device"

        /** A property of a Drive file, name and value, fits in 124 bytes. */
        private const val DEVICE_MAX_BYTES = 100
        private const val HTTP_UNAUTHORIZED = 401

        /** The Drive API is not enabled in the Google Cloud project of the app. */
        private val SETUP_REASONS = setOf("accessNotConfigured", "SERVICE_DISABLED", "projectNotLinked")

        /** The token does not give access to the folder of the app. */
        private val ACCESS_REASONS = setOf("insufficientPermissions", "ACCESS_TOKEN_SCOPE_INSUFFICIENT", "authError")

        private val GOOGLE_APIS = "https://www.googleapis.com/".toHttpUrl()
        private val JSON = "application/json; charset=UTF-8".toMediaType()
        private val json = Json { ignoreUnknownKeys = true }

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        private fun String.limitBytes(max: Int): String {
            var text = this
            while (text.toByteArray(Charsets.UTF_8).size > max) text = text.dropLast(1)
            return text
        }
    }
}
