package com.example.taskmanager.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasksStream(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTaskStream(id: Int): Flow<Task> = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun countTasksWithAttachment(path: String): Int {
        return taskDao.countTasksWithAttachment(path)
    }
}