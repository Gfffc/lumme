package com.univesp.lumme.presentation.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.univesp.lumme.presentation.ui.components.GoalProgressCard
import com.univesp.lumme.presentation.ui.components.MetricCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onDevicesClick: () -> Unit,
    onAddDeviceClick: () -> Unit = onDevicesClick,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snack.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minha Casa") },
                actions = {
                    IconButton(onClick = onAddDeviceClick) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar dispositivo")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Meta
            state.goal?.let { goal ->
                GoalProgressCard(
                    targetKwh = goal.targetKwh,
                    currentKwh = goal.progressKwh,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Métricas em grid 2x2
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Consumo 7d",
                    value = "${"%.1f".format(state.totalKwhWeek)} kWh",
                    subtitle = "média diária ${"%.1f".format(state.totalKwhWeek / 7)}",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Custo est.",
                    value = "R$ ${"%.2f".format(state.estimatedCostWeek)}",
                    subtitle = "tarifa ANEEL média",
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Dispositivos",
                    value = "${state.devicesOnline}/${state.devices.size}",
                    subtitle = "online",
                    icon = Icons.Default.DevicesOther,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Agora ligados",
                    value = "${state.devicesOn}",
                    subtitle = "aparelhos em uso",
                    icon = Icons.Default.PowerSettingsNew,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Aqui entraria o gráfico Vico com state.weeklyConsumption
            // Omitido por brevidade — substituímos por texto
            Text(
                "Consumo por dia (últimos 7d)",
                style = MaterialTheme.typography.titleMedium
            )
            state.weeklyConsumption.forEach { reading ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(reading.periodStart.toString().substring(0, 10))
                    Text("${"%.2f".format(reading.energyKwh)} kWh")
                }
            }

            Spacer(Modifier.height(8.dp))

            androidx.compose.material3.Button(
                onClick = onDevicesClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver dispositivos")
            }
        }
    }
}
