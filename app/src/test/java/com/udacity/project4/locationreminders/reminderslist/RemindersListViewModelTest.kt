package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutinesRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    private lateinit var viewModel: RemindersListViewModel
    private lateinit var dataSource: ReminderDataSource
    private lateinit var database: RemindersDatabase
    val reminder = ReminderDTO(
        title = "Test",
        description = "Description",
        location = "Location",
        latitude = 30.085947,
        longitude = 31.223651
    )
    //TODO: provide testing to the RemindersListViewModel and its live data objects
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val mainCoroutinesRule = MainCoroutinesRule()

    @Before
    fun setupTest() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dataSource = FakeDataSource(database.reminderDao())
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(),dataSource)
    }

    @After
    fun cleanTest() {
        database.close()
        stopKoin()
    }

    @Test
    fun getReminders(){
        mainCoroutinesRule.runBlockingTest {
            database.reminderDao().saveReminder(reminder)
            val result = database.reminderDao().getReminderById(reminder.id)
            assertThat(result,notNullValue())
            assertThat(result!!.description, `is`(reminder.description))
            assertThat(result.title, `is`(reminder.title))
            assertThat(result.latitude, `is`(reminder.latitude))
            assertThat(result .longitude, `is`(reminder.longitude))
        }
    }

    @Test
    fun getReminder_showLoading(){
        mainCoroutinesRule.pauseDispatcher()
        viewModel.loadReminders()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutinesRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}