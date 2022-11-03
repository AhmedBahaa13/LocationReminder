package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    private val remindersDao: RemindersDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        val reminder = ReminderDTO(
            title = "Test",
            description = "Description",
            location = "Location",
            latitude = 30.085947,
            longitude = 31.223651
        )
        return Result.Success(listOf<ReminderDTO>(reminder))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersDao.saveReminder(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
       return Result.Success(remindersDao.getReminderById(id)!!)
    }

    override suspend fun deleteAllReminders() {
        remindersDao.deleteAllReminders()
    }


}