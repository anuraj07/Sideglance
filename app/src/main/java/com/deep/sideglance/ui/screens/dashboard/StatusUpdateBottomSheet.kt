package com.deep.sideglance.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deep.sideglance.domain.model.UserStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusUpdateBottomSheet(
    onDismissRequest: () -> Unit,
    onStatusSelected: (UserStatus) -> Unit
) {
    val statusTypes = listOf(
        Pair("Work", "💼"),
        Pair("Cook", "🍳"),
        Pair("Play", "🎮"),
        Pair("Rest", "😴"),
        Pair("Travel", "🚗"),
        Pair("Custom", "✨")
    )
    var selectedType by remember { mutableStateOf("Work") }
    var selectedEmoji by remember { mutableStateOf("💼") }
    var detailText by remember { mutableStateOf("") }
    
    var customActivityText by remember { mutableStateOf("") }
    var customEmojiText by remember { mutableStateOf("✨") }
    
    // Expiration options in milliseconds: 0L = Never, otherwise relative from now
    val expirationOptions = listOf(
        Pair("1 Hour", 1000L * 60 * 60),
        Pair("4 Hours", 1000L * 60 * 60 * 4),
        Pair("8 Hours", 1000L * 60 * 60 * 8),
        Pair("1 Day", 1000L * 60 * 60 * 24), // default
        Pair("Forever", 0L)
    )
    var selectedExpirationIndex by remember { mutableStateOf(0) } // Default to 1 hour

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "What are you doing?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Status Types Grid/Chips in a 2x3 layout
            val firstRow = statusTypes.take(3)
            val secondRow = statusTypes.drop(3)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    firstRow.forEach { (type, emoji) ->
                        StatusChip(
                            type = type,
                            emoji = emoji,
                            isSelected = selectedType == type,
                            onClick = {
                                selectedType = type
                                selectedEmoji = emoji
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    secondRow.forEach { (type, emoji) ->
                        StatusChip(
                            type = type,
                            emoji = emoji,
                            isSelected = selectedType == type,
                            onClick = {
                                selectedType = type
                                selectedEmoji = emoji
                            }
                        )
                    }
                }
            }

            // Custom Status Input fields (visible only when "Custom" is selected)
            if (selectedType == "Custom") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = customEmojiText,
                        onValueChange = { customEmojiText = it.take(2) },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)
                    )
                    OutlinedTextField(
                        value = customActivityText,
                        onValueChange = { customActivityText = it },
                        label = { Text("Custom Activity") },
                        placeholder = { Text("e.g. Gym, Coffee, Study") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Mirror Modification Detail Field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Add some detail",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = detailText,
                    onValueChange = { detailText = it },
                    placeholder = { Text("e.g. Football, Pasta, Coding...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }

            // Expiration Time Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How long should this last?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expirationOptions.forEachIndexed { index, (label, _) ->
                        val isSelected = selectedExpirationIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedExpirationIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            Button(
                onClick = {
                    val relativeMs = expirationOptions[selectedExpirationIndex].second
                    val expirationTime = if (relativeMs == 0L) 0L else System.currentTimeMillis() + relativeMs
                    
                    val finalEmoji = if (selectedType == "Custom") customEmojiText.ifBlank { "✨" } else selectedEmoji
                    val finalType = if (selectedType == "Custom") customActivityText.ifBlank { "Custom" } else selectedType
                    
                    val status = UserStatus(
                        statusType = "$finalEmoji $finalType",
                        detail = detailText.ifBlank { null },
                        timestamp = System.currentTimeMillis(),
                        expirationTime = expirationTime
                    )
                    onStatusSelected(status)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Set Status", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun RowScope.StatusChip(
    type: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = type,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
