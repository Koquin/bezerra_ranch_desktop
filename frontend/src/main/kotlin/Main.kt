import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
// removed asImageBitmap usage; using loadImageBitmap instead
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import backend.PdfExporter
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

@Composable
fun App() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var hovered by remember { mutableStateOf("") }
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
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start) {
                    // Left side: navigation buttons
                    for (name in navItems) {
                        NavSquareButton(name = name, hovered = hovered, onHover = { hovered = it }) {
                            try {
                                // placeholder navigation
                                throw NotImplementedError("Tela ainda em construção")
                            } catch (e: Throwable) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Erro ao navegar para outra tela: A tela ainda está em construção")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Right side: export button
                Row(modifier = Modifier.wrapContentWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = {
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
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Janela não encontrada para captura")
                                }
                            }
                        } catch (e: Throwable) {
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
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavSquareButton(name: String, hovered: String, onHover: (String) -> Unit, onClick: () -> Unit) {
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
        try {
            val p = assets[name]
            if (p != null) {
                val f = java.io.File(p)
                if (f.exists()) {
                    loadImageBitmap(f.inputStream())
                } else null
            } else null
        } catch (e: Throwable) {
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
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Bezerra Ranch") {
        App()
    }
}
