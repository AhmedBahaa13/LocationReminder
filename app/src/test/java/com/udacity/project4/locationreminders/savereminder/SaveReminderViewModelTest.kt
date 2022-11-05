package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutinesRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutinesRule = MainCoroutinesRule()

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var dataSource: ReminderDataSource
    private lateinit var database: RemindersDatabase
    val reminderDataItem = ReminderDataItem(
        title = "Test",
        description = "Description",
        location = "Location",
        latitude = 30.085947,
        longitude = 31.223651
    )

    @Before
    fun setupTest() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(),dataSource)
    }

    @After
    fun cleanTest() {
        database.close()
        stopKoin()
    }

    @Test
    fun saveReminder_showLoading(){
        mainCoroutinesRule.pauseDispatcher()
        viewModel.validateAndSaveReminder(reminderDataItem)
        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))
        mainCoroutinesRule.resumeDispatcher()
        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    @Test
    fun saveUnValidReminder_returnError(){
        reminderDataItem.title = null
        viewModel.validateAndSaveReminder(reminderDataItem)
        MatcherAssert.assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), CoreMatchers.`is`(R.string.err_enter_title))
    }

}