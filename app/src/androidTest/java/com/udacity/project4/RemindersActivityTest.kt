package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private val reminder = ReminderDTO(
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
        repository = get()

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
    fun launchRemindersActivity(){
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        activityScenario.onActivity {
            dataBindingIdlingResource.activity = it
        }
        // Start Flow Of Adding New Reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        val title = "Test Title"
        val description = "Test Description"
        // Add Details of reminder
        onView(withId(R.id.reminderTitle)).perform(typeText(title))
        onView(withId(R.id.reminderDescription)).perform(typeText(description))
        closeSoftKeyboard()
        // add Location
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_action))
            .perform(click())
        onView(withId(R.id.map)).perform(click())
        // Test was falling when try click on add_location cause of
        // button is not Completely visible
        // so we sleep Thread for giving time to processor make button Visible
        Thread.sleep(1000)
        onView(withId(R.id.add_location)).perform(click())
        // Check Result
        onView(withText(title)).check(matches(isDisplayed()))
        onView(withText(description)).check(matches(isDisplayed()))

        activityScenario.close()

    }

    @Test
    fun openRemindersActivityWithActiveReminder() {
        runBlocking {
            repository.saveReminder(reminder)
        }
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        activityScenario.onActivity { dataBindingIdlingResource.activity = it }
        // Check if reminder is displayed
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.location)).check(matches(isDisplayed()))
        onView(withText(reminder.description)).check(matches(isDisplayed()))

        activityScenario.close()
    }
    @Test
    fun navigateSave_Save_ShowToast() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        lateinit var activity:Activity
        activityScenario.onActivity {
            activity = it
        }
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_action))
            .perform(click())
        onView(withId(R.id.map)).perform(click())
        Thread.sleep(800L)

        onView(withId(R.id.add_location)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Test Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Test Description"))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(R.string.reminder_saved))
            .inRoot(withDecorView(not(`is`(activity.window.decorView))))
            .check(matches(isDisplayed()))

        activityScenario.close()
    }

}
