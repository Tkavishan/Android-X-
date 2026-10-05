package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.VaultXViewModel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.FilePreviewCard
import com.example.ui.components.OperationProgressCard
import com.example.ui.components.VaultCard
import com.example.ui.components.VaultGoldButton
import com.example.ui.components.VaultOutlinedGoldButton
import com.example.ui.components.VaultPasswordField
import com.example.ui.theme.VaultBlack
import com.example.ui.theme.VaultCharcoal
import com.example.ui.theme.VaultCharcoalBorder
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultGoldBorder
import com.example.ui.theme.VaultGoldLight
import com.example.ui.theme.VaultGoldSubtle
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.theme.VaultTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecryptScreen(
    viewModel: VaultXViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.decryptState.collectAsState()
    val scrollState = rememberScrollState()

    // SAF Picker to select .vaultx file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onDecryptFileSelected(it) }
    }

    // SAF Saver to pick custom destination for restored file
    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let { viewModel.startDecryption(customOutputUri = it) }
    }

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Decrypt File",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !state.isDecrypting,
                        modifier = Modifier.testTag("decrypt_back_button")
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (state.isCompleted) {
                // SUCCESS RESTORE VIEW
                DecryptSuccessView(
                    fileName = state.resultFileName ?: "restored_file",
                    filePath = state.resultFilePath ?: "Downloads",
                    onOpenFile = {
                        state.resultFileUri?.let { viewModel.openFile(it) }
                    },
                    onShare = {
                        state.resultFileUri?.let { viewModel.shareFile(it) }
                    },
                    onDecryptAnother = {
                        viewModel.resetDecryptState()
                    },
                    onReturnHome = onNavigateBack
                )
            } else {
                // 1. SELECT .VAULTX FILE
                if (state.selectedFile == null) {
                    SelectVaultXFilePlaceholderCard(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                    )
                } else {
                    FilePreviewCard(
                        fileInfo = state.selectedFile!!,
                        onRemove = if (!state.isDecrypting) {
                            { filePickerLauncher.launch(arrayOf("*/*")) }
                        } else null
                    )
                }

                // 2. PASSWORD INPUT
                if (state.selectedFile != null && !state.isDecrypting) {
                    VaultCard(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, VaultCharcoalBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Enter Decryption Password",
                                style = MaterialTheme.typography.titleMedium,
                                color = VaultTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "The password used during encryption is required to unlock and verify this file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultTextSecondary,
                                lineHeight = 16.sp
                            )

                            VaultPasswordField(
                                value = state.password,
                                onValueChange = { viewModel.onDecryptPasswordChanged(it) },
                                label = "Master Password",
                                placeholder = "Enter password",
                                testTag = "decrypt_password_input"
                            )
                        }
                    }

                    // 3. ACTION BUTTONS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val canDecrypt = state.password.isNotEmpty()

                        VaultGoldButton(
                            text = "Decrypt & Save to Downloads",
                            icon = Icons.Default.LockOpen,
                            enabled = canDecrypt,
                            onClick = { viewModel.startDecryption(customOutputUri = null) },
                            testTag = "decrypt_submit_button"
                        )
                    }
                }

                // 4. PROGRESS CARD
                if (state.isDecrypting) {
                    OperationProgressCard(
                        isEncrypting = false,
                        progress = state.progress,
                        bytesProcessed = state.bytesProcessed,
                        totalBytes = state.selectedFile?.sizeBytes ?: 1L
                    )
                }

                // 5. ERROR BANNER
                if (state.errorMessage != null) {
                    ErrorBanner(
                        message = state.errorMessage!!,
                        modifier = Modifier.testTag("decrypt_error_banner")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SelectVaultXFilePlaceholderCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("decrypt_file_picker_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCharcoal),
        border = BorderStroke(1.2.dp, VaultGoldBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(VaultGoldSubtle)
                    .border(1.dp, VaultGoldBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = VaultGold,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select .vaultx File",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap here to choose an encrypted file created with VaultX",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextSecondary,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun DecryptSuccessView(
    fileName: String,
    filePath: String,
    onOpenFile: () -> Unit,
    onShare: () -> Unit,
    onDecryptAnother: () -> Unit,
    onReturnHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(VaultGoldSubtle)
                .border(2.dp, VaultGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = VaultGold,
                modifier = Modifier.size(44.dp)
            )
        }

        Text(
            text = "Decrypted Successfully",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = VaultTextPrimary
        )

        VaultCard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, VaultGoldBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Restored File", fontSize = 12.sp, color = VaultTextTertiary)
                    Text(text = "Saved to Downloads", fontSize = 12.sp, color = VaultGold, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = VaultTextPrimary
                )

                Text(
                    text = "File Location:\n$filePath",
                    fontSize = 12.sp,
                    color = VaultTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        VaultGoldButton(
            text = "Open Restored File",
            icon = Icons.Default.FolderOpen,
            onClick = onOpenFile,
            testTag = "decrypt_open_button"
        )

        VaultOutlinedGoldButton(
            text = "Share File",
            icon = Icons.Default.Share,
            onClick = onShare,
            testTag = "decrypt_share_button"
        )

        VaultOutlinedGoldButton(
            text = "Decrypt Another File",
            icon = Icons.Default.Key,
            onClick = onDecryptAnother,
            testTag = "decrypt_another_button"
        )

        Text(
            text = "Return to Dashboard",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = VaultGoldLight,
            modifier = Modifier
                .clickable(onClick = onReturnHome)
                .padding(8.dp)
                .testTag("decrypt_return_home_button")
        )
    }
}
