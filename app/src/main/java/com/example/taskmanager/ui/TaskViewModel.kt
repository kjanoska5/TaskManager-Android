package com.example.taskmanager.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.taskmanager.TaskManagerApplication
import com.example.taskmanager.data.Task
import com.example.taskmanager.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: StateFlow<List<Task>> = repository.getAllTasksStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
    }


    fun deleteTask(task: Task) {
        viewModelScope.launch {
            val path = task.attachmentsPaths
            if (path.isNotBlank()) {
                val usagesCount = repository.countTasksWithAttachment(path)

                if (usagesCount <= 1) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
            repository.deleteTask(task)
        }
    }

    fun copyFileToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val fileName = "attachment_${uri.toString().hashCode()}.file"

            val internalFile = File(context.filesDir, fileName)

            if (internalFile.exists()) {
                return internalFile.absolutePath
            }

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(internalFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            internalFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTask(id: Int): Flow<Task> = repository.getTaskStream(id)

    fun completeTask(task: Task) = viewModelScope.launch {
        val completedTask = task.copy(status = "Wykonane")
        repository.updateTask(completedTask)

        if (task.isCyclic && task.executionTime != null) {
            val newExecutionTime = calculateNextCycleTime(task.executionTime, task.cycleType)

            val nextTask = task.copy(
                id = 0,
                executionTime = newExecutionTime,
                timeNotification = newExecutionTime,
                status = "Do zrobienia",
                createdAt = System.currentTimeMillis()
            )
            repository.insertTask(nextTask)
        }
    }

    private fun calculateNextCycleTime(currentTime: Long, cycleType: String?): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        when {
            cycleType == "Codziennie" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            cycleType == "Tygodniowo" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            cycleType == "Miesięcznie" -> calendar.add(Calendar.MONTH, 1)
            cycleType?.startsWith("Niestandardowo:") == true -> {
                val daysString = cycleType.substringAfter("Niestandardowo:").trim()
                val daysToAdd = daysString.toIntOrNull() ?: 1
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
            }
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TaskManagerApplication
                return TaskViewModel(application.repository) as T
            }
        }
    }
}