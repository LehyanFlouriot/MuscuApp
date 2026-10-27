package com.lehyanflouriot.muscuapp.core.sync

/** The copy of the data shared by the devices of the user. */
data class RemoteDocument(
    val id: String,
    /** Changes with the content: the same content always has the same revision. */
    val revision: String,
    /** Version code of the app that wrote it. */
    val appVersion: Int = 0,
    /** Name of the device that wrote it, when known. */
    val device: String? = null,
    /** When it was written, in milliseconds since the epoch. */
    val modifiedAt: Long? = null,
)

/** Where the shared copy of the data is kept. */
interface SyncRemote {
    /** The shared document, or null when there is none yet. */
    suspend fun find(): RemoteDocument?

    suspend fun download(document: RemoteDocument): String

    /** Writes [content] in place of [document], or creates the shared document when it is null. */
    suspend fun upload(document: RemoteDocument?, content: String): RemoteDocument
}

enum class SyncProblem {
    /** No network: the sync goes on as soon as it is back. */
    OFFLINE,

    /** The access to the account was lost or refused: the user has to connect again. */
    SIGN_IN,

    /** The Google Cloud project of the app is not ready (Drive API off, Android client missing...). */
    SETUP,

    /** Google Drive failed or refused for a while: tried again later. */
    SERVER,

    /** A newer version of the app wrote the shared copy: this one has to be updated first. */
    UPDATE_APP,

    /** The shared copy cannot be read. */
    UNREADABLE,
}

class SyncException(
    val problem: SyncProblem,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/** What to do the first time this device syncs, when the account already holds data. */
enum class FirstSyncChoice {
    /** The data of the account replaces the one of this device. */
    USE_ACCOUNT,

    /** Everything is kept: what exists on either side, this device winning where both differ. */
    MERGE,

    /** The data of this device replaces the one of the account. */
    KEEP_DEVICE,
}

/** What the app shows about the sync. */
data class SyncStatus(
    /** The Google account the data is synced with, null when the sync is off. */
    val account: String? = null,
    val running: Boolean = false,
    /** When both sides were last found the same, in milliseconds since the epoch. */
    val lastSync: Long? = null,
    val problem: SyncProblem? = null,
    /** What Google said about the problem, to help fixing it. */
    val detail: String? = null,
    /** Set while the user has to tell what to do with the data the account already holds. */
    val choice: RemoteDocument? = null,
)
