package com.gymlog.app.ui.screens.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSetScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditSetViewModel = hiltViewModel()
) {
    val series by viewModel.series.collectAsState()
    val reps by viewModel.reps.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val navigateBack by viewModel.navigateBack.collectAsState()
    val showExitConfirmation by viewModel.showExitConfirmation.collectAsState()

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
                        stringResource(R.string.set_edit_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackPressed() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = HunterTextPrimary)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HunterCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.set_section_params), style = MaterialTheme.typography.labelLarge, color = HunterPrimary)

                    HunterInput(
                        value = series,
                        onValueChange = viewModel::updateSeries,
                        label = stringResource(R.string.common_series),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    HunterInput(
                        value = reps,
                        onValueChange = viewModel::updateReps,
                        label = stringResource(R.string.set_label_reps),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    HunterInput(
                        value = weight,
                        onValueChange = viewModel::updateWeight,
                        label = stringResource(R.string.common_weight_kg),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HunterButton(
                text = stringResource(R.string.set_btn_save),
                onClick = viewModel::saveSet,
                enabled = !isLoading,
                icon = {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                    else Icon(Icons.Default.Save, null, tint = Color.Black)
                }
            )
        }
    }

    if (showExitConfirmation) {
        HunterConfirmDialog(
            title = stringResource(R.string.set_dialog_discard_title),
            text = stringResource(R.string.set_dialog_discard_text),
            confirmText = stringResource(R.string.exercise_dialog_discard_confirm), // Reutilizamos "SALIR"
            onConfirm = viewModel::confirmExit,
            onDismiss = viewModel::dismissExitConfirmation
        )
    }
}