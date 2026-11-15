package com.fisalia.files.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fisalia.files.R
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.NetworkConnection
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.ui.theme.GoogleBluePrimary
import com.fisalia.files.ui.theme.GoogleGreen
import com.fisalia.files.ui.theme.GoogleRed

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_create_folder_title)) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Nama Folder") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName.trim()) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Buat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_rename_title)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nama Baru") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (newName.isNotBlank()) onConfirm(newName.trim()) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    itemCount: Int,
    singleFileName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Hapus") },
        text = {
            val message = if (itemCount == 1 && singleFileName != null) {
                stringResource(R.string.dialog_delete_single, singleFileName)
            } else {
                stringResource(R.string.dialog_delete_confirm, itemCount)
            }
            Text(message)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsBottomSheet(
    fileItem: FileItem,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Detail File",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Nama", value = fileItem.name)
            DetailRow(label = "Tipe", value = if (fileItem.isDirectory) "Direktori / Folder" else fileItem.extension.uppercase().ifEmpty { "File" })
            DetailRow(label = "Lokasi", value = fileItem.path)
            if (!fileItem.isDirectory) {
                DetailRow(label = "Ukuran", value = FileOperationsHelper.formatFileSize(fileItem.size))
            }
            DetailRow(label = "Diubah", value = FileOperationsHelper.formatDate(fileItem.lastModified))
            DetailRow(label = "Protokol", value = fileItem.protocol.name)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerDialog(
    initialConnection: NetworkConnection? = null,
    onDismiss: () -> Unit,
    onTestConnection: (NetworkConnection, onResult: (Boolean, String?) -> Unit) -> Unit,
    onSave: (NetworkConnection) -> Unit
) {
    var selectedProtocol by remember { mutableStateOf(initialConnection?.type ?: ProtocolType.SMB) }
    var name by remember { mutableStateOf(initialConnection?.name ?: "") }
    var host by remember { mutableStateOf(initialConnection?.host ?: "") }
    var port by remember {
        mutableStateOf(
            initialConnection?.port?.toString() ?: when (selectedProtocol) {
                ProtocolType.SMB -> "445"
                ProtocolType.FTP -> "21"
                ProtocolType.FTPS -> "990"
                ProtocolType.SFTP -> "22"
                ProtocolType.WEBDAV -> "80"
                else -> "445"
            }
        )
    }
    var sharePath by remember { mutableStateOf(initialConnection?.shareOrBasePath ?: "") }
    var username by remember { mutableStateOf(initialConnection?.username ?: "") }
    var password by remember { mutableStateOf(initialConnection?.password ?: "") }
    var isAnonymous by remember { mutableStateOf(initialConnection?.isAnonymous ?: false) }
    var domain by remember { mutableStateOf(initialConnection?.domain ?: "") }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String?>?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialConnection == null) "Tambah Server Jaringan" else "Edit Server Jaringan")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Protocol Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = when (selectedProtocol) {
                            ProtocolType.SMB -> "SMB / Windows Share (Samba)"
                            ProtocolType.FTP -> "FTP"
                            ProtocolType.FTPS -> "FTPS (FTP over SSL/TLS)"
                            ProtocolType.SFTP -> "SFTP (SSH File Transfer)"
                            ProtocolType.WEBDAV -> "WebDAV"
                            else -> selectedProtocol.name
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.connection_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        listOf(
                            ProtocolType.SMB to "SMB / Windows Share (Samba)",
                            ProtocolType.FTP to "FTP",
                            ProtocolType.FTPS to "FTPS (FTP over SSL/TLS)",
                            ProtocolType.SFTP to "SFTP (SSH)",
                            ProtocolType.WEBDAV to "WebDAV"
                        ).forEach { (proto, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedProtocol = proto
                                    expandedDropdown = false
                                    port = when (proto) {
                                        ProtocolType.SMB -> "445"
                                        ProtocolType.FTP -> "21"
                                        ProtocolType.FTPS -> "990"
                                        ProtocolType.SFTP -> "22"
                                        ProtocolType.WEBDAV -> "80"
                                        else -> "80"
                                    }
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.connection_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text(stringResource(R.string.host_ip)) },
                        placeholder = { Text("192.168.1.100") },
                        singleLine = true,
                        modifier = Modifier.weight(2.5f)
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(stringResource(R.string.port)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = sharePath,
                    onValueChange = { sharePath = it },
                    label = {
                        Text(stringResource(R.string.share_path))
                    },
                    placeholder = {
                        if (selectedProtocol == ProtocolType.SMB) {
                            Text(stringResource(R.string.smb_share_example))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (selectedProtocol == ProtocolType.SMB) {
                    Text(
                        text = stringResource(R.string.smb_share_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedProtocol == ProtocolType.SMB) {
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text(stringResource(R.string.domain_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAnonymous = !isAnonymous }
                ) {
                    Checkbox(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                    Text(stringResource(R.string.anonymous), style = MaterialTheme.typography.bodyMedium)
                }

                if (!isAnonymous) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Test result indicator
                testResult?.let { (success, errorMsg) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (success) GoogleGreen.copy(alpha = 0.1f) else GoogleRed.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (success) GoogleGreen else GoogleRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (success) {
                                stringResource(R.string.connection_success)
                            } else {
                                stringResource(
                                    R.string.connection_failed,
                                    errorMsg ?: stringResource(R.string.connection_check_host_port)
                                )
                            },
                            fontSize = 12.sp,
                            color = if (success) GoogleGreen else GoogleRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val conn = NetworkConnection(
                            id = initialConnection?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.ifBlank { host },
                            type = selectedProtocol,
                            host = host.trim(),
                            port = port.toIntOrNull() ?: 445,
                            shareOrBasePath = sharePath.trim(),
                            username = username.trim(),
                            password = password,
                            isAnonymous = isAnonymous,
                            domain = domain.trim()
                        )
                        isTesting = true
                        testResult = null
                        onTestConnection(conn) { success, err ->
                            isTesting = false
                            testResult = Pair(success, err)
                        }
                    },
                    enabled = host.isNotBlank() &&
                        (selectedProtocol != ProtocolType.SMB || sharePath.isNotBlank()) &&
                        !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.btn_test))
                }

                Button(
                    onClick = {
                        val conn = NetworkConnection(
                            id = initialConnection?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.ifBlank { host },
                            type = selectedProtocol,
                            host = host.trim(),
                            port = port.toIntOrNull() ?: 445,
                            shareOrBasePath = sharePath.trim(),
                            username = username.trim(),
                            password = password,
                            isAnonymous = isAnonymous,
                            domain = domain.trim(),
                            lastConnected = System.currentTimeMillis()
                        )
                        onSave(conn)
                    },
                    enabled = host.isNotBlank() &&
                        (selectedProtocol != ProtocolType.SMB || sharePath.isNotBlank())
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun HelpFeedbackDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.help_dialog_title))
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.help_about_title,
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.help_about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:${context.getString(R.string.feedback_email)}")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.help_feedback_subject))
                            }
                            context.startActivity(intent)
                        } catch (_: Throwable) {
                            android.widget.Toast.makeText(context, R.string.feedback_email, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.help_feedback_button))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_ok))
            }
        }
    )
}

@Composable
fun PrivacyTermsDialog(
    isPrivacy: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isPrivacy) stringResource(R.string.drawer_privacy) else stringResource(R.string.drawer_terms))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isPrivacy) {
                    Text(
                        text = stringResource(R.string.privacy_policy_content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.terms_of_service_content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.btn_ok))
            }
        }
    )
}

@Composable
fun SafeFolderPinDialog(
    isSettingPin: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pinMinimumLengthError = stringResource(R.string.error_pin_minimum_length)
    val pinMismatchError = stringResource(R.string.error_pin_mismatch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSettingPin) stringResource(R.string.safe_folder_set_pin) else stringResource(R.string.safe_folder_enter_pin),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isSettingPin) {
                        stringResource(R.string.safe_folder_pin_hint)
                    } else {
                        stringResource(R.string.safe_folder_enter_pin_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.safe_folder_pin_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSettingPin) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                confirmPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text(stringResource(R.string.safe_folder_confirm_pin_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        errorMessage = pinMinimumLengthError
                        return@Button
                    }
                    if (isSettingPin && pin != confirmPin) {
                        errorMessage = pinMismatchError
                        return@Button
                    }
                    onConfirm(pin)
                },
                enabled = pin.length >= 4 && (!isSettingPin || confirmPin.length >= 4)
            ) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
