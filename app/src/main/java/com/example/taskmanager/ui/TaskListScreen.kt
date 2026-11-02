package com.example.taskmanager.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.taskmanager.data.Task
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navigateToTaskEntry: () -> Unit,
    navigateToTaskUpdate: (Int) -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
) {
    val taskList by viewModel.allTasks.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 - wszystkie, 1 - na dziś, 2 - zaległe
    var expandedSort by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("Brak") }

    var quickTaskTitle by remember { mutableStateOf("") }

    val tabs = listOf("Wszystkie", "Na dziś", "Zaległe")
    val sortOptions = listOf("Brak", "Termin rosnąco", "Termin malejąco", "Priorytet")

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = todayStart + 86400000L - 1

    var processedTasks = taskList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }.filter { task ->
        when (selectedTabIndex) {
            1 -> task.executionTime != null && task.executionTime in todayStart..todayEnd
            2 -> task.executionTime != null && task.executionTime < todayStart
            else -> true
        }
    }

    processedTasks = when (sortOption) {
        "Termin rosnąco" -> processedTasks.sortedBy { it.executionTime ?: Long.MAX_VALUE }
        "Termin malejąco" -> processedTasks.sortedByDescending { it.executionTime ?: 0L }
        "Priorytet" -> processedTasks.sortedByDescending {
            when(it.priority) { "Wysoki" -> 3; "Średni" -> 2; "Niski" -> 1; else -> 0 }
        }
        else -> processedTasks
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Zadań") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = navigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ustawienia")
                    }
                    Box {
                        IconButton(onClick = { expandedSort = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Sortowanie")
                        }
                        DropdownMenu(
                            expanded = expandedSort,
                            onDismissRequest = { expandedSort = false }
                        ) {
                            sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        sortOption = option
                                        expandedSort = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = navigateToTaskEntry) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj zadanie")
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quickTaskTitle,
                    onValueChange = { quickTaskTitle = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Szybkie zadanie...") },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (quickTaskTitle.isNotBlank()) {
                            viewModel.insertTask(
                                Task(
                                    title = quickTaskTitle,
                                    description = "",
                                    createdAt = System.currentTimeMillis(),
                                    status = "Do zrobienia",
                                    category = "Ogólne",
                                    priority = "Średni",
                                    locations = emptyList(),
                                    attachmentsPaths = "",
                                    isCyclic = false
                                )
                            )
                            quickTaskTitle = ""
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Zapisz", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Szukaj zadania...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ikona szukaj") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = processedTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onItemClick = { navigateToTaskUpdate(task.id) },
                        onCompleteClick = {
                            viewModel.completeTask(task)
                            if (task.isCyclic) {
                                Toast.makeText(context, "Zadanie odnowione", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Zadanie wykonane", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteClick = {
                            viewModel.deleteTask(task)
                            Toast.makeText(context, "Zadanie usunięte", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
//pojedyncze zadanie
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onItemClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isCompleted = task.status == "Wykonane"

    val dismissState = rememberSwipeToDismissBoxState(//logika do gestoww
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCompleteClick()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteClick()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(horizontal = 16.dp),
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                }
            )
            val alignment = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            val icon = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Check
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                SwipeToDismissBoxValue.Settled -> Icons.Filled.Check
            }
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1.2f
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Akcja gestu",
                        modifier = Modifier.scale(scale),
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isCompleted) 0.5f else 1f)
                    .clickable { onItemClick() },
                elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.attachmentsPaths.isNotBlank()) {
                        val uri = Uri.parse(task.attachmentsPaths)
                        val mimeType = context.contentResolver.getType(uri)
                        val isImage = mimeType?.startsWith("image") == true

                        if (isImage) {
                            AsyncImage(
                                model = task.attachmentsPaths,
                                contentDescription = "Miniatura zdjęcia",
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(end = 12.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Załącznik (inny plik)",
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(end = 12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        )

                        if (task.executionTime != null) {
                            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            val isOverdue = task.executionTime < System.currentTimeMillis() - 86400000 && !isCompleted
                            Text(
                                text = "Termin: ${formatter.format(java.util.Date(task.executionTime))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = task.priority,
                                style = MaterialTheme.typography.labelMedium,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                            )
                            if (task.locationNotification) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Posiada lokalizację",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (task.isCyclic) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Zadanie cykliczne",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}