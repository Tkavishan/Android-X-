package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.FileProvider
import com.example.data.ActivityRecord
import com.example.data.FileUtils
import com.example.data.OperationStatus
import com.example.data.OperationType
import com.example.ui.VaultXViewModel
import com.example.ui.components.VaultCard
import com.example.ui.theme.VaultBlack
import com.example.ui.theme.VaultBlackSurface
import com.example.ui.theme.VaultCharcoal
import com.example.ui.theme.VaultCharcoalBorder
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultGoldBorder
import com.example.ui.theme.VaultGoldLight
import com.example.ui.theme.VaultGoldSubtle
import com.example.ui.theme.VaultSuccess
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.theme.VaultTextTertiary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: VaultXViewModel,
    onNavigateBack: () -> Unit
) {
    val activities by viewModel.allActivities.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ActivityRecord?>(null) }
    val context = LocalContext.current

    val filteredList = when (selectedFilter) {
        "ENCRYPT" -> activities.filter { it.type == OperationType.ENCRYPT }
        "DECRYPT" -> activities.filter { it.type == OperationType.DECRYPT }
        else -> activities
    }

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity Log",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VaultGoldLight
                        )
                    }
                },
                actions = {
                    if (activities.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = VaultGoldLight
                            )
                        }
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
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabChip(
                    text = "All (${activities.size})",
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                FilterTabChip(
                    text = "Encrypted",
                    selected = selectedFilter == "ENCRYPT",
                    onClick = { selectedFilter = "ENCRYPT" }
                )
                FilterTabChip(
                    text = "Decrypted",
                    selected = selectedFilter == "DECRYPT",
                    onClick = { selectedFilter = "DECRYPT" }
                )
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = VaultTextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Activities Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            color = VaultTextSecondary
                        )
                        Text(
                            text = "Encrypted and decrypted operations will appear here.",
                            fontSize = 12.sp,
                            color = VaultTextTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { record ->
                        DetailedActivityCard(
                            record = record,
                            onDelete = { itemToDelete = record },
                            onShare = {
                                if (record.outputFilePath != null) {
                                    val file = File(record.outputFilePath)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        viewModel.shareFile(uri)
                                    } else {
                                        try {
                                            val projection = arrayOf(android.provider.MediaStore.MediaColumns._ID)
                                            val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                                            val args = arrayOf(record.outputFileName)
                                            context.contentResolver.query(
                                                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                                projection,
                                                selection,
                                                args,
                                                null
                                            )?.use { cursor ->
                                                if (cursor.moveToFirst()) {
                                                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID))
                                                    val contentUri = android.content.ContentUris.withAppendedId(
                                                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                                        id
                                                    )
                                                    viewModel.shareFile(contentUri)
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog to clear all history
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Activity History?",
                    color = VaultTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will delete all local history logs. Encrypted files on your device will NOT be deleted.",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllActivities()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultError)
                ) {
                    Text("Clear All", color = VaultBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = VaultGold)
                }
            },
            containerColor = VaultCharcoal,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation dialog to delete single record
    itemToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete Activity Record?",
                    color = VaultTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Remove log entry for \"${record.outputFileName}\"? The actual file remains on disk.",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteActivity(record.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultError)
                ) {
                    Text("Delete", color = VaultBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = VaultGold)
                }
            },
            containerColor = VaultCharcoal,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun FilterTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VaultGold,
            selectedLabelColor = VaultBlack,
            containerColor = VaultCharcoal,
            labelColor = VaultTextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = VaultCharcoalBorder,
            selectedBorderColor = VaultGold
        )
    )
}

@Composable
fun DetailedActivityCard(
    record: ActivityRecord,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val isEncrypt = record.type == OperationType.ENCRYPT
    val isSuccess = record.status == OperationStatus.SUCCESS
    val hasPhysicalFile = record.outputFilePath != null && record.status == OperationStatus.SUCCESS

    VaultCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, VaultCharcoalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSuccess) VaultGoldSubtle else Color(0x20E55656)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEncrypt) Icons.Default.Lock else Icons.Default.Key,
                        contentDescription = null,
                        tint = if (isSuccess) VaultGold else VaultError,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.outputFileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Source: ${record.sourceFileName}",
                        fontSize = 12.sp,
                        color = VaultTextTertiary,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Log",
                        tint = VaultTextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEncrypt) "Encrypted" else "Decrypted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSuccess) VaultSuccess else VaultError
                    )
                    Text(text = " • ", color = VaultTextTertiary, fontSize = 12.sp)
                    Text(
                        text = FileUtils.formatFileSize(record.fileSizeBytes),
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                    Text(text = " • ", color = VaultTextTertiary, fontSize = 12.sp)
                    Text(
                        text = FileUtils.formatDate(record.timestamp),
                        fontSize = 11.sp,
                        color = VaultTextTertiary
                    )
                }

                if (hasPhysicalFile) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = VaultGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (record.outputFilePath != null) {
                Text(
                    text = "Path: ${record.outputFilePath}",
                    fontSize = 11.sp,
                    color = VaultTextTertiary,
                    maxLines = 1
                )
            }
        }
    }
}
