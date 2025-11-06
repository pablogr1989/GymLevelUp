package com.gymlog.app.ui.screens.edit

import android.net.Uri
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
import coil.compose.AsyncImage
import com.gymlog.app.data.local.entity.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditExerciseViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
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
        topBar = {
            TopAppBar(
                title = { Text("Editar ejercicio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cambiar imagen",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Información básica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Nombre del ejercicio *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showNameError,
                        supportingText = if (showNameError) {
                            { Text("El nombre es obligatorio") }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null
                            )
                        }
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null
                            )
                        }
                    )
                    
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMuscleGroupDialog = true },
                        border = if (showMuscleGroupError) {
                            CardDefaults.outlinedCardBorder().copy(
                                width = 2.dp,
                                brush = SolidColor(MaterialTheme.colorScheme.error)
                            )
                        } else {
                            CardDefaults.outlinedCardBorder()
                        }
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
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (selectedMuscleGroup != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = selectedMuscleGroup?.displayName ?: "Seleccionar grupo muscular *",
                                    color = if (selectedMuscleGroup != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    }
                    
                    if (showMuscleGroupError) {
                        Text(
                            text = "Debes seleccionar un grupo muscular",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }
            
            Button(
                onClick = viewModel::saveExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar cambios",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showMuscleGroupDialog) {
        AlertDialog(
            onDismissRequest = { showMuscleGroupDialog = false },
            title = { Text("Seleccionar grupo muscular") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MuscleGroup.entries.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectMuscleGroup(group)
                                    showMuscleGroupDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMuscleGroup == group,
                                onClick = {
                                    viewModel.selectMuscleGroup(group)
                                    showMuscleGroupDialog = false
                                }
                            )
                            Text(
                                text = group.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMuscleGroupDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
