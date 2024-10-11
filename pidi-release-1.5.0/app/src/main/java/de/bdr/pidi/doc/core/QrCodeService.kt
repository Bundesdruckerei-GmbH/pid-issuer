/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.doc.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage

@Service
class QrCodeService {

    fun generateQrCode(content: String, size: Int): BufferedImage {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to "0")
        return MatrixToImageWriter.toBufferedImage(
            MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        )
    }
}