package service

import java.awt.image.BufferedImage
import java.io.File
import backend.PdfExporter

object ExportService {
    fun exportImageAsPdf(image: BufferedImage, out: File) {
        PdfExporter.exportImageAsPdf(image, out)
    }
}
