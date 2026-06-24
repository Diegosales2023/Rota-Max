package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RouteViewModel

@Composable
fun ReuseStopsScreen(viewModel: RouteViewModel, modifier: Modifier = Modifier) {
    val routeDates by viewModel.allRouteDates.collectAsState()
    val currentRouteDate by viewModel.routeDate.collectAsState()

    // Screen states matching Image 2
    var fromDate by remember { mutableStateOf("19-06-2026") }
    var toDate by remember { mutableStateOf(currentRouteDate) }

    var copyPending by remember { mutableStateOf(true) }
    var copySkipped by remember { mutableStateOf(true) }
    var copyCompleted by remember { mutableStateOf(false) }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.MAIN) },
                    modifier = Modifier.testTag("reuse_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Reutilizar paradas",
                    fontSize = 20.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { /* Simulated search */ }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Pesquisar",
                        tint = TextPrimary
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // "De" Route Selector
            Text(text = "De:", fontSize = 14.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .clickable { fromExpanded = true }
                    .padding(16.dp)
                    .testTag("reuse_from_selector")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "De: $fromDate - DIEGO DOS SANTOS",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = fromExpanded,
                    onDismissRequest = { fromExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    routeDates.forEach { date ->
                        DropdownMenuItem(
                            text = { Text(text = "$date - DIEGO DOS SANTOS", color = TextPrimary) },
                            onClick = {
                                fromDate = date
                                fromExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Para" Route Selector
            Text(text = "Para:", fontSize = 14.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .clickable { toExpanded = true }
                    .padding(16.dp)
                    .testTag("reuse_to_selector")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Para: $toDate - DIEGO DOS SANTOS",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = toExpanded,
                    onDismissRequest = { toExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    routeDates.forEach { date ->
                        DropdownMenuItem(
                            text = { Text(text = "$date - DIEGO DOS SANTOS", color = TextPrimary) },
                            onClick = {
                                toDate = date
                                toExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Paradas não realizadas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = copyPending,
                    onCheckedChange = { copyPending = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandBlue,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.testTag("checkbox_pending")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Paradas não realizadas", color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Nenhuma parada não realizada nesta rota", color = TextSecondary, fontSize = 13.sp)
                }
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Pendente",
                    tint = StatusSkipped,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 2: Paradas puladas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = copySkipped,
                    onCheckedChange = { copySkipped = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandBlue,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.testTag("checkbox_skipped")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Paradas puladas", color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Nenhuma parada pulada nesta rota", color = TextSecondary, fontSize = 13.sp)
                }
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Pulada",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 3: Paradas feitas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = copyCompleted,
                    onCheckedChange = { copyCompleted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandBlue,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.testTag("checkbox_completed")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Paradas feitas", color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Nenhuma parada feita nesta rota", color = TextSecondary, fontSize = 13.sp)
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Feita",
                    tint = StatusCompleted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Button
            Button(
                onClick = {
                    viewModel.copyStops(
                        fromDate = fromDate,
                        toDate = toDate,
                        copyPending = copyPending,
                        copySkipped = copySkipped,
                        copyCompleted = copyCompleted
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("copy_stops_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (copyPending || copySkipped || copyCompleted) BrandBlue else DarkSurfaceVariant,
                    contentColor = if (copyPending || copySkipped || copyCompleted) Color.White else TextSecondary
                ),
                shape = RoundedCornerShape(25.dp),
                enabled = copyPending || copySkipped || copyCompleted
            ) {
                Text(text = "Copiar paradas", fontSize = 16.sp)
            }
        }
    }
}
