package com.yonatankarp.agentdesk.app.operator.verification

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object JvmContentDigest {
    fun sha256(path: Path): ContentDigest {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return ContentDigest.parseSha256(digest.digest().toHexString())
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
        byte.toUByte().toString(radix = 16).padStart(length = 2, padChar = '0')
    }
}
