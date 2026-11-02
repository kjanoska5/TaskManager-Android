package com.example.taskmanager.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskmanager.data.TaskLocation
import com.example.taskmanager.services.AlarmScheduler
import com.example.taskmanager.services.LocationNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun TaskUpdateScreen(
    taskId: Int,
    navigateBack: () -> Unit,
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
) {
    val task by viewModel.getTask(taskId).collectAsState(initial = null)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var priority by remember { mutableStateOf("Średni") }
    var priorityExpanded by remember { mutableStateOf(false) }
    val priorities = listOf("Niski", "Średni", "Wysoki")

    var category by remember { mutableStateOf("Ogólne") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Ogólne", "Praca", "Dom", "Zakupy", "Osobiste")

    var isCyclic by remember { mutableStateOf(false) }
    var cycleType by remember { mutableStateOf("Codziennie") }
    var cycleExpanded by remember { mutableStateOf(false) }
    var customCycleDays by remember { mutableStateOf("") }
    val cycleTypes = listOf("Codziennie", "Tygodniowo", "Miesięcznie", "Niestandardowo")

    var taskLocations by remember { mutableStateOf(listOf<TaskLocation>()) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var addressInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    var attachmentUri by remember { mutableStateOf<Uri?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateTimeMillis by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState()

    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val locationService = remember { LocationNotificationService(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }

    LaunchedEffect(task) {
        task?.let {
            title = it.title
            description = it.description
            priority = it.priority
            category = it.category
            isCyclic = it.isCyclic
            taskLocations = it.locations
            if (it.attachmentsPaths.isNotBlank()) attachmentUri = Uri.parse(it.attachmentsPaths)

            if (it.executionTime != null) {
                datePickerState.selectedDateMillis = it.executionTime
                selectedDateTimeMillis = it.executionTime
            }

            if (it.cycleType?.startsWith("Niestandardowo:") == true) {
                cycleType = "Niestandardowo"
                customCycleDays = it.cycleType.substringAfter("Niestandardowo:").trim()
            } else if (it.cycleType != null) {
                cycleType = it.cycleType
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showLocationDialog = true
        else Toast.makeText(context, "Musisz nadać uprawnienia", Toast.LENGTH_SHORT).show()
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachmentUri = it
        }
    }

    if (task == null) return

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edytuj Zadanie") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tytuł") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Opis") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedDateTimeMillis != null)
                        "Termin: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(selectedDateTimeMillis!!))}"
                    else "Wybierz termin wykonania"
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            showTimePicker = true
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") } }
                ) { DatePicker(state = datePickerState) }
            }

            if (showTimePicker) {
                val calendar = Calendar.getInstance()
                LaunchedEffect(Unit) {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            val calDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calDate.timeInMillis = dateMillis

                            val finalCal = Calendar.getInstance()
                            finalCal.set(Calendar.YEAR, calDate.get(Calendar.YEAR))
                            finalCal.set(Calendar.MONTH, calDate.get(Calendar.MONTH))
                            finalCal.set(Calendar.DAY_OF_MONTH, calDate.get(Calendar.DAY_OF_MONTH))
                            finalCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            finalCal.set(Calendar.MINUTE, minute)
                            finalCal.set(Calendar.SECOND, 0)

                            selectedDateTimeMillis = finalCal.timeInMillis
                            showTimePicker = false
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).apply {
                        setOnDismissListener { showTimePicker = false }
                        show()
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Kategoria") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { category = selectionOption; categoryExpanded = false }) }
                }
            }

            ExposedDropdownMenuBox(expanded = priorityExpanded, onExpandedChange = { priorityExpanded = it }) {
                OutlinedTextField(value = priority, onValueChange = {}, readOnly = true, label = { Text("Priorytet") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                    priorities.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { priority = selectionOption; priorityExpanded = false }) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCyclic, onCheckedChange = { isCyclic = it })
                Text("Zadanie cykliczne")
            }

            if (isCyclic) {
                ExposedDropdownMenuBox(expanded = cycleExpanded, onExpandedChange = { cycleExpanded = it }) {
                    OutlinedTextField(value = cycleType, onValueChange = {}, readOnly = true, label = { Text("Częstotliwość") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = cycleExpanded, onDismissRequest = { cycleExpanded = false }) {
                        cycleTypes.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { cycleType = selectionOption; cycleExpanded = false }) }
                    }
                }
                if (cycleType == "Niestandardowo") {
                    OutlinedTextField(value = customCycleDays, onValueChange = { customCycleDays = it }, label = { Text("Liczba dni") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = { pickMediaLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (attachmentUri == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary)
            ) { Text(if (attachmentUri == null) "Zmień załącznik" else "Załącznik ustawiony") }

            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium).padding(8.dp)) {
                taskLocations.forEach { loc ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            Text(loc.addressName, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { taskLocations = taskLocations.filter { it != loc } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Button(
                    onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Dodaj nową lokalizację") }
            }

            if (showLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationDialog = false },
                    title = { Text("Szukaj Lokalizacji") },
                    text = { OutlinedTextField(value = addressInput, onValueChange = { addressInput = it }, label = { Text("Wpisz adres") }) },
                    confirmButton = {
                        Button(onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val addresses = geocoder.getFromLocationName(addressInput, 1)
                                    withContext(Dispatchers.Main) {
                                        if (!addresses.isNullOrEmpty()) {
                                            val loc = addresses[0]
                                            taskLocations = taskLocations + TaskLocation(loc.latitude, loc.longitude, addressInput)
                                            showLocationDialog = false
                                            addressInput = ""
                                        } else {
                                            Toast.makeText(context, "Nie znaleziono adresu", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Błąd wyszukiwania", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }) { Text("Dodaj") }
                    },
                    dismissButton = { TextButton(onClick = { showLocationDialog = false }) { Text("Anuluj") } }
                )
            }

            if (taskLocations.isNotEmpty()) {
                taskLocations.forEach { location ->
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try { context.startActivity(mapIntent) } catch (e: android.content.ActivityNotFoundException) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}"))
                                context.startActivity(browserIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Nawiguj do: ${location.addressName}")
                    }
                }
            }

            Button(
                onClick = {
                    val finalCycleType = if (isCyclic) { if (cycleType == "Niestandardowo") "Niestandardowo: $customCycleDays" else cycleType } else null

                    val finalAttachmentPath = attachmentUri?.let { uri ->
                        if (uri.toString().startsWith("/")) {
                            uri.toString()
                        } else {
                            viewModel.copyFileToInternalStorage(context, uri) ?: ""
                        }
                    } ?: ""

                    val updatedTask = task!!.copy(
                        title = title,
                        description = description,
                        executionTime = selectedDateTimeMillis,
                        timeNotification = selectedDateTimeMillis,
                        locationNotification = taskLocations.isNotEmpty(),
                        category = category,
                        priority = priority,
                        locations = taskLocations,
                        attachmentsPaths = finalAttachmentPath,
                        isCyclic = isCyclic,
                        cycleType = finalCycleType
                    )

                    viewModel.updateTask(updatedTask)

                    taskLocations.forEachIndexed { index, loc ->
                        locationService.addGeofence("${task!!.id}_$index", loc.latitude, loc.longitude)
                    }

                    if (selectedDateTimeMillis != null) {
                        alarmScheduler.scheduleAlarm(task!!.id, title, selectedDateTimeMillis!!)
                    }

                    navigateBack()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = title.isNotBlank() && (!isCyclic || cycleType != "Niestandardowo" || customCycleDays.isNotBlank())
            ) { Text("Aktualizuj Zadanie") }
        }
    }
}