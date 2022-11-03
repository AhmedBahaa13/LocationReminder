package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import junit.framework.Assert.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    private val fakeReminder = ReminderDTO(
        "title",
        "description",
        "location",
        (-360..360).random().toDouble(),
        (-360..360).random().toDouble()
    )
    private val fakeRemindersList = listOf<ReminderDTO>(
        fakeReminder,
        fakeReminder,
        fakeReminder,
        fakeReminder
    )

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()


    @Test
    fun getReminders() = runBlockingTest {
        // GIVEN - insert a reminder
        database.reminderDao().saveReminder(fakeReminder)

        // WHEN - Get reminders from the database
        val reminders = database.reminderDao().getReminders()

        // THEN - Check is There only 1 reminder in the database
        // And it is the same one we had given
        assertThat(reminders.size, `is`(1))
        assertThat(reminders[0].id, `is`(fakeReminder.id))
        assertThat(reminders[0].title, `is`(fakeReminder.title))
        assertThat(reminders[0].description, `is`(fakeReminder.description))
        assertThat(reminders[0].location, `is`(fakeReminder.location))
        assertThat(reminders[0].latitude, `is`(fakeReminder.latitude))
        assertThat(reminders[0].longitude, `is`(fakeReminder.longitude))
    }


    @Test
    fun insertReminder_GetById() = runBlockingTest {
        // GIVEN - Insert a reminder.
        database.reminderDao().saveReminder(fakeReminder)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(fakeReminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(fakeReminder.id))
        assertThat(loaded.title, `is`(fakeReminder.title))
        assertThat(loaded.description, `is`(fakeReminder.description))
        assertThat(loaded.location, `is`(fakeReminder.location))
        assertThat(loaded.latitude, `is`(fakeReminder.latitude))
        assertThat(loaded.longitude, `is`(fakeReminder.longitude))
    }

    @Test
    fun getReminderByIdNotFound() = runBlockingTest {
        // GIVEN - a random reminder id
        val reminderId = UUID.randomUUID().toString()
        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(reminderId)
        // THEN - The loaded data should be  null.
        assertNull(loaded)
    }


    @Test
    fun deleteReminders() = runBlockingTest {
        // Given - reminders inserted
        fakeRemindersList.forEach {
            database.reminderDao().saveReminder(it)
        }

        // WHEN - deleting all reminders
        database.reminderDao().deleteAllReminders()

        // THEN - The list is empty
        val reminders = database.reminderDao().getReminders()
        assertThat(reminders.isEmpty(), `is`(true))
    }
}