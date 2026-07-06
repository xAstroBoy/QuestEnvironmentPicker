package dev.codex.questhomeswitcher.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.codex.questhomeswitcher.R
import dev.codex.questhomeswitcher.domain.HomeEnvironment

private val Mint = Color(0xFF5EE0B8)
private val Cyan = Color(0xFF61D9FF)
private val Amber = Color(0xFFF2C94C)
private val Violet = Color(0xFF8792FF)
private val Ink = Color(0xFF070A10)
private val VoidPanel = Color(0xFF0E141D)
private val Glass = Color(0xCC121A25)
private val GlassRaised = Color(0xFF202A39)
private val TextSoft = Color(0xFFB8C2D2)

private val AppColors = darkColorScheme(
    primary = Mint,
    secondary = Amber,
    tertiary = Violet,
    surface = VoidPanel,
    background = Ink,
    surfaceVariant = GlassRaised,
)

@Composable
fun HomeSwitcherApp(
    onActivationStarted: () -> Unit = {},
    viewModel: HomeSwitcherViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = AppColors) {
        SpatialHomeSwitcherScreen(
            state = state,
            formatSize = viewModel::formatSize,
            onRefresh = viewModel::refresh,
            onRequestShizuku = viewModel::requestShizukuPermission,
            onSelect = viewModel::select,
            onActivate = {
                viewModel.activateSelected()
                onActivationStarted()
            },
            onRestart = viewModel::restartQuest,
        )
    }
}

@Composable
private fun SpatialHomeSwitcherScreen(
    state: HomeSwitcherUiState,
    formatSize: (Long) -> String,
    onRefresh: () -> Unit,
    onRequestShizuku: () -> Unit,
    onSelect: (HomeEnvironment) -> Unit,
    onActivate: () -> Unit,
    onRestart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF070A10),
                        Color(0xFF101926),
                        Color(0xFF161128),
                    ),
                ),
            ),
    ) {
        SpatialBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SpatialHeader(
                state = state,
                onRefresh = onRefresh,
                onRequestShizuku = onRequestShizuku,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                CommandRail(
                    state = state,
                    onRequestShizuku = onRequestShizuku,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .width(86.dp)
                        .fillMaxHeight(),
                )

                EnvironmentDeck(
                    homes = state.homes,
                    selected = state.selected,
                    activeHome = state.activeHome,
                    formatSize = formatSize,
                    onSelect = onSelect,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                )

                HologramStage(
                    state = state,
                    onActivate = onActivate,
                    onRestart = onRestart,
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun SpatialBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val gridColor = Color(0x1A5EE0B8)
        val horizonY = size.height * 0.72f
        var x = -size.width
        while (x < size.width * 2f) {
            drawLine(
                color = gridColor,
                start = Offset(x, size.height),
                end = Offset(size.width * 0.5f, horizonY),
                strokeWidth = 1.2f,
            )
            x += 96f
        }
        var y = horizonY
        var step = 32f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += step
            step *= 1.18f
        }
        drawLine(
            color = Color(0x335EE0B8),
            start = Offset(0f, horizonY),
            end = Offset(size.width, horizonY),
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun SpatialHeader(
    state: HomeSwitcherUiState,
    onRefresh: () -> Unit,
    onRequestShizuku: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .shadow(18.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60B1018))
            .border(1.dp, Color(0x335EE0B8), RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Quest Environment Picker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                "Spatial environment console",
                color = TextSoft,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        MetricChip("${state.homes.size}", "homes")
        Spacer(Modifier.width(10.dp))
        StatusPill(
            label = when {
                state.rootReady -> "Root direct"
                state.shizukuReady -> "Shizuku online"
                else -> "Shizuku offline"
            },
            positive = state.rootReady || state.shizukuReady,
            onClick = onRequestShizuku,
        )
        Spacer(Modifier.width(10.dp))
        IconButtonFrame(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Mint)
        }
    }
}

@Composable
private fun AppMark() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(12.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF213041), Color(0xFF0D121A)),
                ),
            )
            .border(1.dp, Color(0x665EE0B8), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(29.dp)
                .graphicsLayer {
                    rotationZ = -8f
                    shadowElevation = 10f
                }
                .clip(RoundedCornerShape(4.dp))
                .background(Mint),
        )
        Box(
            modifier = Modifier
                .size(19.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 7.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF07100D)),
        )
        Icon(Icons.Rounded.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp))
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Amber),
        )
    }
}

@Composable
private fun MetricChip(value: String, label: String) {
    Column(
        modifier = Modifier
            .height(46.dp)
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF171F2B))
            .border(1.dp, Color(0x334B5B72), RoundedCornerShape(8.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Black)
        Text(label, color = TextSoft, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusPill(label: String, positive: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (positive) Mint else Amber),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Icon(
            imageVector = if (positive) Icons.Rounded.Security else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (positive) Mint else Amber,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold, color = if (positive) Mint else Amber)
    }
}

@Composable
private fun IconButtonFrame(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141D28))
            .border(1.dp, Color(0x665EE0B8), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun CommandRail(
    state: HomeSwitcherUiState,
    onRequestShizuku: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .graphicsLayer {
                rotationY = 5f
                cameraDistance = 18f * density
            }
            .shadow(20.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60B1018))
            .border(1.dp, Color(0x244B5B72), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RailButton(active = true, onClick = {}) {
            Icon(Icons.Rounded.Home, contentDescription = null, tint = Mint)
        }
        RailButton(active = state.shizukuReady, onClick = onRequestShizuku) {
            Icon(
                imageVector = if (state.shizukuReady) Icons.Rounded.Security else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (state.shizukuReady) Mint else Amber,
            )
        }
        RailButton(active = false, onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Cyan)
        }
    }
}

@Composable
private fun RailButton(active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(if (active) 12.dp else 4.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0xFF1E3440) else Color(0xFF151D28))
            .border(1.dp, if (active) Mint else Color(0x294B5B72), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun EnvironmentDeck(
    homes: List<HomeEnvironment>,
    selected: HomeEnvironment?,
    activeHome: HomeEnvironment?,
    formatSize: (Long) -> String,
    onSelect: (HomeEnvironment) -> Unit,
    modifier: Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val visibleHomes = remember(homes, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) homes else homes.filter { home ->
            home.displayName.contains(query, ignoreCase = true) ||
                home.packageName?.contains(query, ignoreCase = true) == true
        }
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                rotationY = -4f
                cameraDistance = 18f * density
            }
            .shadow(28.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xE61A2735),
                        Color(0xE60D151F),
                        Color(0xF2070B11),
                    ),
                ),
            )
            .border(1.dp, Color(0x5568DCC2), RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text("Environment Deck", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Select a spatial shell", color = TextSoft, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search homes") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        )
        Spacer(Modifier.height(8.dp))

        if (visibleHomes.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 8.dp),
            ) {
                items(visibleHomes, key = { it.apkPath }) { home ->
                    EnvironmentTile(
                        home = home,
                        selected = home.apkPath == selected?.apkPath,
                        active = home.apkPath == activeHome?.apkPath,
                        size = formatSize(home.sizeBytes),
                        onClick = { onSelect(home) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvironmentTile(home: HomeEnvironment, selected: Boolean, active: Boolean, size: String, onClick: () -> Unit) {
    val edge = if (selected) Mint else Color(0x334B5B72)
    val glass = if (selected) {
        Brush.linearGradient(
            listOf(Color(0xEE3B536A), Color(0xE6243448), Color(0xF00E1722)),
        )
    } else {
        Brush.linearGradient(
            listOf(Color(0xD936465B), Color(0xE6202A39), Color(0xF0101721)),
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (selected) 120.dp else 108.dp)
            .graphicsLayer {
                scaleX = 1f
                scaleY = 1f
                rotationX = if (selected) 0f else 0.8f
                rotationY = if (selected) 0f else -2.4f
                cameraDistance = 18f * density
                shadowElevation = if (selected) 22f else 9f
            }
            .border(1.dp, edge, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(glass),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xB3E8FFFF), Color.Transparent),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(if (selected) 18.dp else 10.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, if (selected) Color(0x775EE0B8) else Color(0x222A94A4)),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PreviewBox(
                    home.displayName,
                    home.previewPath,
                    Modifier
                        .size(if (selected) 92.dp else 82.dp)
                        .shadow(if (selected) 14.dp else 7.dp, RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        home.displayName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (active) "ACTIVE  •  $size" else size,
                        color = if (active) Mint else TextSoft,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                if (active) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Mint, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun HologramStage(
    state: HomeSwitcherUiState,
    onActivate: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .graphicsLayer {
                rotationY = 3f
                rotationX = 1.5f
                cameraDistance = 18f * density
            }
            .shadow(34.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60A0E15))
            .border(1.dp, Color(0x335EE0B8), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StagePreview(
            home = state.selected,
            active = state.selected?.apkPath == state.activeHome?.apkPath,
            modifier = Modifier.weight(1f),
        )
        SpatialActionBar(state, onActivate, onRestart)

        if (state.log.isNotBlank()) {
            Text(
                text = state.log,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE05080D))
                    .border(1.dp, Color(0x224B5B72), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                color = Color(0xFFE2E6EF),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StagePreview(home: HomeEnvironment?, active: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF080B10))
            .border(1.dp, Color(0x294B5B72), RoundedCornerShape(8.dp)),
    ) {
        if (home == null) {
            EmptyState()
        } else {
            PreviewBox(home.displayName, home.previewPath, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0x885EE0B8),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 3f,
                )
                drawLine(
                    color = Color(0x445EE0B8),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = Color(0x335EE0B8),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.18f, size.height),
                    strokeWidth = 1.5f,
                )
                drawLine(
                    color = Color(0x337B8DFF),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width * 0.82f, size.height),
                    strokeWidth = 1.5f,
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x00000000),
                                Color(0x22000000),
                                Color(0xEE030609),
                            ),
                        ),
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(28.dp),
            ) {
                Text(
                    text = if (active) "ACTIVE ENVIRONMENT" else "SELECTED TARGET",
                    color = if (active) Mint else Cyan,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    home.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    home.apkPath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFFD2D7DF),
                )
            }
        }
    }
}

@Composable
private fun SpatialActionBar(
    state: HomeSwitcherUiState,
    onActivate: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC111923))
            .border(1.dp, Color(0x224B5B72), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onActivate,
                enabled = state.selected != null && (state.rootReady || state.shizukuReady) && !state.isBusy,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Mint,
                    contentColor = Color(0xFF07100D),
                    disabledContainerColor = Color(0xFF38414C),
                    disabledContentColor = Color(0xFF8B96A6),
                ),
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF07100D))
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Activate", fontWeight = FontWeight.Black)
            }

            if (state.showRestartAction) {
                OutlinedButton(
                    onClick = onRestart,
                    enabled = state.shizukuReady && !state.isBusy,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Amber),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Amber)
                    Spacer(Modifier.width(8.dp))
                    Text("Restart", color = Amber, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (state.rootReady || state.shizukuReady) Icons.Rounded.Security else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (state.rootReady || state.shizukuReady) Mint else Amber,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            state.message,
            color = Color(0xFFD2D7DF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PreviewBox(name: String, path: String?, modifier: Modifier) {
    val bitmap = remember(path) {
        path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1B2634),
                        Color(0xFF080C12),
                    ),
                ),
            )
            .border(1.dp, Color(0x224B5B72), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            ThemedHomeArtwork(name)
        }
    }
}

@Composable
private fun ThemedHomeArtwork(name: String) {
    val key = name.lowercase()
    Canvas(Modifier.fillMaxSize()) {
        when {
            key.contains("space") || key.contains("station") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF050A18), Color(0xFF12294A))),
                    size = size,
                )
                drawCircle(Color(0xFF9EDBFF), radius = size.minDimension * 0.14f, center = Offset(size.width * 0.76f, size.height * 0.25f))
                drawCircle(Color(0x66FFFFFF), radius = size.minDimension * 0.035f, center = Offset(size.width * 0.22f, size.height * 0.22f))
                drawCircle(Color(0x88FFFFFF), radius = size.minDimension * 0.025f, center = Offset(size.width * 0.45f, size.height * 0.14f))
                drawCircle(Color(0x77FFFFFF), radius = size.minDimension * 0.02f, center = Offset(size.width * 0.62f, size.height * 0.48f))
                drawRect(Color(0xFFE7F7FF), topLeft = Offset(size.width * 0.28f, size.height * 0.55f), size = Size(size.width * 0.44f, size.height * 0.08f))
                drawRect(Color(0xFF5EE0B8), topLeft = Offset(size.width * 0.46f, size.height * 0.38f), size = Size(size.width * 0.08f, size.height * 0.34f))
                drawCircle(Color(0xFF0A0E14), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.5f, size.height * 0.55f))
                drawLine(Color(0xAA61D9FF), Offset(size.width * 0.15f, size.height * 0.78f), Offset(size.width * 0.85f, size.height * 0.78f), strokeWidth = size.minDimension * 0.025f)
            }
            key.contains("polar") || key.contains("village") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF18305A), Color(0xFF77C7D8), Color(0xFFEAF9FF))),
                    size = size,
                )
                drawLine(Color(0xAA5EE0B8), Offset(0f, size.height * 0.2f), Offset(size.width, size.height * 0.35f), strokeWidth = size.minDimension * 0.06f)
                drawLine(Color(0x887B8DFF), Offset(0f, size.height * 0.3f), Offset(size.width, size.height * 0.18f), strokeWidth = size.minDimension * 0.045f)
                drawCircle(Color(0xEEFFFFFF), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.2f, size.height * 1.05f))
                drawCircle(Color(0xFFE4F5FF), radius = size.minDimension * 0.5f, center = Offset(size.width * 0.8f, size.height * 1.0f))
                drawRect(Color(0xFF4B2F26), topLeft = Offset(size.width * 0.34f, size.height * 0.55f), size = Size(size.width * 0.28f, size.height * 0.22f))
                drawRect(Color(0xFFF2C94C), topLeft = Offset(size.width * 0.44f, size.height * 0.62f), size = Size(size.width * 0.08f, size.height * 0.08f))
                drawLine(Color.White, Offset(size.width * 0.30f, size.height * 0.56f), Offset(size.width * 0.48f, size.height * 0.42f), strokeWidth = size.minDimension * 0.06f)
                drawLine(Color.White, Offset(size.width * 0.66f, size.height * 0.56f), Offset(size.width * 0.48f, size.height * 0.42f), strokeWidth = size.minDimension * 0.06f)
            }
            key.contains("winter") || key.contains("loft") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF101B36), Color(0xFF415B7D), Color(0xFFD8EBF6))),
                    size = size,
                )
                drawCircle(Color(0x55FFFFFF), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.78f, size.height * 0.22f))
                drawCircle(Color(0xFFEAF7FF), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.18f, size.height * 1.0f))
                drawCircle(Color(0xFFFFFFFF), radius = size.minDimension * 0.5f, center = Offset(size.width * 0.78f, size.height * 1.02f))
                drawRect(Color(0xFF3B261F), topLeft = Offset(size.width * 0.28f, size.height * 0.5f), size = Size(size.width * 0.42f, size.height * 0.26f))
                drawLine(Color(0xFFE6F4FF), Offset(size.width * 0.22f, size.height * 0.51f), Offset(size.width * 0.49f, size.height * 0.32f), strokeWidth = size.minDimension * 0.07f)
                drawLine(Color(0xFFE6F4FF), Offset(size.width * 0.76f, size.height * 0.51f), Offset(size.width * 0.49f, size.height * 0.32f), strokeWidth = size.minDimension * 0.07f)
                drawRect(Color(0xFFFFD36B), topLeft = Offset(size.width * 0.42f, size.height * 0.6f), size = Size(size.width * 0.12f, size.height * 0.12f))
                drawLine(Color(0xFF5EE0B8), Offset(size.width * 0.16f, size.height * 0.82f), Offset(size.width * 0.84f, size.height * 0.82f), strokeWidth = size.minDimension * 0.025f)
            }
            key.contains("dome") || key.contains("environment") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF10152A), Color(0xFF143645), Color(0xFF091014))),
                    size = size,
                )
                val left = Offset(size.width * 0.16f, size.height * 0.72f)
                val top = Offset(size.width * 0.5f, size.height * 0.26f)
                val right = Offset(size.width * 0.84f, size.height * 0.72f)
                drawLine(Color(0xCC5EE0B8), left, top, strokeWidth = size.minDimension * 0.025f)
                drawLine(Color(0xCC5EE0B8), top, right, strokeWidth = size.minDimension * 0.025f)
                drawLine(Color(0x885EE0B8), Offset(size.width * 0.28f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.26f), strokeWidth = size.minDimension * 0.015f)
                drawLine(Color(0x885EE0B8), Offset(size.width * 0.72f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.26f), strokeWidth = size.minDimension * 0.015f)
                drawLine(Color(0x665EE0B8), Offset(size.width * 0.16f, size.height * 0.72f), Offset(size.width * 0.84f, size.height * 0.72f), strokeWidth = size.minDimension * 0.018f)
                drawCircle(Color(0xAAF2C94C), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.5f, size.height * 0.58f))
                drawLine(Color(0x777B8DFF), Offset(0f, size.height * 0.24f), Offset(size.width, size.height * 0.14f), strokeWidth = size.minDimension * 0.04f)
            }
            else -> {
                drawRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF1B2634), Color(0xFF080C12))),
                    size = size,
                )
                drawCircle(Color(0x335EE0B8), radius = size.minDimension * 0.34f, center = Offset(size.width * 0.7f, size.height * 0.25f))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color(0xFF7E8797), modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(10.dp))
            Text("No home APKs found", color = Color(0xFFD2D7DF))
        }
    }
}
