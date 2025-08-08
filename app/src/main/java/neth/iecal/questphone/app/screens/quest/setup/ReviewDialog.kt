package neth.iecal.questphone.app.screens.quest.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nethical.questphone.data.ExcludeFromReviewDialog
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


@Composable
fun ReviewDialog(
    items: List<Any>, // Mixed data classes
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Data") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(items) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = item::class.simpleName ?: "Item",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        RenderObject(item)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenderObject(item: Any, indentLevel: Int = 0) {
    val properties = item::class.memberProperties

    properties.forEach { prop ->
        // Skip properties marked with @ExcludeFromReviewDialog
        if (prop.findAnnotation<ExcludeFromReviewDialog>() == null) {
            prop.isAccessible = true
            val value = prop.getter.call(item)

            RenderProperty(prop.name, value, indentLevel)
        }
    }
}

@Composable
private fun RenderProperty(key: String, value: Any?, indentLevel: Int = 0) {
    val indent = Modifier.padding(start = (indentLevel * 16).dp)

    when {
        value == null -> {
            DataFieldRow(
                key = key,
                value = "null",
                modifier = indent
            )
        }

        value is Set<*> -> {
            Column(modifier = Modifier.fillMaxWidth().then(indent)) {
                Text(
                    text = key.replaceFirstChar { it.uppercase() } + " (${value.size} items):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                value.forEachIndexed { index, item ->
                    if (item == null) {
                        Text(
                            text = "  ${index}: null",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else if (isDataClass(item::class)) {
                        Text(
                            text = "  ${index}: ${item::class.simpleName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                        RenderObject(item, indentLevel + 2)
                    } else {
                        Text(
                            text = "  ${index}: $item",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        isDataClass(value::class) -> {
            Column(modifier = Modifier.fillMaxWidth().then(indent)) {
                Text(
                    text = key.replaceFirstChar { it.uppercase() } + " (${value::class.simpleName}):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                RenderObject(value, indentLevel + 1)
            }
        }

        else -> {
            DataFieldRow(
                key = key,
                value = value.toString(),
                modifier = indent
            )
        }
    }
}

// Helper function to check if a class is a data class
private fun isDataClass(kClass: KClass<*>): Boolean {
    return kClass.isData
}

@Composable
private fun DataFieldRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
    }
}