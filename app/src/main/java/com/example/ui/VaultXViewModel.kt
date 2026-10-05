package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoConstants
import com.example.crypto.CryptoEngine
import com.example.crypto.PasswordEvaluation
import com.example.crypto.PasswordStrengthEvaluator
import com.example.crypto.VaultCryptoException
import com.example.crypto.VaultMetadata
import com.example.data.ActivityRecord
import com.example.data.FileUtils
import com.example.data.OperationStatus
import com.example.data.OperationType
import com.example.data.SelectedFileInfo
import com.example.data.VaultDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

enum class Screen {
    DASHBOARD,
    ENCRYPT,
    DECRYPT,
    HISTORY,
    SETTINGS
}

data class EncryptUiState(
    val selectedFile: SelectedFileInfo? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val evaluation: PasswordEvaluation = PasswordStrengthEvaluator.evaluate(""),
    val isEncrypting: Boolean = false,
    val progress: Float = 0f,
    val bytesProcessed: Long = 0L,
    val isCompleted: Boolean = false,
    val resultFileName: String? = null,
    val resultFilePath: String? = null,
    val resultFileUri: Uri? = null,
    val errorMessage: String? = null
)

data class DecryptUiState(
    val selectedFile: SelectedFileInfo? = null,
    val password: String = "",
    val isDecrypting: Boolean = false,
    val progress: Float = 0f,
    val bytesProcessed: Long = 0L,
    val isCompleted: Boolean = false,
    val resultFileName: String? = null,
    val resultFilePath: String? = null,
    val resultFileUri: Uri? = null,
    val errorMessage: String? = null
)

class VaultXViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VaultDatabase.getInstance(application)
    private val activityDao = database.activityDao()

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<Screen>(Screen.DASHBOARD)

    private val _encryptState = MutableStateFlow(EncryptUiState())
    val encryptState: StateFlow<EncryptUiState> = _encryptState.asStateFlow()

    private val _decryptState = MutableStateFlow(DecryptUiState())
    val decryptState: StateFlow<DecryptUiState> = _decryptState.asStateFlow()

    val recentActivities: StateFlow<List<ActivityRecord>> = activityDao.getRecentActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ActivityRecord>> = activityDao.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeJob: Job? = null

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_screenHistory.size > 1) {
            _screenHistory.removeAt(_screenHistory.lastIndex)
            _currentScreen.value = _screenHistory.last()
            return true
        }
        return false
    }

    // --- ENCRYPT ACTIONS ---

    fun onEncryptFileSelected(uri: Uri) {
        val fileInfo = FileUtils.queryFileInfo(getApplication(), uri)
        _encryptState.update {
            it.copy(
                selectedFile = fileInfo,
                isCompleted = false,
                resultFileName = null,
                resultFilePath = null,
                resultFileUri = null,
                errorMessage = null
            )
        }
    }

    fun onEncryptPasswordChanged(password: String) {
        _encryptState.update {
            it.copy(
                password = password,
                evaluation = PasswordStrengthEvaluator.evaluate(password)
            )
        }
    }

    fun onEncryptConfirmPasswordChanged(confirmPassword: String) {
        _encryptState.update { it.copy(confirmPassword = confirmPassword) }
    }

    fun resetEncryptState() {
        activeJob?.cancel()
        _encryptState.value = EncryptUiState()
    }

    fun startEncryption(customOutputUri: Uri? = null) {
        val state = _encryptState.value
        val fileInfo = state.selectedFile ?: return

        if (state.password.isEmpty()) {
            _encryptState.update { it.copy(errorMessage = "Please enter an encryption password") }
            return
        }

        if (state.password != state.confirmPassword) {
            _encryptState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        _encryptState.update {
            it.copy(
                isEncrypting = true,
                progress = 0f,
                bytesProcessed = 0L,
                errorMessage = null,
                isCompleted = false
            )
        }

        activeJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val cr: ContentResolver = context.contentResolver
            val passwordChars = state.password.toCharArray()

            var outStream: OutputStream? = null
            var inStream: InputStream? = null
            var targetFile: File? = null
            val outName = "${fileInfo.name}${CryptoConstants.FILE_EXTENSION}"
            var successUri: Uri? = null
            var successPath: String? = null

            try {
                inStream = cr.openInputStream(fileInfo.uri)
                    ?: throw IllegalStateException("Cannot open selected file for reading")

                val downloadTarget = FileUtils.createDownloadTarget(
                    context = context,
                    fileName = outName,
                    mimeType = CryptoConstants.MIME_TYPE
                )
                outStream = downloadTarget.outputStream
                successUri = downloadTarget.uri
                successPath = downloadTarget.displayPath

                val metadata = VaultMetadata(
                    originalFileName = fileInfo.name,
                    originalFileSize = fileInfo.sizeBytes,
                    originalMimeType = fileInfo.mimeType,
                    createdAtTimestamp = System.currentTimeMillis()
                )

                CryptoEngine.encryptStream(
                    inputStream = inStream,
                    outputStream = outStream,
                    password = passwordChars,
                    metadata = metadata,
                    onProgress = { progress, processed ->
                        _encryptState.update {
                            it.copy(progress = progress, bytesProcessed = processed)
                        }
                    }
                )

                // Close stream immediately so MediaProvider completes file writing to Downloads
                try {
                    outStream.flush()
                    outStream.close()
                } catch (_: Exception) {}
                outStream = null

                val finalPath = FileUtils.finalizeDownloadTarget(context, downloadTarget.uri)
                successPath = finalPath

                passwordChars.fill('\u0000')

                _encryptState.update {
                    it.copy(
                        isEncrypting = false,
                        isCompleted = true,
                        resultFileName = outName,
                        resultFilePath = finalPath,
                        resultFileUri = successUri,
                        errorMessage = null
                    )
                }

                // Log Activity
                activityDao.insert(
                    ActivityRecord(
                        type = OperationType.ENCRYPT,
                        sourceFileName = fileInfo.name,
                        outputFileName = outName,
                        outputFilePath = successPath,
                        fileSizeBytes = fileInfo.sizeBytes,
                        status = OperationStatus.SUCCESS,
                        message = "Encrypted with AES-256-GCM"
                    )
                )

                vibrateSuccess()

            } catch (e: Exception) {
                passwordChars.fill('\u0000')
                val errorMsg = e.message ?: "Encryption failed"
                _encryptState.update {
                    it.copy(
                        isEncrypting = false,
                        isCompleted = false,
                        errorMessage = errorMsg
                    )
                }
                activityDao.insert(
                    ActivityRecord(
                        type = OperationType.ENCRYPT,
                        sourceFileName = fileInfo.name,
                        outputFileName = outName,
                        outputFilePath = null,
                        fileSizeBytes = fileInfo.sizeBytes,
                        status = OperationStatus.FAILED,
                        message = errorMsg
                    )
                )
                vibrateError()
            } finally {
                try { inStream?.close() } catch (_: Exception) {}
                try { outStream?.close() } catch (_: Exception) {}
            }
        }
    }

    // --- DECRYPT ACTIONS ---

    fun onDecryptFileSelected(uri: Uri) {
        val fileInfo = FileUtils.queryFileInfo(getApplication(), uri)
        _decryptState.update {
            it.copy(
                selectedFile = fileInfo,
                isCompleted = false,
                resultFileName = null,
                resultFilePath = null,
                resultFileUri = null,
                errorMessage = null
            )
        }
    }

    fun onDecryptPasswordChanged(password: String) {
        _decryptState.update { it.copy(password = password, errorMessage = null) }
    }

    fun resetDecryptState() {
        activeJob?.cancel()
        _decryptState.value = DecryptUiState()
    }

    fun startDecryption(customOutputUri: Uri? = null) {
        val state = _decryptState.value
        val fileInfo = state.selectedFile ?: return

        if (state.password.isEmpty()) {
            _decryptState.update { it.copy(errorMessage = "Please enter the vault password") }
            return
        }

        _decryptState.update {
            it.copy(
                isDecrypting = true,
                progress = 0f,
                bytesProcessed = 0L,
                errorMessage = null,
                isCompleted = false
            )
        }

        activeJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val cr: ContentResolver = context.contentResolver
            val passwordChars = state.password.toCharArray()

            var inStream: InputStream? = null
            var outStream: OutputStream? = null
            var successUri: Uri? = null
            var successPath: String? = null
            var restoredFileName = "decrypted_file"

            try {
                inStream = cr.openInputStream(fileInfo.uri)
                    ?: throw IllegalStateException("Cannot open .vaultx file for reading")

                // 1. Instant password validation & header inspection before writing to disk
                val headerMeta = cr.openInputStream(fileInfo.uri)?.use { stream ->
                    CryptoEngine.inspectVaultHeader(stream, passwordChars)
                } ?: throw IllegalStateException("Cannot inspect vault container")

                restoredFileName = headerMeta.originalFileName

                // 2. Stream directly into public Downloads folder
                val downloadTarget = FileUtils.createDownloadTarget(
                    context = context,
                    fileName = restoredFileName,
                    mimeType = headerMeta.originalMimeType
                )
                outStream = downloadTarget.outputStream
                successUri = downloadTarget.uri
                successPath = downloadTarget.displayPath

                // 3. Stream decrypt full payload directly to Downloads
                inStream = cr.openInputStream(fileInfo.uri)
                    ?: throw IllegalStateException("Cannot open .vaultx file for reading")

                val metadata = CryptoEngine.decryptStream(
                    inputStream = inStream,
                    outputStream = outStream,
                    password = passwordChars,
                    onProgress = { progress, processed ->
                        _decryptState.update {
                            it.copy(progress = progress, bytesProcessed = processed)
                        }
                    }
                )

                // Close stream immediately so MediaProvider completes file writing to Downloads
                try {
                    outStream.flush()
                    outStream.close()
                } catch (_: Exception) {}
                outStream = null

                val finalPath = FileUtils.finalizeDownloadTarget(context, downloadTarget.uri)
                successPath = finalPath

                passwordChars.fill('\u0000')

                _decryptState.update {
                    it.copy(
                        isDecrypting = false,
                        isCompleted = true,
                        resultFileName = restoredFileName,
                        resultFilePath = finalPath,
                        resultFileUri = successUri,
                        errorMessage = null
                    )
                }

                activityDao.insert(
                    ActivityRecord(
                        type = OperationType.DECRYPT,
                        sourceFileName = fileInfo.name,
                        outputFileName = restoredFileName,
                        outputFilePath = successPath,
                        fileSizeBytes = metadata.originalFileSize,
                        status = OperationStatus.SUCCESS,
                        message = "Decrypted successfully"
                    )
                )

                vibrateSuccess()

            } catch (e: VaultCryptoException) {
                passwordChars.fill('\u0000')
                val errorMsg = e.message ?: CryptoConstants.ERROR_DECRYPTION_FAILED
                _decryptState.update {
                    it.copy(
                        isDecrypting = false,
                        isCompleted = false,
                        errorMessage = errorMsg
                    )
                }
                activityDao.insert(
                    ActivityRecord(
                        type = OperationType.DECRYPT,
                        sourceFileName = fileInfo.name,
                        outputFileName = restoredFileName,
                        outputFilePath = null,
                        fileSizeBytes = fileInfo.sizeBytes,
                        status = OperationStatus.FAILED,
                        message = errorMsg
                    )
                )
                vibrateError()
            } catch (e: Exception) {
                passwordChars.fill('\u0000')
                val errorMsg = e.message ?: "Decryption error occurred"
                _decryptState.update {
                    it.copy(
                        isDecrypting = false,
                        isCompleted = false,
                        errorMessage = errorMsg
                    )
                }
                activityDao.insert(
                    ActivityRecord(
                        type = OperationType.DECRYPT,
                        sourceFileName = fileInfo.name,
                        outputFileName = restoredFileName,
                        outputFilePath = null,
                        fileSizeBytes = fileInfo.sizeBytes,
                        status = OperationStatus.FAILED,
                        message = errorMsg
                    )
                )
                vibrateError()
            } finally {
                try { inStream?.close() } catch (_: Exception) {}
                try { outStream?.close() } catch (_: Exception) {}
            }
        }
    }

    // --- ACTIVITY / HISTORY ---

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            activityDao.deleteById(id)
        }
    }

    fun clearAllActivities() {
        viewModelScope.launch {
            activityDao.clearAll()
        }
    }

    // --- SHARING ---

    fun openFile(uri: Uri, mimeType: String = "*/*") {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            shareFile(uri, mimeType)
        }
    }

    fun shareFile(uri: Uri, mimeType: String = "*/*") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(getApplication(), "Unable to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrateSuccess() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateError() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        } catch (_: Exception) {}
    }
}
