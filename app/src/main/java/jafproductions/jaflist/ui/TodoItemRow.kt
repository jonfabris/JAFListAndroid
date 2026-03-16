package jafproductions.jaflist.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jafproductions.jaflist.models.TodoItem
import jafproductions.jaflist.viewmodels.AppViewModel
import kotlin.math.roundToInt

private const val BUTTON_WIDTH_DP = 80f
private const val TOTAL_SWIPE_DP = BUTTON_WIDTH_DP * 2
// Approximate pixels for xxhdpi (3x); actual snap uses raw drag units from pointer input
private const val REVEAL_THRESHOLD_RAW = TOTAL_SWIPE_DP * 3f

@Composable
fun TodoItemRow(
    item: TodoItem,
    folderId: String,
    depth: Int,
    appViewModel: AppViewModel
) {
    var isEditing by remember(item.id) { mutableStateOf(false) }
    var editText by remember(item.id) { mutableStateOf(item.text) }
    var showAddSubitemDialog by remember { mutableStateOf(false) }
    var newSubitemText by remember { mutableStateOf("") }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = rawOffsetX,
        animationSpec = tween(durationMillis = 150),
        label = "swipe_offset"
    )

    // Outer Box: fixed row height, clips the slide
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        // Background action buttons — anchored to the right edge
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(56.dp)
        ) {
            // Add Subitem (blue, left of delete)
            Box(
                modifier = Modifier
                    .width(BUTTON_WIDTH_DP.dp)
                    .height(56.dp)
                    .background(Color(0xFF1976D2))
                    .clickable {
                        newSubitemText = ""
                        showAddSubitemDialog = true
                        rawOffsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Subitem",
                    tint = Color.White
                )
            }
            // Delete (red, rightmost)
            Box(
                modifier = Modifier
                    .width(BUTTON_WIDTH_DP.dp)
                    .height(56.dp)
                    .background(Color(0xFFD32F2F))
                    .clickable {
                        appViewModel.deleteItem(folderId, item.id)
                        rawOffsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }

        // Foreground sliding content
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val revealed = -rawOffsetX > REVEAL_THRESHOLD_RAW / 2f
                            rawOffsetX = if (revealed) -REVEAL_THRESHOLD_RAW else 0f
                        },
                        onDragCancel = {
                            rawOffsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            rawOffsetX = (rawOffsetX + dragAmount).coerceIn(-REVEAL_THRESHOLD_RAW, 0f)
                        }
                    )
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Depth indent
                if (depth > 0) {
                    Spacer(modifier = Modifier.width((depth * 20).dp))
                }

                // Checkbox icon
                val checkboxIcon = if (item.isCompleted) {
                    Icons.Default.CheckBox
                } else {
                    Icons.Default.CheckBoxOutlineBlank
                }
                val checkboxTint = if (item.isCompleted) Color(0xFF388E3C) else Color(0xFF9E9E9E)

                Icon(
                    imageVector = checkboxIcon,
                    contentDescription = if (item.isCompleted) "Completed" else "Not completed",
                    tint = checkboxTint,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { appViewModel.toggleItemCompletion(folderId, item.id) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Text area
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isEditing) {
                        BasicTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = item.text,
                            fontSize = 16.sp,
                            color = if (item.isCompleted) Color(0xFF9E9E9E) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editText = item.text
                                    isEditing = true
                                }
                        )
                    }
                }

                // Done button while editing
                if (isEditing) {
                    TextButton(
                        onClick = {
                            if (editText.isNotBlank()) {
                                appViewModel.editItem(folderId, item.id, editText.trim())
                            }
                            isEditing = false
                        }
                    ) {
                        Text("Done", fontSize = 12.sp)
                    }
                }

                // Expansion chevron (only when not editing)
                if (item.children.isNotEmpty() && !isEditing) {
                    val chevronIcon = if (item.isExpanded) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.Default.KeyboardArrowRight
                    }
                    Icon(
                        imageVector = chevronIcon,
                        contentDescription = if (item.isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { appViewModel.toggleItemExpansion(folderId, item.id) }
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }

    // Add Subitem dialog — rendered outside the Box as a dialog overlay
    if (showAddSubitemDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubitemDialog = false },
            title = { Text("New Subitem") },
            text = {
                OutlinedTextField(
                    value = newSubitemText,
                    onValueChange = { newSubitemText = it },
                    label = { Text("Subitem Text") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSubitemText.isNotBlank()) {
                            appViewModel.addChildItem(folderId, item.id, newSubitemText.trim())
                            showAddSubitemDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubitemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
