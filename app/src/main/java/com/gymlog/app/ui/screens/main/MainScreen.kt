package com.gymlog.app.ui.screens.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.ui.theme.*
import com.gymlog.app.ui.util.UiMappers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    // Mantenemos expandidos por defecto para ver el contenido
    var expandedGroups by remember {
        mutableStateOf(MuscleGroup.entries.toSet())
    }

    val visibleGroups = remember(exercises) {
        exercises.keys.sortedBy { it.ordinal }
    }

    Scaffold(
        containerColor = HunterBlack, // Color centralizado
        topBar = {
            // Header personalizado estilo "Player Profile"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HunterBlack)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.main_welcome_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = HunterPrimary
                        )
                        Text(
                            text = stringResource(R.string.main_exercises_header),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = HunterTextPrimary
                        )
                    }

                    // Botones de acción (Backup y Nuevo)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onNavigateToBackup,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HunterSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.main_cd_backup),
                                tint = HunterTextPrimary
                            )
                        }

                        IconButton(
                            onClick = onNavigateToCreate,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HunterPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.main_cd_new),
                                tint = HunterBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buscador Estilo Tech
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filtros Estilo Chips
                MuscleGroupFilterChips(
                    selectedGroup = selectedMuscleGroup,
                    onGroupSelected = viewModel::selectMuscleGroup
                )
            }
        }
    ) { paddingValues ->
        if (exercises.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.main_no_exercises_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = HunterTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                visibleGroups.forEach { group ->
                    val groupExercises = exercises[group] ?: emptyList()
                    if (groupExercises.isNotEmpty()) {
                        item(key = "header_${group.name}") {
                            MuscleGroupSection(
                                group = group,
                                isExpanded = expandedGroups.contains(group),
                                exerciseCount = groupExercises.size,
                                onClick = {
                                    expandedGroups = if (expandedGroups.contains(group)) {
                                        expandedGroups - group
                                    } else {
                                        expandedGroups + group
                                    }
                                }
                            )
                        }

                        if (expandedGroups.contains(group)) {
                            items(
                                items = groupExercises,
                                key = { exercise -> "exercise_${exercise.id}" }
                            ) { exercise ->
                                ExerciseHunterCard(
                                    exercise = exercise,
                                    onClick = { onNavigateToDetail(exercise.id) },
                                    onLongClick = { viewModel.showDeleteDialog(exercise) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogo de borrado (Estilizado)
    showDeleteDialog?.let { exercise ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            containerColor = HunterSurface,
            title = {
                Text(
                    text = stringResource(R.string.main_delete_dialog_title),
                    color = HunterTextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.main_delete_dialog_text, exercise.name),
                    color = HunterTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteExercise(exercise) },
                    colors = ButtonDefaults.buttonColors(containerColor = HunterSecondary)
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = HunterTextPrimary
                    )
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// COMPONENTES AUXILIARES (Esto es lo que te faltaba o estaba roto)
// -----------------------------------------------------------------------------

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.main_search_placeholder),
                color = HunterTextSecondary.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = HunterPrimary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.main_search_clear),
                        tint = HunterTextPrimary
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = HunterSurface,
            unfocusedContainerColor = HunterSurface,
            focusedBorderColor = HunterPrimary,
            unfocusedBorderColor = HunterPrimary.copy(alpha = 0.3f),
            cursorColor = HunterPrimary,
            focusedTextColor = HunterTextPrimary,
            unfocusedTextColor = HunterTextPrimary
        )
    )
}

@Composable
private fun MuscleGroupFilterChips(
    selectedGroup: MuscleGroup?,
    onGroupSelected: (MuscleGroup?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HunterChip(
                text = stringResource(R.string.main_filter_all),
                isSelected = selectedGroup == null,
                onClick = { onGroupSelected(null) }
            )
        }
        items(MuscleGroup.entries) { group ->
            HunterChip(
                // REFACTORIZACIÓN: Uso del nuevo UiMapper
                text = stringResource(UiMappers.getDisplayNameRes(group)),
                isSelected = selectedGroup == group,
                onClick = { onGroupSelected(group) }
            )
        }
    }
}

@Composable
private fun HunterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .height(32.dp),
        shape = RoundedCornerShape(100), // Pill shape
        color = if (isSelected) HunterPrimary else HunterSurface,
        border = if (!isSelected) BorderStroke(1.dp, HunterPrimary.copy(alpha = 0.3f)) else null
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) HunterBlack else HunterTextPrimary
            )
        }
    }
}

@Composable
private fun MuscleGroupSection(
    group: MuscleGroup,
    isExpanded: Boolean,
    exerciseCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Línea decorativa vertical
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HunterPrimary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(UiMappers.getDisplayNameRes(group)).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = HunterTextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Badge(
                containerColor = HunterSurface,
                contentColor = HunterPrimary
            ) {
                Text("$exerciseCount")
            }
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = HunterTextSecondary
        )
    }
}

@Composable
private fun ExerciseHunterCard(
    exercise: Exercise,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HunterSurface),
        border = BorderStroke(1.dp, ScreenColors.MainScreen.IconBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonIconBox(exercise = exercise)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = HunterTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onLongClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.main_cd_options),
                    tint = HunterTextSecondary
                )
            }
        }
    }
}

@Composable
fun NeonIconBox(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    // Contenedor principal
    Box(
        modifier = modifier
            .size(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HunterBlack)
            .border(1.dp, ScreenColors.MainScreen.IconBorder, RoundedCornerShape(12.dp))
    ) {
        val iconRes = getGroupIcon(exercise.muscleGroup)

        // 1. Capa de brillo (Glow) detrás
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(50.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ScreenColors.MainScreen.NeonGlowStart,
                            ScreenColors.MainScreen.NeonGlowEnd
                        )
                    )
                )
        )

        // 2. Icono Principal (Nítido encima)
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = HunterPrimary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp)
        )
    }
}

@DrawableRes
private fun getGroupIcon(group: MuscleGroup): Int {
    return when (group) {
        MuscleGroup.LEGS -> R.drawable.ic_pierna
        MuscleGroup.BICEPS -> R.drawable.ic_biceps
        MuscleGroup.GLUTES -> R.drawable.ic_gluteos
        MuscleGroup.CHEST -> R.drawable.ic_torso
        MuscleGroup.TRICEPS -> R.drawable.ic_triceps
        MuscleGroup.SHOULDERS -> R.drawable.ic_hombros
        MuscleGroup.BACK -> R.drawable.ic_espalda
        else -> R.drawable.ic_exercise_placeholder
    }
}