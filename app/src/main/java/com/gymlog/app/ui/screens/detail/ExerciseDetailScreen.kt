package com.gymlog.app.ui.screens.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.gymlog.app.R
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.model.Set
import com.gymlog.app.ui.theme.*
import com.gymlog.app.ui.util.UiMappers
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToEditSet: (String, String?) -> Unit = { _, _ -> },
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val exercise by viewModel.exercise.collectAsState()
    val history by viewModel.history.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showDeleteHistoryDialog by viewModel.showDeleteHistoryDialog.collectAsState()
    val showDeleteEntryDialog by viewModel.showDeleteEntryDialog.collectAsState()
    val showDeleteSetDialog by viewModel.showDeleteSetDialog.collectAsState()

    // Recargar al volver de editar un set
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadExercise()
    }

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.detail_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.common_back), tint = HunterTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { exercise?.let { onNavigateToEdit(it.id) } }) {
                        Icon(Icons.Default.Settings, stringResource(R.string.detail_cd_edit_file), tint = HunterTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HunterBlack,
                    titleContentColor = HunterTextPrimary,
                    actionIconContentColor = HunterPrimary
                )
            )
        }
    ) { paddingValues ->
        exercise?.let { ex ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // HEADER
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HunterLargeImageBox(exercise = ex)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = ex.name.uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = HunterTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        // REFACTORIZACIÓN CRÍTICA: Mapeo de Enum
                        HunterChip(text = stringResource(UiMappers.getDisplayNameRes(ex.muscleGroup)).uppercase())
                    }
                }

                // SECCIÓN: SETS PLANIFICADOS (Nueva)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(title = stringResource(R.string.detail_section_combat_config))

                            IconButton(onClick = { onNavigateToEditSet(ex.id, null) }) {
                                Icon(Icons.Default.AddCircle, stringResource(R.string.detail_cd_add_set), tint = HunterPrimary)
                            }
                        }

                        if (ex.sets.isEmpty()) {
                            Text(
                                stringResource(R.string.detail_no_variants),
                                style = MaterialTheme.typography.bodyMedium,
                                color = HunterTextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ex.sets.forEachIndexed { index, set ->
                                    HunterSetCard(
                                        set = set,
                                        index = index + 1,
                                        onClick = { onNavigateToEditSet(ex.id, set.id) },
                                        onDelete = { viewModel.confirmDeleteSet(set.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // SECCIÓN: NOTAS (Solo lectura/edición rápida)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(title = stringResource(R.string.detail_section_notes))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = viewModel::updateNotes,
                            placeholder = { Text(stringResource(R.string.detail_notes_placeholder), color = HunterTextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            colors = HunterInputColors(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (notes.trim() != ex.notes) {
                                    IconButton(onClick = viewModel::saveNotes) {
                                        Icon(Icons.Default.Save, stringResource(R.string.common_save), tint = HunterPrimary)
                                    }
                                }
                            }
                        )
                    }
                }

                // SECCIÓN: HISTORIAL
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(title = stringResource(R.string.detail_section_history))

                            if (history.isNotEmpty()) {
                                TextButton(onClick = viewModel::showDeleteHistoryDialog) {
                                    Text(stringResource(R.string.detail_btn_delete_history), color = HunterSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.detail_no_history),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HunterTextSecondary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                items(history) { entry ->
                    HistoryLogItem(
                        entry = entry,
                        onDelete = { viewModel.showDeleteEntryDialog(entry) }
                    )
                }
            }
        }
    }

    // Diálogos con textos centralizados
    if (showDeleteHistoryDialog) {
        HunterConfirmDialog(
            title = stringResource(R.string.detail_dialog_delete_history_title),
            text = stringResource(R.string.detail_dialog_delete_history_text),
            confirmText = stringResource(R.string.common_delete),
            onConfirm = viewModel::deleteAllHistory,
            onDismiss = viewModel::dismissDeleteHistoryDialog
        )
    }

    showDeleteEntryDialog?.let { entry ->
        HunterConfirmDialog(
            title = stringResource(R.string.detail_dialog_delete_entry_title),
            text = stringResource(R.string.detail_dialog_delete_entry_text),
            confirmText = stringResource(R.string.common_delete),
            onConfirm = { viewModel.deleteHistoryEntry(entry) },
            onDismiss = viewModel::dismissDeleteEntryDialog
        )
    }

    showDeleteSetDialog?.let { _ ->
        HunterConfirmDialog(
            title = stringResource(R.string.detail_dialog_delete_set_title),
            text = stringResource(R.string.detail_dialog_delete_set_text),
            confirmText = stringResource(R.string.common_delete),
            onConfirm = viewModel::deleteSet,
            onDismiss = viewModel::dismissDeleteSetDialog
        )
    }
}

// ============ COMPONENTES VISUALES ============

@Composable
fun HunterSetCard(
    set: Set,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HunterSurface),
        border = BorderStroke(1.dp, HunterPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Badge de número
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(HunterPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$index",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = HunterPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Datos
                Column {
                    Text(
                        text = "${set.weightKg} ${stringResource(R.string.common_kg)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = HunterTextPrimary
                    )
                    Text(
                        text = "${set.series} ${stringResource(R.string.common_series)} × ${set.reps} ${stringResource(R.string.common_reps)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HunterTextSecondary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.common_delete), tint = HunterSecondary.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .background(HunterPrimary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = HunterTextSecondary
        )
    }
}

@Composable
fun HunterInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = HunterBlack,
    unfocusedContainerColor = HunterBlack,
    focusedBorderColor = HunterPrimary,
    unfocusedBorderColor = HunterPrimary.copy(alpha = 0.3f), // Reemplazo de color outline hardcodeado
    focusedLabelColor = HunterPrimary,
    unfocusedLabelColor = HunterTextSecondary,
    cursorColor = HunterPrimary,
    focusedTextColor = HunterTextPrimary,
    unfocusedTextColor = HunterTextPrimary
)

@Composable
fun HunterLargeImageBox(exercise: Exercise) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(HunterSurface)
            .border(2.dp, HunterPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    ) {
        if (exercise.imageUri != null) {
            AsyncImage(
                model = exercise.imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val iconRes = getGroupIcon(exercise.muscleGroup)
            // Color primario extraído
            val primaryColor = HunterPrimary

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                            radius = 400f
                        )
                    )
            )

            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
            )
        }
    }
}

@Composable
private fun HistoryLogItem(
    entry: ExerciseHistory,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = HunterSurface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(HunterBlack, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = HunterTextSecondary
                    )
                    Text(
                        text = timeFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = HunterTextSecondary.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "${entry.weightKg} ${stringResource(R.string.common_kg)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = HunterPrimary)
                    )
                    Text(
                        text = "${entry.series} ${stringResource(R.string.common_series)} × ${entry.reps} ${stringResource(R.string.common_reps)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HunterTextPrimary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    stringResource(R.string.common_delete),
                    tint = HunterTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HunterChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = HunterSurface,
        border = BorderStroke(1.dp, HunterPrimary.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = HunterPrimary
        )
    }
}

@DrawableRes
private fun getGroupIcon(group: MuscleGroup): Int {
    return when (group) {
        MuscleGroup.LEGS -> com.gymlog.app.R.drawable.ic_pierna
        MuscleGroup.GLUTES -> com.gymlog.app.R.drawable.ic_gluteos
        MuscleGroup.BACK -> com.gymlog.app.R.drawable.ic_espalda
        MuscleGroup.CHEST -> com.gymlog.app.R.drawable.ic_torso
        MuscleGroup.BICEPS -> com.gymlog.app.R.drawable.ic_biceps
        MuscleGroup.TRICEPS -> com.gymlog.app.R.drawable.ic_triceps
        MuscleGroup.SHOULDERS -> com.gymlog.app.R.drawable.ic_hombros
    }
}