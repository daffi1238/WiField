package com.wifield.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SnapshotDialog(
    onDismiss: () -> Unit,
    onSave: (label: String) -> Unit,
    title: String = "Guardar Snapshot"
) {
    var label by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Text(
                    text = "Asigne un nombre a esta ubicación:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        isError = false
                    },
                    label = { Text("Nombre de ubicación") },
                    placeholder = { Text("Ej: Sala de reuniones, Pasillo planta 2...") },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("El nombre no puede estar vacío") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) {
                        isError = true
                    } else {
                        onSave(label.trim())
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ProjectDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
    title: String = "Nuevo Proyecto",
    initialName: String = "",
    initialDescription: String = ""
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Nombre del proyecto") },
                    placeholder = { Text("Ej: Oficina Central, Almacén Norte...") },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("El nombre no puede estar vacío") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Notas sobre el edificio o proyecto...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else {
                        onSave(name.trim(), description.trim())
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
