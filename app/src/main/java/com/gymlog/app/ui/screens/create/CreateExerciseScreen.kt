package com.gymlog.app.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val series by viewModel.series.collectAsState()
    val reps by viewModel.reps.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val showMuscleGroupError by viewModel.showMuscleGroupError.collectAsState()
    val showNameError by viewModel.showNameError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val navigateBack by viewModel.navigateBack.collectAsState()

    var showMuscleGroupDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateImageUri(uri)
    }

    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            onNavigateBack()
            viewModel.resetNavigation()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "NUEVO OBJETIVO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. SLOT DE IMAGEN
            HunterCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = { imagePickerLauncher.launch("image/*") }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Imagen del ejercicio",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Overlay para cambiar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = HunterPrimary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "AÑADIR VISUAL",
                                style = MaterialTheme.typography.labelMedium,
                                color = HunterPrimary
                            )
                        }
                    }
                }
            }

            // 2. DATOS BÁSICOS
            HunterCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("DATOS DE IDENTIFICACIÓN", style = MaterialTheme.typography.labelLarge, color = HunterPrimary)

                    HunterInput(
                        value = name,
                        onValueChange = viewModel::updateName,
                        label = "NOMBRE CLAVE *"
                    )
                    if (showNameError) {
                        Text("Requerido", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }

                    HunterInput(
                        value = description,
                        onValueChange = viewModel::updateDescription,
                        label = "DESCRIPCIÓN TÁCTICA"
                    )

                    // Selector de Grupo
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMuscleGroupDialog = true },
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.background),
                        border = BorderStroke(1.dp, if (showMuscleGroupError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMuscleGroup?.displayName?.uppercase() ?: "SELECCIONAR CLASE *",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedMuscleGroup != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = HunterPrimary)
                        }
                    }
                }
            }

            // 3. VALORES INICIALES (Opcional)
            HunterCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("PARÁMETROS INICIALES (OPCIONAL)", style = MaterialTheme.typography.labelLarge, color = HunterPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HunterInput(
                            value = series,
                            onValueChange = viewModel::updateSeries,
                            label = "SERIES",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        HunterInput(
                            value = reps,
                            onValueChange = viewModel::updateReps,
                            label = "REPS",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    HunterInput(
                        value = weight,
                        onValueChange = viewModel::updateWeight,
                        label = "PESO (KG)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HunterButton(
                text = "REGISTRAR OBJETIVO",
                onClick = viewModel::saveExercise,
                enabled = !isLoading,
                icon = {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                    else Icon(Icons.Default.Save, null, tint = Color.Black)
                }
            )
        }
    }

    if (showMuscleGroupDialog) {
        AlertDialog(
            onDismissRequest = { showMuscleGroupDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("SELECCIONAR CLASE", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MuscleGroup.values().forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectMuscleGroup(group)
                                    showMuscleGroupDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMuscleGroup == group,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = HunterPrimary, unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = group.displayName.uppercase(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMuscleGroupDialog = false }) {
                    Text("CANCELAR", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}