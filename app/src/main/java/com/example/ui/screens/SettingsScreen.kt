package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FileUtils
import com.example.ui.VaultXViewModel
import com.example.ui.components.VaultCard
import com.example.ui.theme.VaultBlack
import com.example.ui.theme.VaultBlackSurface
import com.example.ui.theme.VaultCharcoal
import com.example.ui.theme.VaultCharcoalBorder
import com.example.ui.theme.VaultCharcoalElevated
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultGoldBorder
import com.example.ui.theme.VaultGoldLight
import com.example.ui.theme.VaultGoldSubtle
import com.example.ui.theme.VaultSuccess
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.theme.VaultTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultXViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Security",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VaultGoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Theme Card
            SectionHeader(title = "Appearance & Interface")
            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VaultGoldSubtle)
                            .border(1.dp, VaultGoldBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = VaultGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Matte Black & Metallic Gold",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                        Text(
                            text = "Executive high-security dark visual identity",
                            fontSize = 12.sp,
                            color = VaultTextSecondary
                        )
                    }
                }
            }

            // 2. Storage & Output Folder
            SectionHeader(title = "Output Storage")
            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = VaultGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Device Downloads Folder",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextPrimary
                        )
                    }
                    Text(
                        text = "Downloads/",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultGoldLight
                    )
                    Text(
                        text = "Encrypted (.vaultx) and decrypted files are streamed directly into your public Downloads folder. Zero hidden app storage, instant access from any file manager.",
                        fontSize = 11.sp,
                        color = VaultTextTertiary,
                        lineHeight = 15.sp
                    )
                }
            }

            // 3. Cryptography & Security Details
            SectionHeader(title = "Cryptographic Architecture")
            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CryptoSpecRow(
                        title = "Authenticated Cipher",
                        spec = "AES-256-GCM (128-bit Auth Tag)"
                    )
                    CryptoSpecRow(
                        title = "Key Derivation Function",
                        spec = "PBKDF2-HMAC-SHA256 (100,000 Rounds)"
                    )
                    CryptoSpecRow(
                        title = "Cryptographic Salt",
                        spec = "256-bit CSPRNG SecureRandom per container"
                    )
                    CryptoSpecRow(
                        title = "Streaming Chunking",
                        spec = "64 KB blocks with unique derived IVs & AAD"
                    )
                    CryptoSpecRow(
                        title = "Tamper Resistance",
                        spec = "Full AAD verification on header and chunk stream"
                    )
                    CryptoSpecRow(
                        title = "RAM Protection",
                        spec = "Sensitive password arrays wiped upon key derivation"
                    )
                }
            }

            // 4. Privacy Statement
            SectionHeader(title = "Privacy & Local-Only Guarantee")
            VaultCard(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, VaultGoldBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnLock,
                            contentDescription = null,
                            tint = VaultSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zero Cloud Dependence",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "• VaultX does not declare or request INTERNET permissions.\n" +
                                "• Zero analytics, zero ad tracking, zero telemetry.\n" +
                                "• All encryption, decryption, and key derivation execute 100% on-device.\n" +
                                "• No file content or password ever leaves your physical device.\n" +
                                "• True zero-knowledge architecture: if password is lost, files cannot be decrypted by anyone.",
                        fontSize = 12.sp,
                        color = VaultTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // 5. Data Management
            SectionHeader(title = "Activity History")
            VaultCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearHistoryDialog = true }
                    .testTag("settings_clear_history_card"),
                border = BorderStroke(1.dp, VaultCharcoalBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x20E55656)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = VaultError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear Activity History",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultError
                        )
                        Text(
                            text = "Purge local database records of past encryptions",
                            fontSize = 12.sp,
                            color = VaultTextSecondary
                        )
                    }
                }
            }

            // 6. About VaultX
            SectionHeader(title = "About")
            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VaultX Security Suite",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary
                        )
                        Text(
                            text = "v1.0.0",
                            style = MaterialTheme.typography.labelLarge,
                            color = VaultGold
                        )
                    }
                    Text(
                        text = "Native Android File Encryption & Decryption Engine with verified authenticated streaming cryptography.",
                        fontSize = 12.sp,
                        color = VaultTextTertiary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear All Activity History?",
                    color = VaultTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This removes all records from the local activity database. Existing encrypted files on device will remain safe.",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllActivities()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultError)
                ) {
                    Text("Clear All", color = VaultBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = VaultGold)
                }
            },
            containerColor = VaultCharcoal,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = VaultGold,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun CryptoSpecRow(title: String, spec: String) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            color = VaultTextTertiary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = spec,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = VaultTextPrimary
        )
    }
}
