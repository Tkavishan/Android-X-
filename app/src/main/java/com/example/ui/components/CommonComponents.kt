package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.PasswordEvaluation
import com.example.crypto.PasswordStrengthLevel
import com.example.data.FileUtils
import com.example.data.SelectedFileInfo
import com.example.ui.theme.VaultBlack
import com.example.ui.theme.VaultBlackSurface
import com.example.ui.theme.VaultCharcoal
import com.example.ui.theme.VaultCharcoalBorder
import com.example.ui.theme.VaultCharcoalElevated
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultErrorBg
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultGoldBorder
import com.example.ui.theme.VaultGoldLight
import com.example.ui.theme.VaultGoldSubtle
import com.example.ui.theme.VaultSuccess
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.theme.VaultTextTertiary

@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = BorderStroke(1.dp, VaultCharcoalBorder),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val cardModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = VaultCharcoal),
        border = border
    ) {
        content()
    }
}

@Composable
fun VaultGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "vault_gold_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VaultGold,
            contentColor = VaultBlack,
            disabledContainerColor = VaultCharcoalBorder,
            disabledContentColor = VaultTextTertiary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) VaultBlack else VaultTextTertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun VaultOutlinedGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "vault_outlined_button"
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) VaultGoldBorder else VaultCharcoalBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (enabled) VaultGold else VaultTextTertiary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) VaultGold else VaultTextTertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun VaultPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "••••••••••••",
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = "password_input"
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = VaultTextSecondary) },
            placeholder = { Text(placeholder, color = VaultTextTertiary) },
            singleLine = true,
            isError = isError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description, tint = VaultGoldLight)
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VaultCharcoalElevated,
                unfocusedContainerColor = VaultBlackSurface,
                focusedBorderColor = VaultGold,
                unfocusedBorderColor = VaultCharcoalBorder,
                focusedTextColor = VaultTextPrimary,
                unfocusedTextColor = VaultTextPrimary,
                cursorColor = VaultGold,
                errorBorderColor = VaultError
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = VaultError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun PasswordStrengthBar(
    evaluation: PasswordEvaluation,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
            Text(
                text = evaluation.level.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = evaluation.level.color
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4 Segmented indicator bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (step in 1..4) {
                val isActive = evaluation.score >= step
                val segmentColor = if (isActive) evaluation.level.color else VaultCharcoalBorder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segmentColor)
                )
            }
        }

        if (evaluation.hints.isNotEmpty() && evaluation.level != PasswordStrengthLevel.EXCELLENT) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tip: ${evaluation.hints.first()}",
                fontSize = 11.sp,
                color = VaultTextTertiary
            )
        }
    }
}

@Composable
fun FilePreviewCard(
    fileInfo: SelectedFileInfo,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    VaultCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, VaultGoldBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultGoldSubtle)
                    .border(1.dp, VaultGoldBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (fileInfo.isVaultXFile) Icons.Default.Lock else Icons.Default.Description,
                    contentDescription = null,
                    tint = VaultGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fileInfo.formattedSize,
                        fontSize = 13.sp,
                        color = VaultGoldLight,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ",
                        fontSize = 13.sp,
                        color = VaultTextTertiary
                    )
                    Text(
                        text = fileInfo.mimeType.substringAfterLast('/'),
                        fontSize = 13.sp,
                        color = VaultTextSecondary,
                        maxLines = 1
                    )
                }
            }

            if (onRemove != null) {
                Text(
                    text = "Change",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VaultGold,
                    modifier = Modifier
                        .clickable(onClick = onRemove)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SecurityStatusPill(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = VaultCharcoalElevated,
        border = BorderStroke(1.dp, VaultGoldBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(VaultSuccess)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "100% Local • Zero Network Access • AES-256 Ready",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = VaultGoldLight
            )
        }
    }
}

@Composable
fun OperationProgressCard(
    isEncrypting: Boolean,
    progress: Float,
    bytesProcessed: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val percent = (progress * 100).toInt().coerceIn(0, 100)

    VaultCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, VaultGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEncrypting) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = null,
                        tint = VaultGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEncrypting) "Encrypting Stream..." else "Decrypting Stream...",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultTextPrimary
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VaultGold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = VaultGold,
                trackColor = VaultBlackSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Streaming chunks (64 KB)",
                    fontSize = 12.sp,
                    color = VaultTextTertiary
                )
                Text(
                    text = "${FileUtils.formatFileSize(bytesProcessed)} of ${FileUtils.formatFileSize(totalBytes)}",
                    fontSize = 12.sp,
                    color = VaultTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VaultErrorBg),
        border = BorderStroke(1.dp, VaultError)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = VaultError,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = VaultTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
