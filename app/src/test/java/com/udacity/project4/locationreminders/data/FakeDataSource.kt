package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.Exception

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource() : ReminderDataSource {
    private val remindersProvider = LinkedHashMap<String,ReminderDTO>()
    private var returnError = false

    fun setReturnError(isError: Boolean) {
        returnError = isError
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return try {
            if (returnError){
                throw Exception("data can't be retrieved!!")
            }else{
                 Result.Success(remindersProvider.values.toList())
            }
        }catch (e:Exception){
            Result.Error(e.localizedMessage)
        }

    }


    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return try {
            if (returnError){
                throw Exception("data can't be retrieved!!")
            }else{
                val value = remindersProvider[id]
                if (value == null) {
                    throw Exception("Test Exception!!")
                } else {
                    Result.Success(value)
                }
            }
        }catch (e:Exception){
            Result.Error(e.localizedMessage)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersProvider[reminder.id] = reminder
    }

    override suspend fun deleteAllReminders() {
        remindersProvider.clear()
    }


}