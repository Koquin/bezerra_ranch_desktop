package backend

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfExporterTest {
    @Test
    fun `exporta dashboard com graficos e tabelas em tres paginas`() {
        val farms = listOf(
            PdfFarmSummary("Fazenda A", 10, 15, 25),
            PdfFarmSummary("Total", 10, 15, 25)
        )
        val ages = listOf(
            PdfAgeSummary("Fazenda A", List(7) { 2 }, List(7) { 3 }),
            PdfAgeSummary("Total", List(7) { 2 }, List(7) { 3 })
        )
        val report = PdfDashboardReport(
            totalHerd = PdfHerdSection("REBANHO TOTAL", farms.dropLast(1)),
            poHerd = PdfHerdSection("REBANHO P.O.", emptyList()),
            commercialHerd = PdfHerdSection("REBANHO COMERCIAL", farms.dropLast(1)),
            farmSummary = farms,
            ageSummary = ages,
            ageLabels = listOf("0-12", "12-24", "24-36", "36-48", "48-60", "60-72", ">72")
        )
        val output = Files.createTempFile("dashboard-report-", ".pdf").toFile()
        output.deleteOnExit()

        PdfExporter.exportDashboardAsPdf(report, output)

        PDDocument.load(output).use { document ->
            assertEquals(3, document.numberOfPages)
            val text = PDFTextStripper().getText(document)
            assertTrue(text.contains("REBANHO TOTAL"))
            assertTrue(text.contains("Tabelas do rebanho"))
            assertTrue(text.contains("Total por faixa etária"))
            assertTrue(text.contains("Total por sexo"))
        }
    }
}
