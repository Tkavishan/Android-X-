package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoConstants
import com.example.crypto.PasswordStrengthLevel
import com.example.ui.Screen
import com.example.ui.VaultXViewModel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.FilePreviewCard
import com.example.ui.components.OperationProgressCard
import com.example.ui.components.PasswordStrengthBar
import com.example.ui.components.VaultCard
import com.example.ui.components.VaultGoldButton
import com.example.ui.components.VaultOutlinedGoldButton
import com.example.ui.components.VaultPasswordField
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
import com.example.ui.theme.VaultWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptScreen(
    viewModel: VaultXViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.encryptState.collectAsState()
    val scrollState = rememberScrollState()

    // SAF File Open Launcher (select source file)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEncryptFileSelected(it) }
    }

    // SAF File Create Launcher (optional custom destination)
    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.startEncryption(customOutputUri = it) }
    }

    Scaffold(
        containerColor = VaultBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Encrypt File",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !state.isEncrypting,
                        modifier = Modifier.testTag("encrypt_back_button")
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
                // SUCCESS VIEW
                EncryptSuccessView(
                    fileName = state.resultFileName ?: "encrypted.vaultx",
                    filePath = state.resultFilePath ?: "Downloads",
                    onOpenFile = {
                        state.resultFileUri?.let { viewModel.openFile(it) }
                    },
                    onShare = {
                        state.resultFileUri?.let { viewModel.shareFile(it) }
                    },
                    onEncryptAnother = {
                        viewModel.resetEncryptState()
                    },
                    onReturnHome = onNavigateBack
                )
            } else {
                // 1. FILE PICKER SECTION
                if (state.selectedFile == null) {
                    SelectFilePlaceholderCard(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                    )
                } else {
                    FilePreviewCard(
                        fileInfo = state.selectedFile!!,
                        onRemove = if (!state.isEncrypting) {
                            { filePickerLauncher.launch(arrayOf("*/*")) }
                        } else null
                    )
                }

                // 2. PASSWORD INPUTS & STRENGTH
                if (state.selectedFile != null && !state.isEncrypting) {
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
                                text = "Create Master Vault Password",
                                style = MaterialTheme.typography.titleMedium,
                                color = VaultTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )

                            VaultPasswordField(
                                value = state.password,
                                onValueChange = { viewModel.onEncryptPasswordChanged(it) },
                                label = "Encryption Password",
                                placeholder = "Choose a strong password",
                                testTag = "encrypt_password_input"
                            )

                            PasswordStrengthBar(evaluation = state.evaluation)

                            VaultPasswordField(
                                value = state.confirmPassword,
                                onValueChange = { viewModel.onEncryptConfirmPasswordChanged(it) },
                                label = "Confirm Password",
                                placeholder = "Re-enter password",
                                isError = state.confirmPassword.isNotEmpty() && state.password != state.confirmPassword,
                                errorMessage = "Passwords do not match",
                                testTag = "encrypt_confirm_password_input"
                            )

                            // Security & Zero Knowledge Notice
                            SecurityWarningCard()
                        }
                    }

                    // 3. ACTION BUTTONS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val canEncrypt = state.password.isNotEmpty() &&
                                state.password == state.confirmPassword &&
                                state.evaluation.level != PasswordStrengthLevel.WEAK

                        VaultGoldButton(
                            text = "Encrypt & Save to Downloads",
                            icon = Icons.Default.Lock,
                            enabled = canEncrypt,
                            onClick = { viewModel.startEncryption(customOutputUri = null) },
                            testTag = "encrypt_submit_button"
                        )
                    }
                }

                // 4. PROGRESS CARD
                if (state.isEncrypting) {
                    OperationProgressCard(
                        isEncrypting = true,
                        progress = state.progress,
                        bytesProcessed = state.bytesProcessed,
                        totalBytes = state.selectedFile?.sizeBytes ?: 1L
                    )
                }

                // 5. ERROR BANNER
                if (state.errorMessage != null) {
                    ErrorBanner(message = state.errorMessage!!)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SelectFilePlaceholderCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("encrypt_file_picker_card"),
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
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = VaultGold,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select File to Encrypt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap here to pick any document, photo, video, or archive from your local device storage",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextSecondary,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SecurityWarningCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VaultBlackSurface)
            .border(1.dp, VaultCharcoalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = VaultGoldLight,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Zero-Knowledge Principle: VaultX does not save passwords and has no recovery backdoor. If you forget your password, this file cannot be recovered.",
                fontSize = 11.sp,
                color = VaultTextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun EncryptSuccessView(
    fileName: String,
    filePath: String,
    onOpenFile: () -> Unit,
    onShare: () -> Unit,
    onEncryptAnother: () -> Unit,
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
            text = "File Encrypted Successfully",
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
                    Text(text = "Saved to Downloads", fontSize = 12.sp, color = VaultGold, fontWeight = FontWeight.Bold)
                    Text(text = "AES-256-GCM", fontSize = 12.sp, color = VaultTextTertiary)
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
            text = "Share Encrypted File",
            icon = Icons.Default.Share,
            onClick = onShare,
            testTag = "encrypt_share_button"
        )

        VaultOutlinedGoldButton(
            text = "Open in Files",
            icon = Icons.Default.FolderOpen,
            onClick = onOpenFile,
            testTag = "encrypt_open_button"
        )

        VaultOutlinedGoldButton(
            text = "Encrypt Another File",
            icon = Icons.Default.Lock,
            onClick = onEncryptAnother,
            testTag = "encrypt_another_button"
        )

        Text(
            text = "Return to Dashboard",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = VaultGoldLight,
            modifier = Modifier
                .clickable(onClick = onReturnHome)
                .padding(8.dp)
                .testTag("encrypt_return_home_button")
        )
    }
}
