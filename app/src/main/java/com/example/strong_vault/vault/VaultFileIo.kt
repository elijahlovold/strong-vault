package com.example.strong_vault.vault

import java.io.File

object VaultFileIo {
    fun readTextOrNull(file: File): String? =
        if (file.isFile) file.readText() else null

    /** Writes via a sibling temp file + rename so a concurrent reader (this app, an editor, a sync
     *  client) never observes a partially-written note. */
    fun writeTextAtomically(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, ".${file.name}.tmp-${System.nanoTime()}")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }
}
