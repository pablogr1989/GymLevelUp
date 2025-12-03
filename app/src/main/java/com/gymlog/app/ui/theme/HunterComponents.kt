package com.gymlog.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlog.app.R

@Composable
fun HunterInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        keyboardOptions = keyboardOptions,
        readOnly = readOnly,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = HunterTextPrimary,
            unfocusedTextColor = HunterTextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = trailingIcon
    )
}

@Composable
fun HunterConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = HunterTextPrimary
            )
        },
        text = { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmText, color = HunterTextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                // Usamos el recurso común "CANCELAR"
                Text(stringResource(R.string.common_cancel), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Composable
fun HunterCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = content
    )
}

@Composable
fun HunterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.Black,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (icon != null) {
            icon()
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
            color = textColor
        )
    }
}