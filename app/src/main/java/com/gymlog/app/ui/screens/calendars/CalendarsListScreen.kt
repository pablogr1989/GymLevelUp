package com.gymlog.app.ui.screens.calendars

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.domain.model.Calendar
import com.gymlog.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarsListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: CalendarsViewModel = hiltViewModel()
) {
    val calendars by viewModel.calendars.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendars_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp)) },
                actions = {
                    // Botón pequeño para añadir
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.AddCircle, contentDescription = stringResource(R.string.main_cd_new), tint = HunterPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HunterBlack,
                    titleContentColor = HunterTextPrimary
                )
            )
        }
    ) { paddingValues ->
        if (calendars.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = HunterTextSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.calendars_empty_state_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = HunterTextSecondary
                    )
                    HunterButton(
                        text = stringResource(R.string.calendars_btn_create),
                        onClick = onNavigateToCreate,
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(calendars) { calendar ->
                    CalendarHunterCard(
                        calendar = calendar,
                        onClick = { onNavigateToDetail(calendar.id) },
                        onDelete = { viewModel.showDeleteDialog(calendar) }
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { calendar ->
        HunterConfirmDialog(
            title = stringResource(R.string.calendars_dialog_delete_title),
            text = stringResource(R.string.calendars_dialog_delete_text, calendar.name),
            confirmText = stringResource(R.string.common_delete),
            onConfirm = { viewModel.deleteCalendar(calendar) },
            onDismiss = viewModel::dismissDeleteDialog
        )
    }
}

@Composable
private fun CalendarHunterCard(
    calendar: Calendar,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    HunterCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icono Decorativo
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = HunterPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = calendar.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = HunterTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.calendars_created_prefix, dateFormat.format(Date(calendar.createdAt))),
                        style = MaterialTheme.typography.bodySmall,
                        color = HunterTextSecondary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = HunterTextSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }
}