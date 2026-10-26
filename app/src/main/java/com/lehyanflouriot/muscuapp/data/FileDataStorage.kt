package com.lehyanflouriot.muscuapp.data

import android.util.AtomicFile
import com.lehyanflouriot.muscuapp.core.data.DataStorage
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** Keeps the data document in the app's private storage, written atomically. */
class FileDataStorage(private val file: File) : DataStorage {
    private val atomicFile = AtomicFile(file)

    override fun read(): String? =
        try {
            atomicFile.readFully().toString(Charsets.UTF_8)
        } catch (e: FileNotFoundException) {
            null
        }

    override fun write(content: String) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(content.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: IOException) {
            atomicFile.failWrite(stream)
            throw e
        }
    }

    override fun keepUnreadableCopy(content: String) {
        File(file.parentFile, "${file.nameWithoutExtension}-unreadable-${System.currentTimeMillis()}.json")
            .writeText(content)
    }
}
