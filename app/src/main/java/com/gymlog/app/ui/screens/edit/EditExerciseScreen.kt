package com.gymlog.app.ui.screens.edit

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gymlog.app.R
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.ui.theme.*
import com.gymlog.app.ui.util.UiMappers

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
    val showExitConfirmation by viewModel.showExitConfirmation.collectAsState()

    var showMuscleGroupDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateImageUri(uri)
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
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.exercise_edit_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackPressed() }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = HunterTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HunterBlack,
                    titleContentColor = HunterTextPrimary
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
            // 1. IMAGEN
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
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ScreenColors.CreateExercise.ImageOverlay),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = HunterTextPrimary,
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
                                tint = ScreenColors.CreateExercise.IconTint
                            )
                            Text(
                                text = stringResource(R.string.exercise_update_visual),
                                style = MaterialTheme.typography.labelMedium,
                                color = HunterPrimary
                            )
                        }
                    }
                }
            }

            // 2. DATOS
            HunterCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.exercise_section_id_data), style = MaterialTheme.typography.labelLarge, color = HunterPrimary)

                    HunterInput(
                        value = name,
                        onValueChange = viewModel::updateName,
                        label = stringResource(R.string.exercise_name_label)
                    )

                    HunterInput(
                        value = description,
                        onValueChange = viewModel::updateDescription,
                        label = stringResource(R.string.exercise_desc_label)
                    )

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMuscleGroupDialog = true },
                        colors = CardDefaults.outlinedCardColors(containerColor = HunterBlack),
                        border = BorderStroke(1.dp, if (showMuscleGroupError) MaterialTheme.colorScheme.error else HunterPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMuscleGroup?.let { stringResource(UiMappers.getDisplayNameRes(it)).uppercase() }
                                    ?: stringResource(R.string.exercise_select_class_label),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedMuscleGroup != null) HunterTextPrimary else HunterTextSecondary
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = HunterPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HunterButton(
                text = stringResource(R.string.exercise_btn_save_changes),
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
            containerColor = HunterSurface,
            title = { Text(stringResource(R.string.exercise_dialog_group_title_edit), color = HunterTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMuscleGroup == group,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = HunterPrimary, unselectedColor = HunterTextSecondary)
                            )
                            Text(
                                text = stringResource(UiMappers.getDisplayNameRes(group)).uppercase(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = HunterTextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMuscleGroupDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = HunterTextPrimary)
                }
            }
        )
    }

    if (showExitConfirmation) {
        HunterConfirmDialog(
            title = stringResource(R.string.exercise_dialog_discard_title),
            text = stringResource(R.string.exercise_dialog_discard_text),
            confirmText = stringResource(R.string.exercise_dialog_discard_confirm),
            onConfirm = viewModel::confirmExit,
            onDismiss = viewModel::dismissExitConfirmation
        )
    }
}