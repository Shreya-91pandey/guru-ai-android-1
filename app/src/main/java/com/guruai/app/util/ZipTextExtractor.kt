package com.guruai.app.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

object ZipTextExtractor {
    fun extractText(context: Context, uri: Uri, maxTotalChars: Int = 2_000_000): String? {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && sb.length < maxTotalChars) {
                        val name = entry.name
                        if (!entry.isDirectory && (name.endsWith(".txt", true) || name.endsWith(".pdf", true))) {
                            val bytes = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                bytes.write(buffer, 0, len)
                            }
                            val entryText = if (name.endsWith(".pdf", true)) {
                                try {
                                    PDDocument.load(ByteArrayInputStream(bytes.toByteArray())).use { doc ->
                                        PDFTextStripper().getText(doc)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                bytes.toString("UTF-8")
                            }
                            if (!entryText.isNullOrBlank()) {
                                if (sb.isNotEmpty()) sb.append("\n\n----- ").append(name).append(" -----\n\n")
                                sb.append(entryText)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            val result = sb.toString()
            if (result.isBlank()) null else result.take(maxTotalChars)
        } catch (e: Exception) {
            null
        }
    }
}
