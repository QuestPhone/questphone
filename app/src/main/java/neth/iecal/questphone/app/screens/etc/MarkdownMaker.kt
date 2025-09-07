@file:OptIn(ExperimentalFoundationApi::class)
package neth.iecal.questphone.app.screens.etc
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.firstUriOrNull
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import neth.iecal.questphone.app.theme.smoothYellow
import neth.iecal.questphone.data.QuestInfoState
import nethical.questphone.data.game.InventoryItem
import java.util.UUID

fun parseMarkdown(markdown: String): List<MdComponent> {
    val components = mutableListOf<MdComponent>()
    val lines = markdown.lines()
    var i = 0
    var inCodeBlock = false
    val codeBuffer = StringBuilder()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuffer.isNotBlank()) {
            components.add(MdComponent(type = ComponentType.TEXT, content = paragraphBuffer.toString().trim()))
            paragraphBuffer.clear()
        }
    }

    fun splitPipeLine(line: String): List<String> {
        var s = line.trim()
        if (s.startsWith("|")) s = s.drop(1)
        if (s.endsWith("|")) s = s.dropLast(1)
        return s.split("|").map { it.trim() }
    }

    fun isSeparatorLine(line: String): Boolean {
        if (!line.contains("|")) return false
        return line.split("|").all { part ->
            val p = part.trim()
            // matches ---, :---, ---:, :---:
            p.matches(Regex("^:?-+:?$"))
        }
    }

    while (i < lines.size) {
        val line = lines[i]

        // fenced code blocks (```)
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                // end code block
                components.add(MdComponent(type = ComponentType.CODE, content = codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
            } else {
                // starting a code block -> flush any pending paragraph
                flushParagraph()
            }
            inCodeBlock = !inCodeBlock
            i++
            continue
        }

        if (inCodeBlock) {
            codeBuffer.appendLine(line)
            i++
            continue
        }

        // Table detection: header line contains '|' and next line is a separator line
        if (line.contains("|") && i + 1 < lines.size && isSeparatorLine(lines[i + 1])) {
            // flush any paragraph text we've been aggregating
            flushParagraph()

            // parse header
            val headers = splitPipeLine(line)

            // collect rows after the separator
            val rows = mutableListOf<List<String>>()
            var j = i + 2
            while (j < lines.size) {
                val r = lines[j]
                if (r.trim().isEmpty() || !r.contains("|")) break
                val rowParts = splitPipeLine(r)
                val fixedRow = if (rowParts.size < headers.size) {
                    rowParts + List(headers.size - rowParts.size) { "" }
                } else {
                    rowParts.take(headers.size)
                }
                rows.add(fixedRow)
                j++
            }

            // create TableData and component
            val tableData = TableData(headers = headers, rows = rows)
            components.add(MdComponent(type = ComponentType.TABLE, content = json.encodeToString(tableData)))

            // advance index to the line after the last table row
            i = j
            continue
        }

        // blank line -> flush paragraph
        if (line.trim().isEmpty()) {
            flushParagraph()
            i++
            continue
        }

        // Header #, e.g. ## Title
        if (line.trimStart().startsWith("#")) {
            flushParagraph()
            val level = line.takeWhile { it == '#' }.length
            val text = line.drop(level).trim()
            components.add(MdComponent(type = ComponentType.HEADER, content = text, level = level))
            i++
            continue
        }

        // Checkbox - [ ] or - [x]
        val trimmed = line.trimStart()
        if (trimmed.startsWith("- [") && (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]", true))) {
            flushParagraph()
            val checked = trimmed.substring(3, 4).equals("x", true)
            val task = trimmed.substringAfter("]").trim()
            components.add(MdComponent(type = ComponentType.CHECKBOX, content = (if (checked) "done|" else "todo|") + task))
            i++
            continue
        }

        // List item - or *
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            flushParagraph()
            val text = trimmed.substring(2).trim()
            components.add(MdComponent(type = ComponentType.LIST, content = text))
            i++
            continue
        }

        // Quote
        if (trimmed.startsWith(">")) {
            flushParagraph()
            val text = trimmed.substring(1).trim()
            components.add(MdComponent(type = ComponentType.QUOTE, content = text))
            i++
            continue
        }

        // Image ![alt](url)
        if (trimmed.startsWith("![")) {
            flushParagraph()
            val alt = trimmed.substringAfter("![").substringBefore("]")
            val url = trimmed.substringAfter("(").substringBefore(")")
            components.add(MdComponent(type = ComponentType.IMAGE, content = "$alt|$url"))
            i++
            continue
        }

        // Link [text](url)
        if (trimmed.startsWith("[")) {
            flushParagraph()
            val text = trimmed.substringAfter("[").substringBefore("]")
            val url = trimmed.substringAfter("(").substringBefore(")")
            components.add(MdComponent(type = ComponentType.LINK, content = "$text|$url"))
            i++
            continue
        }

        // default: aggregate into a paragraph (so multiple lines become one TEXT component)
        if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append("\n")
        paragraphBuffer.append(line)
        i++
    }

    // flush any remaining buffers
    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        components.add(MdComponent(type = ComponentType.CODE, content = codeBuffer.toString().trimEnd()))
    }
    flushParagraph()
    return components
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
// Data classes for components
data class MdComponent(
    val id: String = UUID.randomUUID().toString(),
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
    LINK("Link", Icons.Default.Build),
    IMAGE("Image", Icons.Default.Build),
    TABLE("Table", Icons.Default.Build),
    CHECKBOX("Checkbox", Icons.Default.Build)
}
@Serializable
data class TableData(
    val headers: List<String>,
    val rows: List<List<String>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownComposer(list: List<MdComponent> = listOf(), generatedMarkdown : QuestInfoState = QuestInfoState()) {
    var components by remember { mutableStateOf(list) }
    var showPreview by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(components) {
        generatedMarkdown.instructions = generateMarkdown(components)
    }
    Scaffold (topBar = {
        TopAppBar(

            title = {
            },
            actions = {
                IconButton(onClick = { showPreview = !showPreview }) {
                    Icon(
                        if (showPreview) Icons.Default.Edit else Icons.Default.PlayArrow,
                        contentDescription = if (showPreview) "Edit" else "Preview",
                    )
                }

            }
        )

    }){padding->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar

            if (showPreview) {
                PreviewScreen(components)
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    ComponentPalette(
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight(),
                        onComponentDrop = { componentType ->
                            components = components + MdComponent(type = componentType)
                        }
                    )

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

        }
    }
}
@Composable
fun ComponentPalette(
    modifier: Modifier = Modifier,
    onComponentDrop: (ComponentType) -> Unit
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            ComponentType.entries.forEach { componentType ->
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
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                componentType.icon,
                contentDescription = componentType.displayName,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                componentType.displayName,
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
    Box(
        modifier = modifier,
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
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Drag components here to start",
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
                itemsIndexed(
                    items = components,
                    key = { _, component -> component.id } // Make sure each item has a stable unique key
                ) { index, component ->
                    ComponentEditor(
                        component = component,
                        onUpdate = { updatedComponent ->
                            onComponentUpdate(index, updatedComponent)
                        },
                        onDelete = { onComponentDelete(index) },
                        onMoveUp = if (index > 0) {
                            { onComponentMove(index, index - 1) }
                        } else null,
                        onMoveDown = if (index < components.size - 1) {
                            { onComponentMove(index, index + 1) }
                        } else null,
                        modifier = Modifier.animateItem(
                            placementSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            fadeInSpec = tween(180),
                            fadeOutSpec = tween(180)
                        )
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ComponentEditor(
    component: MdComponent,
    onUpdate: (MdComponent) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        component.type.displayName,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row {
                    onMoveUp?.let {
                        IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move up",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    onMoveDown?.let {
                        IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move down",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Component-specific controls
            when (component.type) {
                ComponentType.TABLE -> {
                    // Parse table data from JSON or initialize empty table
                    val tableData = remember(component.content) {
                        try {
                            if (component.content.isNotEmpty()) {
                                json.decodeFromString<TableData>(component.content).let { data ->
                                    // Validate that all rows match header size
                                    if (data.rows.all { it.size == data.headers.size }) {
                                        data
                                    } else {
                                        // Fix mismatched rows by padding or truncating
                                        val fixedRows = data.rows.map { row ->
                                            when {
                                                row.size < data.headers.size -> row + List(data.headers.size - row.size) { "" }
                                                row.size > data.headers.size -> row.take(data.headers.size)
                                                else -> row
                                            }
                                        }
                                        TableData(data.headers, fixedRows)
                                    }
                                }
                            } else {
                                TableData(
                                    headers = listOf("Column 1", "Column 2"),
                                    rows = listOf(listOf("Cell 1", "Cell 2"))
                                )
                            }
                        } catch (e: Exception) {
                            println("Deserialization error: ${e.message}")
                            TableData(
                                headers = listOf("Column 1", "Column 2"),
                                rows = listOf(listOf("Cell 1", "Cell 2"))
                            )
                        }
                    }

                    // State for table data
                    var headers by remember { mutableStateOf(tableData.headers) }
                    var rows by remember { mutableStateOf(tableData.rows) }

                    // Update component when table data changes
                    fun updateTableData(newHeaders: List<String>, newRows: List<List<String>>) {
                        headers = newHeaders
                        rows = newRows
                        try {
                            val newTableData = TableData(newHeaders, newRows)
                            onUpdate(component.copy(content = json.encodeToString(newTableData)))
                        } catch (e: Exception) {
                            println("Serialization error: ${e.message}")
                        }
                    }

                    Column(
                    ) {
                        // Controls for adding rows/columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                updateTableData(
                                    headers + "Column ${headers.size + 1}",
                                    rows.map { it + "" }
                                )
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Column")
                            }
                            IconButton(onClick = {
                                val newlist =  rows.toMutableList()
                                newlist.add(List(headers.size) {""})
                                updateTableData(
                                    headers,
                                    newlist
                                )
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Row")
                            }
                        }

                        // Table grid
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp), // Adjust height as needed
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Header row
                            item {
                                // Use Box to constrain LazyRow width
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                ) {
                                    LazyRow(
                                        modifier = Modifier
                                            .widthIn(max = 1000.dp) // Set a reasonable max width
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        itemsIndexed(headers) { colIndex, header ->
                                            OutlinedTextField(
                                                value = header,
                                                onValueChange = { newValue ->
                                                    val newHeaders = headers.toMutableList().apply {
                                                        set(colIndex, newValue)
                                                    }
                                                    updateTableData(newHeaders, rows)
                                                },
                                                modifier = Modifier
                                                    .width(150.dp) // Fixed width for consistency
                                                    .padding(vertical = 4.dp),
                                                label = { Text("Header $colIndex") }
                                            )
                                        }
                                        // Delete column button
                                        if (headers.size > 1) {
                                            item {
                                                IconButton(onClick = {
                                                    updateTableData(
                                                        headers.dropLast(1),
                                                        rows.map { it.dropLast(1) }
                                                    )
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Column")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Body rows
                            itemsIndexed(rows) { rowIndex, row ->
                                // Use Box to constrain LazyRow width
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                ) {
                                    LazyRow(
                                        modifier = Modifier
                                            .widthIn(max = 1000.dp) // Set a reasonable max width
                                            .wrapContentHeight(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        itemsIndexed(row) { colIndex, cell ->
                                            OutlinedTextField(
                                                value = cell,
                                                onValueChange = { newValue ->
                                                    val newRows = rows.toMutableList().apply {
                                                        set(rowIndex, row.toMutableList().apply {
                                                            set(colIndex, newValue)
                                                        })
                                                    }
                                                    updateTableData(headers, newRows)
                                                },
                                                modifier = Modifier
                                                    .width(150.dp) // Fixed width for consistency
                                                    .padding(vertical = 4.dp),
                                                label = { Text("Cell") }
                                            )
                                        }
                                        // Delete row button
                                        if (rows.size > 1) {
                                            item {
                                                IconButton(onClick = {
                                                    val newRows = rows.toMutableList().apply {
                                                        removeAt(rowIndex)
                                                    }
                                                    updateTableData(headers, newRows)
                                                }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Row"
                                                    )
                                                }
                                            }
                                        }
                                        }

                                }
                            }
                        }

                        // Optional raw input
                        var showRawInput by remember { mutableStateOf(false) }
                        FilterChip(
                            selected = showRawInput,
                            onClick = { showRawInput = !showRawInput },
                            label = { Text("Edit Raw") }
                        )
                        if (showRawInput) {
                            OutlinedTextField(
                                value = component.content,
                                onValueChange = { newValue ->
                                    try {
                                        val newTableData = json.decodeFromString<TableData>(newValue)
                                        headers = newTableData.headers
                                        rows = newTableData.rows
                                        onUpdate(component.copy(content = newValue))
                                    } catch (e: Exception) {
                                        println("Raw input error: ${e.message}")
                                        // Optionally show a Toast or error message to the user
                                    }
                                },
                                label = { Text("Raw Table Data (JSON)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }
                }
                ComponentType.HEADER -> {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text("Level:")
                            Spacer(modifier = Modifier.width(8.dp))
                            (1..6).forEach { level ->
                                FilterChip(
                                    onClick = {
                                        onUpdate(component.copy(level = level))
                                    },
                                    label = { Text("H$level") },
                                    selected = component.level == level,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = component.content,
                        onValueChange = { onUpdate(component.copy(content = it)) },
                        label = { Text("Header text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = component.content,
                        onValueChange = { onUpdate(component.copy(content = it)) },
                        label = {
                            Text(
                                when (component.type) {
                                    ComponentType.TEXT -> "Paragraph text"
                                    ComponentType.LIST -> "List item text"
                                    ComponentType.CODE -> "Code content"
                                    ComponentType.QUOTE -> "Quote text"
                                    ComponentType.LINK -> "Link text|URL (separated by |)"
                                    ComponentType.IMAGE -> "Alt text|Image URL"
                                    ComponentType.CHECKBOX -> "done|Task text OR todo|Task text"
                                    else -> "Content"
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentReceiver(
                                object : ReceiveContentListener {
                                    override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
                                        val entry = transferableContent.clipEntry
                                        val uri = entry.firstUriOrNull()
                                        if (uri != null) {
                                            component.content += uri.path
                                        }
                                        return transferableContent
                                    }

                                }),
                        keyboardActions = KeyboardActions(),
                        minLines = if (component.type == ComponentType.CODE || component.type == ComponentType.QUOTE) 3 else 1,
                        maxLines = if (component.type == ComponentType.LINK) 1 else Int.MAX_VALUE
                    )
                }
            }

            // Preview of how it will look in markdown
            if (component.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Preview: ${generateComponentMarkdown(component)}",
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}




@Composable
fun PreviewScreen(components: List<MdComponent>) {
    val markdown = generateMarkdown(components)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
        ) {
            Text(
                text = "Quest Title",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text ="Reward: 40 coins + 32 xp",
                    style = MaterialTheme.typography.bodyLarge
                )
                    Text(
                        text = " + ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = smoothYellow
                    )
                    Image(painter = painterResource( InventoryItem.XP_BOOSTER.icon),
                        contentDescription = InventoryItem.XP_BOOSTER.simpleName,
                        Modifier.size(20.dp))
                    Text(
                        text = " 69 xp",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = smoothYellow
                    )
            }

            Text(
                text = "Time: 01:00 to 02:00",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.size(32.dp))
            LazyColumn {
                item {
                    SelectionContainer {
                        MarkdownText(
                            markdown = markdown,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

fun generateComponentMarkdown(component: MdComponent): String {
    return when (component.type) {
        ComponentType.TABLE -> {
            try {
                val tableData = json.decodeFromString<TableData>(component.content)
                val header = tableData.headers.joinToString("|")
                val separator = tableData.headers.joinToString("|") { "---" }
                val body = tableData.rows.joinToString("\n") { it.joinToString("|") }
                "$header\n$separator\n$body"
            } catch (e: Exception) {
                println("Markdown generation error: ${e.message}")
                "" // Return empty string if parsing fails
            }
        }
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
        ComponentType.IMAGE -> {
            val parts = component.content.split("|")
            if (parts.size == 2) {
                "![${parts[0].trim()}](${parts[1].trim()})"
            } else component.content
        }
        ComponentType.CHECKBOX -> {
            val parts = component.content.split("|", limit = 2)
            if (parts.size == 2) {
                val checked = if (parts[0].trim().equals("done", true)) "x" else " "
                "- [$checked] ${parts[1].trim()}"
            } else "- [ ] ${component.content}"
        }
    }
}
fun generateMarkdown(components: List<MdComponent>): String {
    return components.joinToString("\n\n") { generateComponentMarkdown(it) }
}