package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

private const val RESOURCE = "GLOBAL"

object EspressoIdlingResource {
    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}

inline fun <T> wrapEspressoIdlingResource(function: () -> T):T {
    EspressoIdlingResource.increment()
    return try {
        function()
    }finally{
        EspressoIdlingResource.decrement()
    }
}