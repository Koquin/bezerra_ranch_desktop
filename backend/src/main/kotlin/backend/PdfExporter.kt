package backend

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

data class PdfFarmSummary(val farm: String, val males: Int, val females: Int, val total: Int)
data class PdfAgeSummary(val farm: String, val males: List<Int>, val females: List<Int>)
data class PdfHerdSection(val title: String, val farms: List<PdfFarmSummary>)
data class PdfDashboardReport(
    val totalHerd: PdfHerdSection,
    val poHerd: PdfHerdSection,
    val commercialHerd: PdfHerdSection,
    val farmSummary: List<PdfFarmSummary>,
    val ageSummary: List<PdfAgeSummary>,
    val ageLabels: List<String>
)

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

    fun exportDashboardAsPdf(report: PdfDashboardReport, outFile: File) {
        PDDocument().use { document ->
            drawHerdChartsPage(document, listOf(report.totalHerd, report.poHerd, report.commercialHerd))
            drawTablesPage(document, report)
            drawAgeChartsPage(document, report)
            document.save(outFile)
        }
    }

    private fun drawHerdChartsPage(document: PDDocument, sections: List<PdfHerdSection>) {
        val page = landscapePage(document)
        PDPageContentStream(document, page).use { stream ->
            text(stream, "Resumo do rebanho", 30f, 560f, 18f, bold = true)
            val gap = 18f
            val columnWidth = (page.mediaBox.width - 60f - gap * 2) / 3f
            sections.forEachIndexed { index, section ->
                val x = 30f + index * (columnWidth + gap)
                drawHerdSection(document, stream, section, x, 525f, columnWidth, 475f)
            }
        }
    }

    private fun drawHerdSection(
        document: PDDocument,
        stream: PDPageContentStream,
        section: PdfHerdSection,
        x: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        strokeRect(stream, x, top - height, width, height, Color(190, 190, 190))
        val total = section.farms.sumOf { it.total }
        val males = section.farms.sumOf { it.males }
        val females = section.farms.sumOf { it.females }
        drawOptionalImage(document, stream, "assets/grafico.png", x + 9f, top - 49f, 34f, 34f)
        text(stream, section.title, x + 50f, top - 22f, 12f, bold = true)
        text(stream, "Total: $total", x + 50f, top - 42f, 11f, bold = true, color = Color(63, 73, 250))
        val malePercent = if (total == 0) 0 else (males * 100.0 / total).toInt()
        val femalePercent = if (total == 0) 0 else (females * 100.0 / total).toInt()
        drawOptionalImage(document, stream, "assets/rebanho_machos.png", x + 10f, top - 72f, 13f, 13f)
        text(stream, "Machos: $malePercent% ($males)", x + 27f, top - 68f, 8f)
        drawOptionalImage(document, stream, "assets/rebanho_femeas.png", x + width / 2f, top - 72f, 13f, 13f)
        text(stream, "Fêmeas: $femalePercent% ($females)", x + width / 2f + 17f, top - 68f, 8f)
        text(stream, section.title.lowercase().replaceFirstChar { it.uppercase() }, x + 10f, top - 88f, 10f, bold = true)

        val farms = section.farms.filter { it.total > 0 }.sortedBy { it.total }
        val maximum = farms.maxOfOrNull { it.total } ?: 0
        val rowHeight = minOf(19f, 350f / maxOf(1, farms.size))
        farms.forEachIndexed { index, farm ->
            val y = top - 112f - index * rowHeight
            val labelWidth = width * 0.34f
            val valueWidth = 34f
            val barWidth = width - labelWidth - valueWidth - 22f
            text(stream, truncate(farm.farm, 17), x + 8f, y, 7f)
            fillRect(stream, x + labelWidth, y - 2f, barWidth, 8f, Color(230, 230, 230))
            val filledWidth = if (maximum == 0) 0f else barWidth * farm.total / maximum
            fillRect(stream, x + labelWidth, y - 2f, filledWidth, 8f, Color(63, 73, 250))
            text(stream, farm.total.toString(), x + width - valueWidth, y, 7f, bold = true)
        }
    }

    private fun drawTablesPage(document: PDDocument, report: PdfDashboardReport) {
        val page = landscapePage(document)
        PDPageContentStream(document, page).use { stream ->
            text(stream, "Tabelas do rebanho", 30f, 560f, 18f, bold = true)
            val farmWidth = 205f
            val ageX = 250f
            val ageWidth = page.mediaBox.width - ageX - 25f
            fillRect(stream, 25f, 520f, farmWidth, 24f, Color.BLACK)
            tableText(stream, listOf("FAZENDA", "M", "F", "TOTAL"), 25f, 520f, farmWidth, Color.WHITE, true)
            fillRect(stream, ageX, 544f, ageWidth, 22f, Color(131, 226, 142))
            text(stream, "Distribuição por idade", ageX + 6f, 551f, 9f, bold = true)

            val groupWidth = ageWidth / report.ageLabels.size
            report.ageLabels.forEachIndexed { index, label ->
                fillRect(stream, ageX + index * groupWidth, 520f, groupWidth, 24f, Color(128, 128, 128))
                centeredText(stream, truncate(label, 14), ageX + index * groupWidth, 528f, groupWidth, 6f, bold = true)
            }
            val ageCellWidth = ageWidth / (report.ageLabels.size * 2)
            fillRect(stream, ageX, 496f, ageWidth, 24f, Color(210, 210, 210))
            repeat(report.ageLabels.size) { index ->
                centeredText(stream, "MACHO", ageX + index * groupWidth, 505f, ageCellWidth, 5.5f, bold = true)
                centeredText(stream, "FÊMEA", ageX + index * groupWidth + ageCellWidth, 505f, ageCellWidth, 5.5f, bold = true)
            }

            val rows = report.farmSummary
            rows.forEachIndexed { index, farm ->
                val y = 474f - index * 22f
                val isTotal = farm.farm.equals("Total", ignoreCase = true)
                fillRect(stream, 25f, y, farmWidth, 22f, if (isTotal) Color.BLACK else if (index % 2 == 0) Color(248, 248, 248) else Color.WHITE)
                tableText(stream, listOf(farm.farm, farm.males.toString(), farm.females.toString(), farm.total.toString()), 25f, y, farmWidth, if (isTotal) Color.WHITE else Color.BLACK, isTotal)

                val age = report.ageSummary.getOrNull(index)
                fillRect(stream, ageX, y, ageWidth, 22f, if (isTotal) Color.BLACK else if (index % 2 == 0) Color(248, 248, 248) else Color.WHITE)
                if (age != null) {
                    val cells = age.males.indices.flatMap { listOf(age.males[it].toString(), age.females[it].toString()) }
                    cells.forEachIndexed { cellIndex, value ->
                        centeredText(stream, value, ageX + cellIndex * ageCellWidth, y + 7f, ageCellWidth, 6f, bold = isTotal, color = if (isTotal) Color.WHITE else Color.BLACK)
                    }
                }
            }
        }
    }

    private fun drawAgeChartsPage(document: PDDocument, report: PdfDashboardReport) {
        val page = landscapePage(document)
        val totalAge = report.ageSummary.lastOrNull { it.farm.equals("Total", ignoreCase = true) }
        val males = totalAge?.males ?: List(report.ageLabels.size) { 0 }
        val females = totalAge?.females ?: List(report.ageLabels.size) { 0 }
        PDPageContentStream(document, page).use { stream ->
            drawAgeChart(stream, "Total por faixa etária", report.ageLabels, males, females, 305f, grouped = false)
            drawAgeChart(stream, "Total por sexo", report.ageLabels, males, females, 35f, grouped = true)
        }
    }

    private fun drawAgeChart(
        stream: PDPageContentStream,
        title: String,
        labels: List<String>,
        males: List<Int>,
        females: List<Int>,
        bottom: Float,
        grouped: Boolean
    ) {
        val x = 35f
        val width = 770f
        val height = 240f
        strokeRect(stream, x, bottom, width, height, Color(190, 190, 190))
        centeredText(stream, title, x, bottom + height - 24f, width, 13f, bold = true)
        if (grouped) {
            fillRect(stream, x + width - 150f, bottom + height - 25f, 10f, 10f, Color(255, 105, 180))
            text(stream, "FÊMEA", x + width - 136f, bottom + height - 22f, 7f)
            fillRect(stream, x + width - 80f, bottom + height - 25f, 10f, 10f, Color(63, 73, 250))
            text(stream, "MACHO", x + width - 66f, bottom + height - 22f, 7f)
        }
        val values = if (grouped) males + females else males.indices.map { males[it] + females[it] }
        val maximum = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val groupWidth = width / labels.size
        labels.forEachIndexed { index, label ->
            val center = x + index * groupWidth + groupWidth / 2f
            if (grouped) {
                drawPdfColumn(stream, center - 22f, bottom + 40f, females[index], maximum, Color(255, 105, 180), 18f)
                drawPdfColumn(stream, center + 4f, bottom + 40f, males[index], maximum, Color(63, 73, 250), 18f)
            } else {
                drawPdfColumn(stream, center - 18f, bottom + 40f, males[index] + females[index], maximum, Color(131, 226, 142), 36f)
            }
            centeredText(stream, truncate(label, 15), x + index * groupWidth, bottom + 18f, groupWidth, 6f)
        }
    }

    private fun drawPdfColumn(stream: PDPageContentStream, x: Float, bottom: Float, value: Int, maximum: Int, color: Color, width: Float) {
        val height = 140f * value / maximum
        fillRect(stream, x, bottom, width, height, color)
        centeredText(stream, value.toString(), x - 4f, bottom + height + 5f, width + 8f, 7f, bold = true)
    }

    private fun landscapePage(document: PDDocument): PDPage = PDPage(
        PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
    ).also {
        document.addPage(it)
    }

    private fun tableText(stream: PDPageContentStream, cells: List<String>, x: Float, y: Float, width: Float, color: Color, bold: Boolean) {
        val weights = listOf(1.9f, 1f, 1f, 1f)
        val unit = width / weights.sum()
        var currentX = x
        cells.forEachIndexed { index, cell ->
            val cellWidth = unit * weights[index]
            centeredText(stream, truncate(cell, 22), currentX, y + 8f, cellWidth, 6.5f, bold, color)
            currentX += cellWidth
        }
    }

    private fun centeredText(stream: PDPageContentStream, value: String, x: Float, y: Float, width: Float, size: Float, bold: Boolean = false, color: Color = Color.BLACK) {
        val font = if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
        val textWidth = font.getStringWidth(value) / 1000f * size
        text(stream, value, x + (width - textWidth) / 2f, y, size, bold, color)
    }

    private fun text(stream: PDPageContentStream, value: String, x: Float, y: Float, size: Float, bold: Boolean = false, color: Color = Color.BLACK) {
        stream.beginText()
        stream.setNonStrokingColor(color)
        stream.setFont(if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA, size)
        stream.newLineAtOffset(x, y)
        stream.showText(value)
        stream.endText()
    }

    private fun fillRect(stream: PDPageContentStream, x: Float, y: Float, width: Float, height: Float, color: Color) {
        stream.setNonStrokingColor(color)
        stream.addRect(x, y, width.coerceAtLeast(0f), height.coerceAtLeast(0f))
        stream.fill()
    }

    private fun strokeRect(stream: PDPageContentStream, x: Float, y: Float, width: Float, height: Float, color: Color) {
        stream.setStrokingColor(color)
        stream.addRect(x, y, width, height)
        stream.stroke()
    }

    private fun drawOptionalImage(
        document: PDDocument,
        stream: PDPageContentStream,
        path: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        runCatching {
            val imageFile = File(path).takeIf { it.isFile } ?: return
            val bufferedImage = ImageIO.read(imageFile) ?: return
            val image = LosslessFactory.createFromImage(document, bufferedImage)
            stream.drawImage(image, x, y, width, height)
        }
    }

    private fun truncate(value: String, maximum: Int): String =
        if (value.length <= maximum) value else value.take(maximum - 3) + "..."
}
