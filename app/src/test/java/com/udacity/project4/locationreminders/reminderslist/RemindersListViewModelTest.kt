package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutinesRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
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
    // provide testing to the RemindersListViewModel and its live data objects

    private lateinit var viewModel: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutinesRule = MainCoroutinesRule()

    @Before
    fun setupTest() {
        dataSource = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @After
    fun cleanTest() {
        stopKoin()
    }

    @Test
    fun getReminders_NoAvailableData() = mainCoroutinesRule.runBlockingTest {
        //GIVEN
        dataSource.deleteAllReminders()
        // WHEN
        viewModel.loadReminders()
        // THEN
        assertThat(viewModel.remindersList.getOrAwaitValue(), `is`(emptyList<ReminderDataItem>()))
    }


    @Test
    fun getReminders_cantRetrieveData() = mainCoroutinesRule.runBlockingTest {
        //GIVEN
        dataSource.setReturnError(true)
        // WHEN
        viewModel.loadReminders()
        // THEN
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("data can't be retrieved!!"))
    }


    @Test
    fun getReminder_showLoading() {
        mainCoroutinesRule.pauseDispatcher()
        viewModel.loadReminders()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutinesRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}