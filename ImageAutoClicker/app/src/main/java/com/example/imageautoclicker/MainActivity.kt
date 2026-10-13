package com.example.imageautoclicker

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// data satu plan
data class Plan(
    val name: String,
    val description: String,
    val createdAt: String
)

class MainActivity : ComponentActivity() {
    // bertambah setiap aplikasi kembali tampil, dipakai untuk cek ulang izin
    private val resumeTick = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreen(resumeTick = resumeTick.value)
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.value = resumeTick.value + 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(resumeTick: Int) {
    // 0 = Plan, 1 = FAQ, 2 = Profile
    var selectedTab by remember { mutableStateOf(0) }

    // daftar plan (berubah otomatis memperbarui tampilan)
    val plans = remember { mutableStateListOf<Plan>() }

    // state popup Create New Plan
    var showDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }

    // cek izin, dihitung ulang setiap aplikasi kembali tampil
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var permissionDismissed by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh = refresh + 1 }

    val overlayOk = remember(resumeTick, refresh) { Settings.canDrawOverlays(context) }
    val accessibilityOk = remember(resumeTick, refresh) {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val me = ComponentName(context, ClickService::class.java)
        enabled.split(":").any { ComponentName.unflattenFromString(it) == me }
    }
    val notifOk = remember(resumeTick, refresh) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
    val allGranted = overlayOk && accessibilityOk && notifOk

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("IM Clicker") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF703810),
                    titleContentColor = Color.White
                ),
                actions = {
                    FilledIconButton(
                        onClick = { /* TODO: buka menu */ },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF6B4E98),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        nameInput = ""
                        descInput = ""
                        showDialog = true
                    },
                    containerColor = Color(0xFFEBDDFF),
                    contentColor = Color(0xFF21005D)
                ) {
                    Text("Create New Plan")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF3EDF7)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Plan") },
                    label = { Text("Plan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "FAQ") },
                    label = { Text("FAQ") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == 0) {
                if (plans.isEmpty()) {
                    // tampilan kosong
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(96.dp)
                        )
                        Text("No plans available", color = Color.Gray)
                    }
                } else {
                    // daftar plan dalam 2 kolom
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = 96.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(plans) { plan ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F2F8)),
                                border = BorderStroke(1.dp, Color(0xFFC9C5CA)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        plan.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (plan.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            plan.description,
                                            color = Color.DarkGray,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        plan.createdAt,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (overlayOk) {
                                                // tampilkan panel melayang, lalu sembunyikan aplikasi
                                                val intent = Intent(context, OverlayService::class.java)
                                                intent.putExtra("plan_name", plan.name)
                                                context.startService(intent)
                                                (context as? Activity)?.moveTaskToBack(true)
                                            } else {
                                                // izin overlay belum aktif, munculkan popup izin lagi
                                                permissionDismissed = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEBDDFF),
                                            contentColor = Color(0xFF703810)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Edit & Run", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("FAQ")
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Profile")
                }
            }
        }
    }

    // popup Create New Plan
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Buat Plan Baru") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama plan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Deskripsi (opsional)") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val now = LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                        )
                        // plan terbaru ditaruh paling depan
                        plans.add(0, Plan(nameInput.trim(), descInput.trim(), now))
                        showDialog = false
                    },
                    enabled = nameInput.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // popup izin yang dibutuhkan
    if (!allGranted && !permissionDismissed) {
        AlertDialog(
            onDismissRequest = { permissionDismissed = true },
            title = { Text("Izin Dibutuhkan") },
            text = {
                Column {
                    Text(
                        "Aplikasi butuh izin berikut supaya panel bisa tampil di atas aplikasi lain dan bisa menekan layar otomatis.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionRow(
                        "Tampil di atas aplikasi lain",
                        "Untuk menampilkan panel melayang",
                        overlayOk
                    ) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + context.packageName)
                        )
                        context.startActivity(intent)
                    }
                    PermissionRow(
                        "Layanan aksesibilitas",
                        "Untuk menekan layar otomatis. Kalau tombolnya abu-abu: Info aplikasi, titik tiga, Izinkan setelan terbatas",
                        accessibilityOk
                    ) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        PermissionRow(
                            "Notifikasi",
                            "Penanda saat plan berjalan",
                            notifOk
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { permissionDismissed = true }) {
                    Text("Nanti")
                }
            }
        )
    }
}

// satu baris izin: judul, keterangan, dan tombol Aktifkan (atau tulisan Aktif)
@Composable
fun PermissionRow(title: String, desc: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }
        if (granted) {
            Text("Aktif", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        } else {
            TextButton(onClick = onClick) {
                Text("Aktifkan")
            }
        }
    }
}
