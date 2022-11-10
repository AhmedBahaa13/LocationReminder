package com.udacity.project4.locationreminders.data.local

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.Result
import org.hamcrest.Matchers.`is`
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.isNotNull

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).build()
        repository = RemindersLocalRepository(database.reminderDao())
    }
    @After
    fun clearDatabase(){
        database.close()
    }

    @Test
    fun getReminderList_emptyList() = runBlocking {
        val reminders = repository.getReminders()
        assertThat(reminders is Result.Success, `is`(true))
        reminders as Result.Success
        assertThat(reminders.data.size, `is`(0))
    }

    @Test
    fun saveReminder() = runBlocking{
       val reminder = ReminderDataItem("title","description","location").asDTO()
        repository.saveReminder(reminder)
        val returnValue = repository.getReminder(reminder.id)
        assertThat(returnValue is Result.Success, `is`(true))
        returnValue as Result.Success
        assertThat(returnValue.data.title, `is`(reminder.title))
        assertThat(returnValue.data.description , `is`(reminder.description))
        assertThat(returnValue.data.location, `is`(reminder.location))
    }
}
