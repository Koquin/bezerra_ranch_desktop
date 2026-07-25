import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import backend.PdfAgeSummary
import backend.PdfDashboardReport
import backend.PdfExporter
import backend.PdfFarmSummary
import backend.PdfHerdSection
import backend.controller.AnimalController
import backend.controller.SynchronizationController
import backend.model.Animal
import backend.sync.SyncTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    debugLog("App", "entrada; arquivo=Main.kt; camada=Frontend")
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val appFocusRequester = remember { FocusRequester() }
    val animalController = remember { AnimalController() }
    val synchronizationController = remember { SynchronizationController() }
    var hovered by remember { mutableStateOf("") }
    var animalRows by remember { mutableStateOf<List<AnimalSummaryRow>>(emptyList()) }
    var poAnimalRows by remember { mutableStateOf<List<AnimalSummaryRow>>(emptyList()) }
    var commercialAnimalRows by remember { mutableStateOf<List<AnimalSummaryRow>>(emptyList()) }
    var ageRows by remember { mutableStateOf<List<AgeDistributionRow>>(emptyList()) }
    var animalsLoaded by remember { mutableStateOf(false) }
    var animalLoadError by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    suspend fun updateAnimalTable() {
        debugLog("App.updateAnimalTable", "entrada; controller=AnimalController; lendo a tabela animal do SQLite")
        println("-----------(animalRows)--------- INICIO da atualização")
        try {
            val animals = withContext(Dispatchers.IO) { animalController.listar() }
            println("-----------(animalRows)--------- animais recebidos=${animals.size}")
            animalRows = animals.toAnimalSummaryRows()
            poAnimalRows = animals
                .filter { it.grauSanguineo?.trim()?.uppercase() == "PO" }
                .toAnimalSummaryRows()
            commercialAnimalRows = animals
                .filterNot { it.grauSanguineo?.trim()?.uppercase() == "PO" }
                .toAnimalSummaryRows()
            ageRows = animals.toAgeDistributionRows()
            println("-----------(animalRows)--------- linhas resumidas=${animalRows.size} dados=$animalRows")
            animalsLoaded = true
            animalLoadError = null
            debugLog("App.updateAnimalTable", "retorno=Unit; animaisCarregados=${animalRows.size}")
        } catch (error: Throwable) {
            println("-----------(animalRows)--------- ERRO ao buscar/converter: ${error.stackTraceToString()}")
            animalLoadError = error.message ?: "erro desconhecido"
            throw error
        }
    }

    LaunchedEffect(Unit) {
        appFocusRequester.requestFocus()
        debugLog("App.LaunchedEffect", "entrada; variável-chave=Unit; carregamento inicial da tabela")
        try {
            updateAnimalTable()
            debugLog("App.LaunchedEffect", "retorno=Unit; carregamento inicial concluído")
        } catch (error: Throwable) {
            debugLog("App.LaunchedEffect", "retorno=Unit; falha no carregamento; mensagem=${error.message}")
        }
    }
    val navItems = listOf(
        "GRÁFICOS",
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
                .focusRequester(appFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val distance = when (event.key) {
                        Key.DirectionDown -> 120
                        Key.DirectionUp -> -120
                        Key.PageDown -> 480
                        Key.PageUp -> -480
                        else -> return@onPreviewKeyEvent false
                    }
                    coroutineScope.launch {
                        scrollState.animateScrollTo(
                            (scrollState.value + distance).coerceIn(0, scrollState.maxValue)
                        )
                    }
                    true
                }
                .padding(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start) {
                    // Left side: navigation buttons
                    for (name in navItems) {
                        NavSquareButton(name = name, hovered = hovered, onHover = { hovered = it }) {
                            debugLog("App.navigationClick", "entrada; nome=$name")
                            try {
                                if (name == "GRÁFICOS") {
                                    debugLog("App.navigationClick", "retorno=Unit; tela de gráficos já está ativa")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Você já está na tela de gráficos")
                                    }
                                } else {
                                    // placeholder navigation
                                    throw NotImplementedError("Tela ainda em construção")
                                }
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
                        modifier = Modifier.pointerMoveFilter(
                            onEnter = {
                                hovered = "Atualiza os gráficos com os dados do banco de dados local"
                                false
                            },
                            onExit = {
                                hovered = ""
                                false
                            }
                        ),
                        enabled = !isDownloading,
                        onClick = {
                            coroutineScope.launch {
                                debugLog("App.atualizarClick", "entrada; isDownloading=$isDownloading")
                                try {
                                    updateAnimalTable()
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
                        modifier = Modifier.pointerMoveFilter(
                            onEnter = {
                                hovered = "Baixa os dados da nuvem e salva no banco de dados local"
                                false
                            },
                            onExit = {
                                hovered = ""
                                false
                            }
                        ),
                        enabled = !isDownloading,
                        onClick = {
                            coroutineScope.launch {
                                debugLog("App.baixarDadosClick", "entrada; tabela=${SyncTable.ANIMAL}; isDownloading=$isDownloading")
                                isDownloading = true
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        synchronizationController.baixarTabela(SyncTable.ANIMAL)
                                    }
                                    updateAnimalTable()
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
                        debugLog("App.exportarPdfClick", "entrada; escolhendo destino do relatório")
                        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        val outFile = choosePdfDestination("export_report_$ts.pdf")
                        if (outFile == null) {
                            debugLog("App.exportarPdfClick", "retorno=Unit; seleção cancelada")
                        } else {
                            coroutineScope.launch {
                                try {
                                val report = PdfDashboardReport(
                                    totalHerd = PdfHerdSection("REBANHO TOTAL", animalRows.toPdfFarmRows(false)),
                                    poHerd = PdfHerdSection("REBANHO P.O.", poAnimalRows.toPdfFarmRows(false)),
                                    commercialHerd = PdfHerdSection("REBANHO COMERCIAL", commercialAnimalRows.toPdfFarmRows(false)),
                                    farmSummary = animalRows.toPdfFarmRows(true),
                                    ageSummary = ageRows.map { row ->
                                        PdfAgeSummary(
                                            farm = row.fazenda,
                                            males = row.groups.map { it.machos },
                                            females = row.groups.map { it.femeas }
                                        )
                                    },
                                    ageLabels = ageRangeLabels
                                )
                                withContext(Dispatchers.IO) {
                                    PdfExporter.exportDashboardAsPdf(report, outFile)
                                }
                                snackbarHostState.showSnackbar("Exportado para ${outFile.absolutePath}")
                                debugLog("App.exportarPdfClick", "retorno=Unit; arquivo=${outFile.absolutePath}")
                                } catch (e: Throwable) {
                                    debugLog("App.exportarPdfClick", "erro; mensagem=${e.message}")
                                    snackbarHostState.showSnackbar("Erro ao exportar: ${e.message}")
                                }
                            }
                        }
                    }) {
                        Text("Exportar PDF")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hover text label
            Text(
                text = if (hovered.isNotBlank()) hovered else " ",
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Middle area with three tall rectangular cards side by side
            Row(modifier = Modifier.fillMaxWidth().height(680.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 1..3) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                        elevation = 4.dp
                    ) {
                        when (i) {
                            1 -> HerdCard(
                                rows = animalRows,
                                title = "REBANHO TOTAL",
                                chartTitle = "Rebanho total",
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                            2 -> HerdCard(
                                rows = poAnimalRows,
                                title = "REBANHO P.O.",
                                chartTitle = "Rebanho P.O.",
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                            else -> HerdCard(
                                rows = commercialAnimalRows,
                                title = "REBANHO COMERCIAL",
                                chartTitle = "Rebanho comercial",
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
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
                            .background(Color(0xFF83E28E), RoundedCornerShape(6.dp))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Animais",
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

                    AnimalTable(
                        rows = animalRows,
                        loaded = animalsLoaded,
                        error = animalLoadError,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFBDBDBD))
                )

                Column(
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgeDistributionHeader()
                    AgeDistributionTable(
                        rows = ageRows,
                        loaded = animalsLoaded,
                        error = animalLoadError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AgeTotalColumnChart(
                rows = ageRows,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            AgeSexClusteredColumnChart(
                rows = ageRows,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .padding(horizontal = 8.dp)
            )
        }
    }
    debugLog("App", "retorno=Unit; composição configurada; linhas=${animalRows.size}; baixando=$isDownloading")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavSquareButton(name: String, hovered: String, onHover: (String) -> Unit, onClick: () -> Unit) {
    debugLog("NavSquareButton", "entrada; name=$name; hovered=$hovered")
    val size = 56.dp
    val assets = mapOf(
        "GRÁFICOS" to "assets/tela_graficos.png",
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
        assets[name]?.let(::loadAssetBitmap)
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

private data class AnimalSummaryRow(
    val fazenda: String,
    val machos: Int,
    val femeas: Int,
    val total: Int
)

private fun List<AnimalSummaryRow>.toPdfFarmRows(includeTotal: Boolean): List<PdfFarmSummary> =
    asSequence()
        .filter { includeTotal || it.fazenda != "Total" }
        .filter { it.fazenda == "Total" || it.total > 0 }
        .map { PdfFarmSummary(it.fazenda, it.machos, it.femeas, it.total) }
        .toList()

private data class AgeSexCount(val machos: Int, val femeas: Int)

private data class AgeDistributionRow(
    val fazenda: String,
    val groups: List<AgeSexCount>
)

private val ageRangeLabels = listOf(
    "0 - 12 MESES",
    "12 - 24 MESES",
    "24 - 36 MESES",
    "36 - 48 MESES",
    "48 - 60 MESES",
    "60 - 72 MESES",
    "> 72 MESES"
)

@Composable
private fun HerdCard(
    rows: List<AnimalSummaryRow>,
    title: String,
    chartTitle: String,
    modifier: Modifier = Modifier
) {
    val totals = rows.lastOrNull { it.fazenda == "Total" }
    val farms = rows
        .filter { it.fazenda != "Total" && it.total > 0 }
        .sortedWith(compareBy<AnimalSummaryRow> { it.total }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.fazenda })
    val totalAnimals = totals?.total ?: 0
    val totalMales = totals?.machos ?: 0
    val totalFemales = totals?.femeas ?: 0

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth().height(76.dp)
        ) {
            DashboardImage(
                path = "assets/grafico.png",
                description = title,
                modifier = Modifier.size(60.dp).align(Alignment.CenterStart)
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = totalAnimals.toString(),
                    color = Color(0xFF3F49FA),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    style = MaterialTheme.typography.h5
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().height(76.dp)) {
            HerdSexSummary(
                label = "Machos",
                imagePath = "assets/rebanho_machos.png",
                amount = totalMales,
                total = totalAnimals,
                backgroundColor = Color(0xFFDDEAF6),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            HerdSexSummary(
                label = "Fêmeas",
                imagePath = "assets/rebanho_femeas.png",
                amount = totalFemales,
                total = totalAnimals,
                backgroundColor = Color(0xFFFECCFF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = chartTitle,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            style = MaterialTheme.typography.subtitle1
        )
        Spacer(modifier = Modifier.height(10.dp))

        val largestHerd = farms.maxOfOrNull { it.total } ?: 0
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(farms) { _, farm ->
                HorizontalHerdBar(farm = farm, largestHerd = largestHerd)
            }
        }
    }
}

@Composable
private fun HerdSexSummary(
    label: String,
    imagePath: String,
    amount: Int,
    total: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val percentage = if (total == 0) 0 else (amount * 100.0 / total).roundToInt()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.caption)
            DashboardImage(path = imagePath, description = label, modifier = Modifier.size(24.dp))
        }
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$percentage%",
                modifier = Modifier.weight(1f),
                color = Color(0xFF3F49FA),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(text = amount.toString(), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

@Composable
private fun DashboardImage(path: String, description: String, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { loadAssetBitmap(path) }
    Box(
        modifier = modifier
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = description, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.Image, contentDescription = description, tint = Color.Gray)
        }
    }
}

private fun loadAssetBitmap(path: String): ImageBitmap? = runCatching {
    val localFile = File(path)
    val input = if (localFile.isFile) {
        localFile.inputStream()
    } else {
        Thread.currentThread().contextClassLoader
            .getResourceAsStream(path.removePrefix("assets/"))
            ?: return null
    }
    input.use(::loadImageBitmap)
}.getOrNull()

@Composable
private fun HorizontalHerdBar(farm: AnimalSummaryRow, largestHerd: Int) {
    val fraction = if (largestHerd == 0) 0f else farm.total.toFloat() / largestHerd
    Row(
        modifier = Modifier.fillMaxWidth().height(25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = farm.fazenda,
            modifier = Modifier.width(100.dp).padding(end = 6.dp),
            maxLines = 1,
            style = MaterialTheme.typography.caption
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(Color(0xFF3F49FA), RoundedCornerShape(3.dp))
            )
        }
        Text(
            text = farm.total.toString(),
            modifier = Modifier.width(48.dp).padding(start = 6.dp),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            style = MaterialTheme.typography.caption
        )
    }
}

private fun List<Animal>.toAnimalSummaryRows(): List<AnimalSummaryRow> {
    val rowsByFarm = groupBy { animal ->
        animal.fazenda?.trim()?.takeIf(String::isNotEmpty) ?: "Sem fazenda"
    }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .map { (farm, animals) ->
            val males = animals.count { it.sexo?.trim()?.uppercase() == "M" }
            val females = animals.count { it.sexo?.trim()?.uppercase() == "F" }
            AnimalSummaryRow(
                fazenda = farm,
                machos = males,
                femeas = females,
                total = animals.size
            )
        }

    return rowsByFarm + AnimalSummaryRow(
        fazenda = "Total",
        machos = rowsByFarm.sumOf { it.machos },
        femeas = rowsByFarm.sumOf { it.femeas },
        total = rowsByFarm.sumOf { it.total }
    )
}

private fun List<Animal>.toAgeDistributionRows(
    currentDate: LocalDate = LocalDate.now()
): List<AgeDistributionRow> {
    val rowsByFarm = groupBy { animal ->
        animal.fazenda?.trim()?.takeIf(String::isNotEmpty) ?: "Sem fazenda"
    }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .map { (farm, animals) ->
            val counts = MutableList(ageRangeLabels.size) { IntArray(2) }
            animals.forEach { animal ->
                val birthDate = animal.dataNascimento
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDate()
                    ?: return@forEach
                val months = ChronoUnit.MONTHS.between(birthDate, currentDate)
                val groupIndex = when {
                    months < 0 -> null
                    months < 12 -> 0
                    months < 24 -> 1
                    months < 36 -> 2
                    months < 48 -> 3
                    months < 60 -> 4
                    months <= 72 -> 5
                    else -> 6
                } ?: return@forEach
                when (animal.sexo?.trim()?.uppercase()) {
                    "M" -> counts[groupIndex][0]++
                    "F" -> counts[groupIndex][1]++
                }
            }
            AgeDistributionRow(
                fazenda = farm,
                groups = counts.map { AgeSexCount(machos = it[0], femeas = it[1]) }
            )
        }

    return rowsByFarm + AgeDistributionRow(
        fazenda = "Total",
        groups = ageRangeLabels.indices.map { index ->
            AgeSexCount(
                machos = rowsByFarm.sumOf { it.groups[index].machos },
                femeas = rowsByFarm.sumOf { it.groups[index].femeas }
            )
        }
    )
}

@Composable
private fun AgeDistributionHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF83E28E), RoundedCornerShape(6.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Distribuição por fazenda",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color.Black,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            style = MaterialTheme.typography.subtitle1
        )
        EqualWidthRow(
            cells = ageRangeLabels,
            backgroundColor = Color(0x83E28E),
            bold = true,
            height = 34
        )
    }
}

@Composable
private fun AgeDistributionTable(
    rows: List<AgeDistributionRow>,
    loaded: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
            .background(Color.White)
    ) {
        EqualWidthRow(
            cells = List(ageRangeLabels.size) { listOf("MACHO", "FÊMEA") }.flatten(),
            backgroundColor = Color(0xFF808080),
            bold = true,
            height = 28
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(rows) { index, row ->
                EqualWidthRow(
                    cells = row.groups.flatMap { listOf(it.machos.toString(), it.femeas.toString()) },
                    backgroundColor = when {
                        row.fazenda == "Total" -> Color.Black
                        index % 2 == 0 -> Color(0xFFF8F8F8)
                        else -> Color.White
                    },
                    textColor = if (row.fazenda == "Total") Color.White else Color.Black,
                    bold = row.fazenda == "Total",
                    height = if (row.fazenda == "Total") 28 else 24
                )
            }
        }

        val totals = rows.lastOrNull()?.groups
        if (totals != null) {
            EqualWidthRow(
                cells = totals.map { (it.machos + it.femeas).toString() },
                backgroundColor = Color.Black,
                textColor = Color.White,
                bold = true,
                height = 28
            )
        } else {
            Text(
                text = error?.let { "Erro: $it" } ?: if (loaded) "Sem dados" else "Carregando...",
                modifier = Modifier.padding(8.dp),
                color = Color.Gray,
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
private fun AgeSexClusteredColumnChart(
    rows: List<AgeDistributionRow>,
    modifier: Modifier = Modifier
) {
    val totals = rows.lastOrNull { it.fazenda == "Total" }?.groups
        ?: List(ageRangeLabels.size) { AgeSexCount(0, 0) }
    val largestValue = totals.maxOfOrNull { maxOf(it.machos, it.femeas) } ?: 0

    Card(
        modifier = modifier.border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(8.dp)),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Total por sexo",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                ChartLegendItem(color = Color(0xFFFF69B4), label = "FÊMEA")
                Spacer(modifier = Modifier.width(18.dp))
                ChartLegendItem(color = Color(0xFF3F49FA), label = "MACHO")
            }

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                totals.forEachIndexed { index, count ->
                    AgeSexColumnGroup(
                        label = ageRangeLabels[index],
                        females = count.femeas,
                        males = count.machos,
                        largestValue = largestValue,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun AgeTotalColumnChart(
    rows: List<AgeDistributionRow>,
    modifier: Modifier = Modifier
) {
    val totals = rows.lastOrNull { it.fazenda == "Total" }?.groups
        ?: List(ageRangeLabels.size) { AgeSexCount(0, 0) }
    val totalsByAge = totals.map { it.machos + it.femeas }
    val largestValue = totalsByAge.maxOrNull() ?: 0

    Card(
        modifier = modifier.border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(8.dp)),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Total por faixa etária",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                totalsByAge.forEachIndexed { index, total ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            ChartColumn(
                                value = total,
                                largestValue = largestValue,
                                color = Color(0xFF83E28E),
                                width = 46
                            )
                        }
                        Text(
                            text = ageRangeLabels[index],
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                                .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.caption)
    }
}

@Composable
private fun AgeSexColumnGroup(
    label: String,
    females: Int,
    males: Int,
    largestValue: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            ChartColumn(
                value = females,
                largestValue = largestValue,
                color = Color(0xFFFF69B4)
            )
            Spacer(modifier = Modifier.width(6.dp))
            ChartColumn(
                value = males,
                largestValue = largestValue,
                color = Color(0xFF3F49FA)
            )
        }
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth().height(42.dp).padding(top = 6.dp, start = 2.dp, end = 2.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
private fun ChartColumn(value: Int, largestValue: Int, color: Color, width: Int = 30) {
    val barHeight = if (largestValue == 0) 1f else 210f * value / largestValue
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Text(
            text = value.toString(),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            style = MaterialTheme.typography.caption
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(barHeight.dp)
                .background(color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
        )
    }
}

@Composable
private fun EqualWidthRow(
    cells: List<String>,
    backgroundColor: Color,
    bold: Boolean,
    height: Int,
    textColor: Color = Color.Black
) {
    Row(modifier = Modifier.fillMaxWidth().height(height.dp).background(backgroundColor)) {
        cells.forEach { cell ->
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color(0xFFBDBDBD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    color = textColor,
                    fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private fun AnimalTable(
    rows: List<AnimalSummaryRow>,
    loaded: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    debugLog("AnimalTable", "entrada; linhas=${rows.size}; loaded=$loaded; erro=$error")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
            .background(Color.White)
    ) {
        TableRow(
            cells = listOf("FAZENDA", "MACHOS", "FEMEAS", "TOTAL"),
            isHeader = true
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(rows) { index, row ->
                TableRow(
                    cells = listOf(
                        row.fazenda,
                        row.machos.toString(),
                        row.femeas.toString(),
                        row.total.toString()
                    ),
                    isHeader = row.fazenda == "Total",
                    zebra = index % 2 == 0
                )
            }
        }

        Text(
            text = when {
                rows.isNotEmpty() -> "${rows.last().total} animal(is) em ${rows.size - 1} fazenda(s)"
                error != null -> "Não foi possível carregar os animais: $error"
                loaded -> "Nenhum animal encontrado no banco local."
                else -> "Carregando animais..."
            },
            modifier = Modifier.padding(8.dp),
            color = Color(0xFF3F49FA),
            style = MaterialTheme.typography.caption
        )
    }
    debugLog("AnimalTable", "retorno=Unit; linhas renderizadas=${rows.size}")
}

@Composable
private fun TableRow(
    cells: List<String>,
    isHeader: Boolean,
    zebra: Boolean = false
) {
    debugLog("TableRow", "entrada; cells=$cells; isHeader=$isHeader; zebra=$zebra")
    val backgroundColor = when {
        isHeader -> Color.Black
        zebra -> Color(0xFFF8F8F8)
        else -> Color.White
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .height(if (isHeader) 28.dp else 24.dp)
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
                    color = when {
                        isHeader -> Color.White
                        index == 0 -> Color.Black
                        else -> Color(0xFF3F49FA)
                    },
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

private fun choosePdfDestination(suggestedName: String): File? {
    val parent = java.awt.Window.getWindows().firstOrNull { it.isVisible }
    val chooser = JFileChooser().apply {
        dialogTitle = "Salvar relatório em PDF"
        fileFilter = FileNameExtensionFilter("Documento PDF (*.pdf)", "pdf")
        selectedFile = File(suggestedName)
    }
    if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null

    val selected = chooser.selectedFile
    val destination = if (selected.extension.equals("pdf", ignoreCase = true)) {
        selected
    } else {
        File(selected.parentFile, "${selected.name}.pdf")
    }

    if (destination.exists()) {
        val answer = JOptionPane.showConfirmDialog(
            parent,
            "O arquivo ${destination.name} já existe. Deseja substituí-lo?",
            "Confirmar substituição",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        if (answer != JOptionPane.YES_OPTION) return null
    }
    return destination
}

/** Logger de desenvolvimento: usa stdout para aparecer no terminal do Gradle/IDE. */
private fun debugLog(function: String, message: String) {
    println("[DEBUG][Frontend][Main.kt][$function][thread=${Thread.currentThread().name}] $message")
}
