package backend

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.awt.image.BufferedImage
import java.io.File

object PdfExporter {
    fun exportImageAsPdf(image: BufferedImage, outFile: File) {
        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)
            val pdImage = LosslessFactory.createFromImage(doc, image)
            PDPageContentStream(doc, page).use { cs ->
                // Scale image to page size while preserving aspect
                val pageWidth = page.mediaBox.width
                val pageHeight = page.mediaBox.height
                val imgWidth = pdImage.width.toFloat()
                val imgHeight = pdImage.height.toFloat()
                val scale = minOf(pageWidth / imgWidth, pageHeight / imgHeight)
                val drawW = imgWidth * scale
                val drawH = imgHeight * scale
                val x = (pageWidth - drawW) / 2f
                val y = (pageHeight - drawH) / 2f
                cs.drawImage(pdImage, x, y, drawW, drawH)
            }
            doc.save(outFile)
        }
    }
}
