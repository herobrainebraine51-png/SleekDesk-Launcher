package com.example.sleekdesklauncher

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appManager = AppManager(this)
        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

        setContent {
            var isDesktopMode by remember { mutableStateOf(prefs.getBoolean("desktop_mode", false)) }
            var selectedIconPack by remember { mutableStateOf(prefs.getString("icon_pack", null)) }
            var showIconDialog by remember { mutableStateOf(false) }

            val apps = remember(selectedIconPack) {
                appManager.getInstalledApps(selectedIconPack)
            }
            val iconPacks = remember { appManager.getInstalledIconPacks() }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                if (isDesktopMode) {
                    DesktopModeScreen(
                        apps = apps,
                        onSwitchMode = {
                            isDesktopMode = false
                            prefs.edit().putBoolean("desktop_mode", false).apply()
                        },
                        onOpenIconSettings = { showIconDialog = true }
                    )
                } else {
                    SleekLauncherScreen(
                        apps = apps,
                        onSwitchMode = {
                            isDesktopMode = true
                            prefs.edit().putBoolean("desktop_mode", true).apply()
                        },
                        onOpenIconSettings = { showIconDialog = true }
                    )
                }

                if (showIconDialog) {
                    IconPackDialog(
                        iconPacks = iconPacks,
                        currentPack = selectedIconPack,
                        onSelectPack = { pkg ->
                            selectedIconPack = pkg
                            prefs.edit().putString("icon_pack", pkg).apply()
                            showIconDialog = false
                        },
                        onDismiss = { showIconDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun SleekLauncherScreen(
    apps: List<AppInfo>,
    onSwitchMode: () -> Unit,
    onOpenIconSettings: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onOpenIconSettings) {
                Text("🎨 الأيقونات", color = Color.White, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onSwitchMode,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0, 120, 212, 220))
            ) {
                Text("وضع الكمبيوتر 💻", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 82.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                AppIconItem(app = app) {
                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                        ?.let { context.startActivity(it) }
                }
            }
        }
    }
}

@Composable
fun DesktopModeScreen(
    apps: List<AppInfo>,
    onSwitchMode: () -> Unit,
    onOpenIconSettings: () -> Unit
) {
    val context = LocalContext.current
    var isStartMenuOpen by remember { mutableStateOf(false) }
    val currentTime = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 90.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps.take(15)) { app ->
                AppIconItem(app = app) {
                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                        ?.let { context.startActivity(it) }
                }
            }
        }

        if (isStartMenuOpen) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 64.dp)
                    .width(340.dp)
                    .height(450.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(26, 26, 26, 235))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التطبيقات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            isStartMenuOpen = false
                            onOpenIconSettings()
                        }) {
                            Text("🎨 الأيقونات", color = Color(76, 194, 255))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(apps) { app ->
                            AppIconItem(app = app) {
                                isStartMenuOpen = false
                                context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    ?.let { context.startActivity(it) }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(54.dp)
                .background(Color(20, 20, 20, 210))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { isStartMenuOpen = !isStartMenuOpen },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0, 120, 212))
                ) {
                    Text("⊞ ابدأ", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onOpenIconSettings) {
                    Text("🎨", fontSize = 18.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSwitchMode) {
                    Text("📱 وضع الهاتف", color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = currentTime, color = Color.LightGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun IconPackDialog(
    iconPacks: List<IconPackInfo>,
    currentPack: String?,
    onSelectPack: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(32, 32, 32),
@Composable
fun AppIconItem(app: AppInfo, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Image(
            bitmap = app.icon.toBitmap(128, 128).asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
