package com.univesp.lumme.presentation.ui.devices

import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.univesp.lumme.domain.model.Device
import com.univesp.lumme.domain.model.DeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onBack: () -> Unit,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snack = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<Device?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snack.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    if (showAddDialog) {
        AddDeviceDialog(
            isLoading = state.adding,
            onDismiss = { if (!state.adding) showAddDialog = false },
            onConfirm = { label, type, room ->
                viewModel.addDevice(label, type, room)
                showAddDialog = false
            }
        )
    }

    deviceToDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Remover dispositivo") },
            text = { Text("Deseja remover \"${device.label}\"? Apenas dispositivos demo podem ser removidos.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDevice(device.id)
                    deviceToDelete = null
                }) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dispositivos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar dispositivo")
            }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        if (state.loading && state.devicesByRoom.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.devicesByRoom.forEach { (room, devices) ->
                item {
                    Text(
                        text = room,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(devices, key = { it.id }) { device ->
                    DeviceRow(
                        device = device,
                        isToggling = device.id in state.togglingIds,
                        onToggle = { viewModel.toggle(device) },
                        onLongPress = {
                            if (device.id.startsWith("demo-")) deviceToDelete = device
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceRow(
    device: Device,
    isToggling: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = iconForType(device.type),
                    contentDescription = null,
                    tint = if (device.status.isOn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.size(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(device.label, fontWeight = FontWeight.Medium)
                        if (device.id.startsWith("demo-")) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "DEMO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    val statusText = when {
                        !device.status.online -> "Offline"
                        device.status.powerWatts != null && device.status.isOn ->
                            "${"%.0f".format(device.status.powerWatts)} W"
                        device.status.isOn -> "Ligado"
                        else -> "Desligado"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isToggling) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Switch(
                    checked = device.status.isOn,
                    onCheckedChange = { onToggle() },
                    enabled = device.status.online
                )
            }
        }
    }
}

private fun iconForType(type: DeviceType): ImageVector = when (type) {
    DeviceType.LIGHT_BULB -> Icons.Default.Lightbulb
    DeviceType.THERMOSTAT -> Icons.Default.Thermostat
    DeviceType.SENSOR -> Icons.Default.Sensors
    else -> Icons.Default.Power
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (label: String, type: DeviceType, room: String?) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DeviceType.LIGHT_BULB) }
    var selectedRoom by remember { mutableStateOf("Sala") }
    var typeExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

    val types = listOf(
        DeviceType.LIGHT_BULB to "Lâmpada",
        DeviceType.OUTLET to "Tomada",
        DeviceType.SWITCH to "Interruptor",
        DeviceType.THERMOSTAT to "Termostato",
        DeviceType.SENSOR to "Sensor"
    )
    val rooms = listOf("Sala", "Quarto", "Cozinha", "Banheiro", "Área Externa", "Escritório")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar dispositivo") },
        text = {
            Column {
                Text(
                    "Cria um dispositivo demo no servidor (não conecta a hardware real).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex: Ventilador da sala") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = types.first { it.first == selectedType }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        types.forEach { (type, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = roomExpanded,
                    onExpandedChange = { roomExpanded = !roomExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRoom,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cômodo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roomExpanded,
                        onDismissRequest = { roomExpanded = false }
                    ) {
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text(room) },
                                onClick = {
                                    selectedRoom = room
                                    roomExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label, selectedType, selectedRoom) },
                enabled = !isLoading && label.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Adicionar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}

// Dica de uso: long-press num dispositivo "demo-..." abre o diálogo de remoção
