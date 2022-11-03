package com.udacity.project4.locationreminders.reminderslist

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import java.io.Serializable
import java.util.*

/**
 * data class acts as a data mapper between the DB and the UI
 */
data class ReminderDataItem(
    var title: String?=null,
    var description: String?=null,
    var location: String?=null,
    var latitude: Double?=null,
    var longitude: Double?=null,
    val id: String = UUID.randomUUID().toString()
) : Serializable{
    fun asDTO():ReminderDTO{
        return ReminderDTO(
            title, description, location, latitude, longitude, id
        )
    }
}