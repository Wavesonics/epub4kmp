package io.documentnode.epub4j.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A Date used by the book's metadata.
 *
 * Examples: creation-date, modification-date, etc
 */
class Date(
    val value: String,
    val event: Event? = null
) {
    enum class Event(private val value: String) {
        PUBLICATION("publication"),
        MODIFICATION("modification"),
        CREATION("creation");

        companion object {
            fun fromValue(value: String): Event? {
                return entries.firstOrNull { it.value == value }
            }
        }
    }

    constructor(date: LocalDate) : this(date.toString())

    constructor(date: LocalDate, event: Event?) : this(date.toString(), event)

    constructor(instant: Instant) : this(
        instant.toLocalDateTime(TimeZone.UTC).date.toString()
    )

    constructor(dateString: String, event: String) : this(
        dateString,
        Event.fromValue(event)
    )

    override fun toString(): String {
        if (event == null) {
            return value
        }
        return "$event:$value"
    }
}
