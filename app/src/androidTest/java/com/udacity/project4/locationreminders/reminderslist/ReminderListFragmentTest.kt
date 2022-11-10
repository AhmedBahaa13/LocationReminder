package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var remindersViewModel: RemindersListViewModel
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private val reminder = ReminderDataItem(
        title = "Test",
        description = "Description",
        location = "Location",
        latitude = 30.085947,
        longitude = 31.223651
    )

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = GlobalContext.get().koin.get()
        saveReminderViewModel = GlobalContext.get().koin.get()
        remindersViewModel = GlobalContext.get().koin.get()
        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    //    TODO: add End to End testing to the app
    @Before
    fun registerIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        register(EspressoIdlingResource.countingIdlingResource)
        register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        unregister(EspressoIdlingResource.countingIdlingResource)
        unregister(dataBindingIdlingResource)
    }

    @Test
    fun openSaveReminder() {
        val navController = mock(NavController::class.java)
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)

        }
        onView(withId(R.id.addReminderFAB)).perform(click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun saveReminder_reminderIsDisplayed() {
        runBlocking {
            repository.saveReminder(reminder.asDTO())
        }
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        activityScenario.onActivity {
            dataBindingIdlingResource.activity = it }
        // Check if reminder is displayed
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.location)).check(matches(isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun saveUnValidReminder_showError() {
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY,R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)
        reminder.title = null
        saveReminderViewModel.validateAndSaveReminder(reminder)
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))
    }
}