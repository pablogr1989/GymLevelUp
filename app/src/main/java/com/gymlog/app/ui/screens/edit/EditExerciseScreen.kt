package com.gymlog.app.ui.screens.edit

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Set

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditSet: (String, String?) -> Unit = { _, _ -> },
    viewModel: EditExerciseViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val sets by viewModel.sets.collectAsState()
    val showMuscleGroupError by viewModel.showMuscleGroupError.collectAsState()
    val showNameError by viewModel.showNameError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val navigateBack by viewModel.navigateBack.collectAsState()
    val showExitConfirmation by viewModel.showExitConfirmation.collectAsState()

    var showMuscleGroupDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateImageUri(uri)
    }

    // Recargar datos al volver de la pantalla de Sets
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadExercise()
    }

    BackHandler {
        viewModel.onBackPressed()
    }

    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            onNavigateBack()
            viewModel.resetNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar ejercicio") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackPressed() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Imagen del ejercicio",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Añadir imagen",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Basic information
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Información básica", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Nombre *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showNameError
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMuscleGroupDialog = true },
                        border = if (showMuscleGroupError) {
                            CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MaterialTheme.colorScheme.error))
                        } else {
                            CardDefaults.outlinedCardBorder()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMuscleGroup?.displayName ?: "Seleccionar grupo muscular *",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }
            }

            // SETS SECTION
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Variantes (Sets)", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = {
                            onNavigateToEditSet(name, null)
                        }) {
                            Icon(Icons.Default.Add, "Añadir Set")
                        }
                    }

                    if (sets.isEmpty()) {
                        Text(
                            "No hay sets configurados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        sets.forEachIndexed { index, set ->
                            SetItem(
                                set = set,
                                index = index + 1,
                                onEdit = { onNavigateToEditSet(name, set.id) },
                                onDelete = { viewModel.deleteSet(set.id) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::saveExercise,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Guardar Cambios")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showMuscleGroupDialog) {
        AlertDialog(
            onDismissRequest = { showMuscleGroupDialog = false },
            title = { Text("Grupo muscular") },
            text = {
                Column {
                    MuscleGroup.values().forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectMuscleGroup(group)
                                    showMuscleGroupDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMuscleGroup == group,
                                onClick = {
                                    viewModel.selectMuscleGroup(group)
                                    showMuscleGroupDialog = false
                                }
                            )
                            Text(group.displayName)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMuscleGroupDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExitConfirmation,
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmExit) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExitConfirmation) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun SetItem(
    set: Set,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Variante #$index",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${set.series} series × ${set.reps} reps @ ${set.weightKg} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}