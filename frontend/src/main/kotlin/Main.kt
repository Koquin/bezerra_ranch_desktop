import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import backend.PdfExporter
import backend.controller.AnimalController
import backend.controller.SynchronizationController
import backend.dashboard.AnimalDashboard
import backend.sync.SyncTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

@Composable
fun App() {
    debugLog("App", "entrada; arquivo=Main.kt; camada=Frontend")
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val animalController = remember { AnimalController() }
    val synchronizationController = remember { SynchronizationController() }
    var hovered by remember { mutableStateOf("") }
    var herdSummaryRows by remember { mutableStateOf(fallbackHerdRows()) }
    var totalMales by remember { mutableStateOf("N/A") }
    var totalFemales by remember { mutableStateOf("N/A") }
    var totalAnimals by remember { mutableStateOf("N/A") }
    var isDownloading by remember { mutableStateOf(false) }

    suspend fun updateHerdSummary() {
        debugLog("App.updateHerdSummary", "entrada; controller=AnimalController; carregando painel local do SQLite")
        val dashboard = withContext(Dispatchers.IO) { animalController.painelLocal() }
        debugLog("App.updateHerdSummary", "variáveis; animais=${dashboard.quantidadeAnimais}; machos=${dashboard.quantidadeMachos}; femeas=${dashboard.quantidadeFemeas}; fazendas=${dashboard.porFazenda.size}")
        val display = dashboard.toHerdDisplay()
        herdSummaryRows = display.rows
        totalMales = display.totalMales
        totalFemales = display.totalFemales
        totalAnimals = display.totalAnimals
        debugLog("App.updateHerdSummary", "retorno=Unit; linhas=${display.rows.size}; total=${display.totalAnimals}")
    }

    LaunchedEffect(Unit) {
        debugLog("App.LaunchedEffect", "entrada; variável-chave=Unit; carregamento inicial da tabela")
        try {
            updateHerdSummary()
            debugLog("App.LaunchedEffect", "retorno=Unit; carregamento inicial concluído")
        } catch (_: Throwable) {
            // A tabela mantém o estado N/A quando ainda não houver banco ou registros locais.
            debugLog("App.LaunchedEffect", "retorno=Unit; falha no carregamento, mantendo fallback=N/A")
        }
    }
    val navItems = listOf(
        "NASCIMENTOS",
        "COMPRAS",
        "VENDAS",
        "MORTES",
        "ABATES",
        "TRANSFERENCIAS",
        "PESAGEM"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bezerra Ranch") },
                backgroundColor = Color(0xFF1976D2),
                contentColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start) {
                    // Left side: navigation buttons
                    for (name in navItems) {
                        NavSquareButton(name = name, hovered = hovered, onHover = { hovered = it }) {
                            debugLog("App.navigationClick", "entrada; nome=$name")
                            try {
                                // placeholder navigation
                                throw NotImplementedError("Tela ainda em construção")
                            } catch (e: Throwable) {
                                debugLog("App.navigationClick", "erro; nome=$name; mensagem=${e.message}")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Erro ao navegar para outra tela: A tela ainda está em construção")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Right side: local refresh, download from Supabase and export
                Row(modifier = Modifier.wrapContentWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = !isDownloading,
                        onClick = {
                            coroutineScope.launch {
                                debugLog("App.atualizarClick", "entrada; isDownloading=$isDownloading")
                                try {
                                    updateHerdSummary()
                                    snackbarHostState.showSnackbar("Tabela atualizada com os dados locais")
                                    debugLog("App.atualizarClick", "retorno=Unit; tabela local atualizada")
                                } catch (e: Throwable) {
                                    debugLog("App.atualizarClick", "erro; mensagem=${e.message}")
                                    snackbarHostState.showSnackbar("Erro ao atualizar: ${e.message}")
                                }
                            }
                        }
                    ) { Text("Atualizar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isDownloading,
                        onClick = {
                            coroutineScope.launch {
                                debugLog("App.baixarDadosClick", "entrada; tabela=${SyncTable.ANIMAL}; isDownloading=$isDownloading")
                                isDownloading = true
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        synchronizationController.baixarTabela(SyncTable.ANIMAL)
                                    }
                                    updateHerdSummary()
                                    snackbarHostState.showSnackbar("${result.records} animais baixados para o SQLite")
                                    debugLog("App.baixarDadosClick", "retorno=Unit; registros=${result.records}; chunks=${result.chunks.size}")
                                } catch (e: Throwable) {
                                    debugLog("App.baixarDadosClick", "erro; mensagem=${e.message}")
                                    snackbarHostState.showSnackbar("Erro ao baixar dados: ${e.message}")
                                } finally {
                                    isDownloading = false
                                    debugLog("App.baixarDadosClick", "finalização; isDownloading=$isDownloading")
                                }
                            }
                        }
                    ) { Text(if (isDownloading) "Baixando..." else "Baixar dados") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        debugLog("App.exportarPdfClick", "entrada; iniciando captura da janela")
                        // Capture center area and export to PDF
                        try {
                            val windows = java.awt.Window.getWindows().filter { it.isVisible }
                            val win = windows.firstOrNull()
                            if (win != null) {
                                val b = win.bounds
                                val rx = (b.x + b.width * 0.15).toInt()
                                val ry = (b.y + b.height * 0.18).toInt()
                                val rw = (b.width * 0.6).toInt()
                                val rh = (b.height * 0.64).toInt()
                                val robot = Robot()
                                val capture = robot.createScreenCapture(Rectangle(rx, ry, rw, rh))
                                val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                val outFile = File("export_report_$ts.pdf")
                                PdfExporter.exportImageAsPdf(capture, outFile)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Exportado para ${outFile.absolutePath}")
                                }
                                debugLog("App.exportarPdfClick", "retorno=Unit; arquivo=${outFile.absolutePath}")
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Janela não encontrada para captura")
                                }
                                debugLog("App.exportarPdfClick", "retorno=Unit; janela não encontrada")
                            }
                        } catch (e: Throwable) {
                            debugLog("App.exportarPdfClick", "erro; mensagem=${e.message}")
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Erro ao exportar: ${e.message}")
                            }
                        }
                    }) {
                        Text("Exportar PDF")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hover text label
            Text(text = if (hovered.isNotBlank()) hovered else " ", modifier = Modifier.padding(4.dp))

            Spacer(modifier = Modifier.height(8.dp))

            // Middle area with three tall rectangular cards side by side
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 1..3) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                        elevation = 4.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Placeholder Relatório $i")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lower section: summary uses only the local SQLite database.

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF8BC34A), RoundedCornerShape(6.dp))
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Resumo",
                            color = Color.Black,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = "Rebanho Atual",
                            color = Color.Black,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    HerdSummaryTable(
                        rows = herdSummaryRows,
                        totalMales = totalMales,
                        totalFemales = totalFemales,
                        totalAnimals = totalAnimals
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFBDBDBD))
                )

                Box(
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight()
                        .background(Color(0xFFF7F7F7))
                ) {
                    Text(
                        text = "Área da direita será preenchida no próximo passo",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
    debugLog("App", "retorno=Unit; composição configurada; linhas=${herdSummaryRows.size}; baixando=$isDownloading")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavSquareButton(name: String, hovered: String, onHover: (String) -> Unit, onClick: () -> Unit) {
    debugLog("NavSquareButton", "entrada; name=$name; hovered=$hovered")
    val size = 56.dp
    val assets = mapOf(
        "NASCIMENTOS" to "assets/nascimentos.png",
        "COMPRAS" to "assets/compras.png",
        "VENDAS" to "assets/vendas.png",
        "MORTES" to "assets/mortes.png",
        "ABATES" to "assets/abates.png",
        "TRANSFERENCIAS" to "assets/transferencias.png",
        "PESAGEM" to "assets/pesagem.png"
    )

    val imgBitmap = remember(name) {
        debugLog("NavSquareButton.imageLoader", "entrada; name=$name; caminho=${assets[name]}")
        try {
            val p = assets[name]
            if (p != null) {
                val f = java.io.File(p)
                if (f.exists()) {
                    debugLog("NavSquareButton.imageLoader", "retorno=ImageBitmap; arquivo=${f.path}")
                    loadImageBitmap(f.inputStream())
                } else {
                    debugLog("NavSquareButton.imageLoader", "retorno=null; arquivo ausente=${f.path}")
                    null
                }
            } else {
                debugLog("NavSquareButton.imageLoader", "retorno=null; não há caminho de ícone para name=$name")
                null
            }
        } catch (e: Throwable) {
            debugLog("NavSquareButton.imageLoader", "retorno=null; erro=${e.message}")
            null
        }
    }

    Surface(modifier = Modifier.size(size).border(1.dp, Color.DarkGray), color = Color(0xFFEFEFEF)) {
        Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            if (imgBitmap != null) {
                Image(bitmap = imgBitmap, contentDescription = name, modifier = Modifier.fillMaxSize())
            } else {
                Icon(imageVector = Icons.Default.Image, contentDescription = name, tint = Color.Gray)
            }
            // overlay for interactions
            Box(modifier = Modifier.matchParentSize().background(Color.Transparent)
                .pointerInput(Unit) {
                    // pointer input not required
                }
                .pointerMoveFilter(
                    onEnter = {
                        onHover(name)
                        false
                    },
                    onExit = {
                        onHover("")
                        false
                    }
                )
                .clickable(onClick = onClick))
        }
    }
    debugLog("NavSquareButton", "retorno=Unit; name=$name; possuiImagem=${imgBitmap != null}")
}

data class HerdSummaryRow(
    val farmName: String,
    val males: String,
    val females: String,
    val total: String
)

private data class HerdDisplay(
    val rows: List<HerdSummaryRow>,
    val totalMales: String,
    val totalFemales: String,
    val totalAnimals: String
)

private fun fallbackHerdRows(): List<HerdSummaryRow> {
    debugLog("fallbackHerdRows", "entrada; sem dados locais")
    val result = listOf(HerdSummaryRow("N/A", "N/A", "N/A", "N/A"))
    debugLog("fallbackHerdRows", "retorno=$result")
    return result
}

private fun AnimalDashboard.toHerdDisplay(): HerdDisplay {
    debugLog("AnimalDashboard.toHerdDisplay", "entrada; animais=$quantidadeAnimais; machos=$quantidadeMachos; femeas=$quantidadeFemeas; fazendas=${porFazenda.size}")
    if (quantidadeAnimais == 0) {
        val result = HerdDisplay(fallbackHerdRows(), "N/A", "N/A", "N/A")
        debugLog("AnimalDashboard.toHerdDisplay", "retorno=$result")
        return result
    }
    val result = HerdDisplay(
        rows = porFazenda.map { summary ->
            HerdSummaryRow(
                farmName = summary.fazenda.ifBlank { "N/A" },
                males = summary.machos.toString(),
                females = summary.femeas.toString(),
                total = summary.total.toString()
            )
        },
        totalMales = quantidadeMachos.toString(),
        totalFemales = quantidadeFemeas.toString(),
        totalAnimals = quantidadeAnimais.toString()
    )
    debugLog("AnimalDashboard.toHerdDisplay", "retorno=$result")
    return result
}

@Composable
fun HerdSummaryTable(
    rows: List<HerdSummaryRow>,
    totalMales: String,
    totalFemales: String,
    totalAnimals: String
) {
    debugLog("HerdSummaryTable", "entrada; linhas=${rows.size}; totalMales=$totalMales; totalFemales=$totalFemales; totalAnimals=$totalAnimals")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
            .background(Color.White)
    ) {
        TableRow(
            cells = listOf("FAZENDA", "MACHOS", "FEMEAS", "TOTAL"),
            isHeader = true
        )

        rows.forEachIndexed { index, row ->
            TableRow(
                cells = listOf(
                    row.farmName,
                    row.males,
                    row.females,
                    row.total
                ),
                isHeader = false,
                zebra = index % 2 == 0
            )
        }

        TableRow(
            cells = listOf(
                "TOTAL",
                totalMales,
                totalFemales,
                totalAnimals
            ),
            isHeader = true
        )
    }
    debugLog("HerdSummaryTable", "retorno=Unit; linhas renderizadas=${rows.size}")
}

@Composable
private fun TableRow(
    cells: List<String>,
    isHeader: Boolean,
    zebra: Boolean = false
) {
    debugLog("TableRow", "entrada; cells=$cells; isHeader=$isHeader; zebra=$zebra")
    val backgroundColor = when {
        isHeader -> Color(0xFFE8F5E9)
        zebra -> Color(0xFFF8F8F8)
        else -> Color.White
    }
    val cellWeight = 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .heightIn(min = if (isHeader) 28.dp else 22.dp)
    ) {
        cells.forEachIndexed { index, cell ->
            val weight = if (index == 0) 1.9f else 1f
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .border(0.5.dp, Color(0xFFBDBDBD)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = cell,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.Black,
                    fontWeight = if (isHeader) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
    debugLog("TableRow", "retorno=Unit; quantidadeDeCelulas=${cells.size}")
}

fun main() = application {
    debugLog("main", "entrada; arquivo=Main.kt; camada=Frontend; iniciando aplicação desktop")
    Window(onCloseRequest = ::exitApplication, title = "Bezerra Ranch") {
        App()
    }
    debugLog("main", "retorno=Unit; application encerrada")
}

/** Logger de desenvolvimento: usa stdout para aparecer no terminal do Gradle/IDE. */
private fun debugLog(function: String, message: String) {
    println("[DEBUG][Frontend][Main.kt][$function][thread=${Thread.currentThread().name}] $message")
}
