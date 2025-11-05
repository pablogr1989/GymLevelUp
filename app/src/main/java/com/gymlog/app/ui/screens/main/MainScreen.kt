package com.gymlog.app.ui.screens.main

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.res.painterResource
import com.gymlog.app.data.local.entity.DayCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    // Preexpandir grupos comunes y usar remember para evitar recreación
    var expandedGroups by remember {
        mutableStateOf(setOf(MuscleGroup.LEGS, MuscleGroup.CHEST))
    }

    // Cachear grupos visibles para evitar recalcular
    val visibleGroups = remember(exercises) {
        exercises.keys.sortedBy { it.ordinal }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GymLevelUp")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::prepopulateDatabase) {
                        Icon(Icons.Default.Info, contentDescription = "Repoblar base de datos")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir ejercicio")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.padding(16.dp)
            )

            // Muscle group filter chips
            MuscleGroupFilterChips(
                selectedGroup = selectedMuscleGroup,
                onGroupSelected = viewModel::selectMuscleGroup,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Exercise list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                visibleGroups.forEach { group ->
                    val groupExercises = exercises[group] ?: emptyList()
                    if (groupExercises.isNotEmpty()) {
                        item(key = "header_${group.name}") {
                            MuscleGroupHeader(
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
                                key = { exercise -> "exercise_${exercise.id}" },
                                contentType = { "exercise" }
                            ) { exercise ->
                                ExerciseCard(
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

    // Delete confirmation dialog
    showDeleteDialog?.let { exercise ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Eliminar ejercicio") },
            text = {
                Text("¿Estas seguro de que quieres eliminar '${exercise.name}'? Se eliminara tambien todo su historial.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteExercise(exercise) }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Buscar ejercicio") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Buscar")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun MuscleGroupFilterChips(
    selectedGroup: MuscleGroup?,
    onGroupSelected: (MuscleGroup?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedGroup == null,
                onClick = { onGroupSelected(null) },
                label = { Text("Todos") }
            )
        }
        items(MuscleGroup.values().toList()) { group ->
            FilterChip(
                selected = selectedGroup == group,
                onClick = { onGroupSelected(group) },
                label = { Text(group.displayName) }
            )
        }
    }
}

@Composable
private fun MuscleGroupHeader(
    group: MuscleGroup,
    isExpanded: Boolean,
    exerciseCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconRes = when (group) {
                    MuscleGroup.LEGS -> com.gymlog.app.R.drawable.ic_pierna
                    MuscleGroup.GLUTES -> com.gymlog.app.R.drawable.ic_gluteos
                    MuscleGroup.BACK -> com.gymlog.app.R.drawable.ic_espalda
                    MuscleGroup.CHEST -> com.gymlog.app.R.drawable.ic_torso
                    MuscleGroup.BICEPS -> com.gymlog.app.R.drawable.ic_biceps
                    MuscleGroup.TRICEPS -> com.gymlog.app.R.drawable.ic_triceps
                    MuscleGroup.SHOULDERS -> com.gymlog.app.R.drawable.ic_hombros
                }
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$exerciseCount ejercicios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Contraer" else "Expandir"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (exercise.currentSeries > 0 && exercise.currentReps > 0) {
                        Text(
                            text = "${exercise.currentSeries} — ${exercise.currentReps}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (exercise.currentWeightKg > 0) {
                        Text(
                            text = "${exercise.currentWeightKg} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            IconButton(onClick = onLongClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Mas opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/*
@Composable
private fun LazyRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}
*/