@file:OptIn(ExperimentalFoundationApi::class)
package neth.iecal.questphone.app.screens.etc
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.io.File
import java.io.FileWriter

// Data classes for components
data class MdComponent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ComponentType,
    var content: String = "",
    var level: Int = 1 // for headers
)

enum class ComponentType(val displayName: String, val icon: ImageVector) {
    HEADER("Header", Icons.Default.Build),
    TEXT("Text", Icons.Default.Build),
    LIST("List Item", Icons.Default.Build),
    CODE("Code Block", Icons.Default.Build),
    QUOTE("Quote", Icons.Default.Build),
    LINK("Link", Icons.Default.Build)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownComposer() {
    var components by remember { mutableStateOf(listOf<MdComponent>()) }
    var showPreview by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("document") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    "MD Composer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF313244)
            ),
            actions = {
                IconButton(onClick = { showPreview = !showPreview }) {
                    Icon(
                        if (showPreview) Icons.Default.Edit else Icons.Default.Build,
                        contentDescription = if (showPreview) "Edit" else "Preview",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    saveMarkdownFile(context, fileName, generateMarkdown(components))
                }) {
                    Icon(Icons.Default.Build, contentDescription = "Save", tint = Color.White)
                }
            }
        )

        if (showPreview) {
            // Preview Mode
            PreviewScreen(components)
        } else {
            // Edit Mode
            Row(modifier = Modifier.fillMaxSize()) {
                // Component Palette (Left Panel)
                ComponentPalette(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight(),
                    onComponentDrop = { componentType ->
                        components = components + MdComponent(type = componentType)
                    }
                )

                // Main Canvas (Right Panel)
                MainCanvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    components = components,
                    onComponentUpdate = { index, newComponent ->
                        components = components.toMutableList().apply {
                            this[index] = newComponent
                        }
                    },
                    onComponentDelete = { index ->
                        components = components.toMutableList().apply {
                            removeAt(index)
                        }
                    },
                    onComponentMove = { from, to ->
                        components = components.toMutableList().apply {
                            add(to, removeAt(from))
                        }
                    }
                )
            }
        }

        // File name input at bottom
        if (!showPreview) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF313244))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("File name:", color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF89B4FA),
                            unfocusedBorderColor = Color(0xFF6C7086)
                        ),
                        singleLine = true
                    )
                    Text(".md", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ComponentPalette(
    modifier: Modifier = Modifier,
    onComponentDrop: (ComponentType) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF313244))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                "Components",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ComponentType.values().forEach { componentType ->
                DraggableComponent(
                    componentType = componentType,
                    onDrop = onComponentDrop
                )
            }
        }
    }
}

@Composable
fun DraggableComponent(
    componentType: ComponentType,
    onDrop: (ComponentType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onDrop(componentType) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF45475A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                componentType.icon,
                contentDescription = componentType.displayName,
                tint = Color(0xFF89B4FA),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                componentType.displayName,
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun MainCanvas(
    modifier: Modifier = Modifier,
    components: List<MdComponent>,
    onComponentUpdate: (Int, MdComponent) -> Unit,
    onComponentDelete: (Int) -> Unit,
    onComponentMove: (Int, Int) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
    ) {
        if (components.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add components",
                        tint = Color(0xFF6C7086),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Drag components here to start",
                        color = Color(0xFF6C7086),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(components) { index, component ->
                    ComponentEditor(
                        component = component,
                        onUpdate = { updatedComponent ->
                            onComponentUpdate(index, updatedComponent)
                        },
                        onDelete = { onComponentDelete(index) },
                        onMoveUp = if (index > 0) { { onComponentMove(index, index - 1) } } else null,
                        onMoveDown = if (index < components.size - 1) { { onComponentMove(index, index + 1) } } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentEditor(
    component: MdComponent,
    onUpdate: (MdComponent) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF313244))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with type and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        component.type.icon,
                        contentDescription = component.type.displayName,
                        tint = Color(0xFF89B4FA),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        component.type.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row {
                    onMoveUp?.let {
                        IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move up",
                                tint = Color(0xFF89B4FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    onMoveDown?.let {
                        IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move down",
                                tint = Color(0xFF89B4FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF38BA8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Component-specific controls
            when (component.type) {
                ComponentType.HEADER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Level:", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        (1..6).forEach { level ->
                            FilterChip(
                                onClick = {
                                    onUpdate(component.copy(level = level))
                                },
                                label = { Text("H$level") },
                                selected = component.level == level,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF89B4FA),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF45475A),
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {}
            }

            // Content input
            OutlinedTextField(
                value = component.content,
                onValueChange = { onUpdate(component.copy(content = it)) },
                label = {
                    Text(
                        when (component.type) {
                            ComponentType.HEADER -> "Header text"
                            ComponentType.TEXT -> "Paragraph text"
                            ComponentType.LIST -> "List item text"
                            ComponentType.CODE -> "Code content"
                            ComponentType.QUOTE -> "Quote text"
                            ComponentType.LINK -> "Link text|URL (separated by |)"
                        },
                        color = Color(0xFF6C7086)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF89B4FA),
                    unfocusedBorderColor = Color(0xFF6C7086)
                ),
                minLines = if (component.type == ComponentType.CODE || component.type == ComponentType.QUOTE) 3 else 1,
                maxLines = if (component.type == ComponentType.LINK) 1 else Int.MAX_VALUE
            )

            // Preview of how it will look in markdown
            if (component.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Preview: ${generateComponentMarkdown(component)}",
                    color = Color(0xFF6C7086),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF181825),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun PreviewScreen(components: List<MdComponent>) {
    val markdown = generateMarkdown(components)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF313244))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Markdown Preview",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                item {
                    SelectionContainer {
                        MarkdownText(
                            markdown = markdown,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF181825),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

fun generateComponentMarkdown(component: MdComponent): String {
    return when (component.type) {
        ComponentType.HEADER -> "${"#".repeat(component.level)} ${component.content}"
        ComponentType.TEXT -> component.content
        ComponentType.LIST -> "- ${component.content}"
        ComponentType.CODE -> "```\n${component.content}\n```"
        ComponentType.QUOTE -> "> ${component.content}"
        ComponentType.LINK -> {
            val parts = component.content.split("|")
            if (parts.size == 2) {
                "[${parts[0].trim()}](${parts[1].trim()})"
            } else {
                component.content
            }
        }
    }
}

fun generateMarkdown(components: List<MdComponent>): String {
    return components.joinToString("\n\n") { generateComponentMarkdown(it) }
}

fun saveMarkdownFile(context: android.content.Context, fileName: String, content: String) {
    try {
        val file = File(context.getExternalFilesDir(null), "$fileName.md")
        FileWriter(file).use { writer ->
            writer.write(content)
        }
        // You can add a toast notification here
        println("File saved: ${file.absolutePath}")
    } catch (e: Exception) {
        println("Error saving file: ${e.message}")
    }
}

