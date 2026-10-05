package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityRecord
import com.example.data.FileUtils
import com.example.data.OperationStatus
import com.example.data.OperationType
import com.example.ui.Screen
import com.example.ui.VaultXViewModel
import com.example.ui.components.SecurityStatusPill
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
fun MainDashboardScreen(
    viewModel: VaultXViewModel,
    onNavigate: (Screen) -> Unit
) {
    val recentActivities by viewModel.recentActivities.collectAsState()

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(
                        onClick = { onNavigate(Screen.HISTORY) },
                        modifier = Modifier.testTag("dashboard_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Activity History",
                            tint = VaultGoldLight
                        )
                    }
                    IconButton(
                        onClick = { onNavigate(Screen.SETTINGS) },
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = VaultGoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultBlack)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. VaultX Logo & Branding Hero
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(VaultCharcoalElevated)
                            .border(1.5.dp, VaultGold, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "VaultX Emblem",
                            tint = VaultGold,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "VaultX",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Private. Local. Secure.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VaultGoldLight,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SecurityStatusPill()
                }
            }

            // 2. Large Action Cards: Encrypt File & Decrypt File
            item {
                ActionCard(
                    title = "Encrypt File",
                    subtitle = "Secure any local file with AES-256-GCM authenticated cipher",
                    icon = Icons.Default.Lock,
                    badge = ".vaultx",
                    testTag = "dashboard_encrypt_card",
                    isPrimary = true,
                    onClick = {
                        viewModel.resetEncryptState()
                        onNavigate(Screen.ENCRYPT)
                    }
                )
            }

            item {
                ActionCard(
                    title = "Decrypt File",
                    subtitle = "Unlock and restore encrypted .vaultx containers locally",
                    icon = Icons.Default.Key,
                    badge = "Restore",
                    testTag = "dashboard_decrypt_card",
                    isPrimary = false,
                    onClick = {
                        viewModel.resetDecryptState()
                        onNavigate(Screen.DECRYPT)
                    }
                )
            }

            // 3. Hardware & Crypto Specs Pill
            item {
                CryptoSpecsCard()
            }

            // 4. Recent Activity Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultTextPrimary
                    )
                    if (recentActivities.isNotEmpty()) {
                        Text(
                            text = "View All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VaultGold,
                            modifier = Modifier
                                .clickable { onNavigate(Screen.HISTORY) }
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (recentActivities.isEmpty()) {
                item {
                    EmptyActivityCard()
                }
            } else {
                items(recentActivities, key = { it.id }) { record ->
                    ActivityItemCard(record = record)
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String,
    testTag: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isPrimary) VaultGold else VaultGoldBorder
    val background = if (isPrimary) VaultCharcoalElevated else VaultCharcoal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isPrimary) VaultGoldSubtle else VaultBlackSurface)
                    .border(1.dp, VaultGoldBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VaultGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VaultGoldSubtle)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultGold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = VaultGold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CryptoSpecsCard() {
    VaultCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, VaultCharcoalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = VaultGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cryptographic Architecture",
                    style = MaterialTheme.typography.labelLarge,
                    color = VaultGold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecItem(label = "Cipher", value = "AES-256-GCM")
                SpecItem(label = "KDF", value = "PBKDF2-SHA256")
                SpecItem(label = "Rounds", value = "15,000")
                SpecItem(label = "Chunks", value = "256 KB AAD")
            }
        }
    }
}

@Composable
fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = VaultTextTertiary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VaultTextPrimary
        )
    }
}

@Composable
fun EmptyActivityCard() {
    VaultCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, VaultCharcoalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VaultBlackSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = VaultTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Recent Encryptions",
                style = MaterialTheme.typography.titleMedium,
                color = VaultTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Files encrypted or decrypted will appear in your private local log.",
                fontSize = 12.sp,
                color = VaultTextTertiary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ActivityItemCard(record: ActivityRecord) {
    val isEncrypt = record.type == OperationType.ENCRYPT
    val isSuccess = record.status == OperationStatus.SUCCESS

    VaultCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, VaultCharcoalBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSuccess) VaultGoldSubtle else Color(0x20E55656)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEncrypt) Icons.Default.Lock else Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isSuccess) VaultGold else VaultError,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEncrypt) record.outputFileName else record.sourceFileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp,
                    color = VaultTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEncrypt) "Encrypted" else "Decrypted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSuccess) VaultSuccess else VaultError
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = VaultTextTertiary
                    )
                    Text(
                        text = FileUtils.formatFileSize(record.fileSizeBytes),
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = VaultTextTertiary
                    )
                    Text(
                        text = FileUtils.formatDate(record.timestamp),
                        fontSize = 11.sp,
                        color = VaultTextTertiary
                    )
                }
            }
        }
    }
}
