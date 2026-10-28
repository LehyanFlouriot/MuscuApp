package com.lehyanflouriot.muscuapp.core.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** A typical week followed until another one takes over: a meal plan, a training program. */
interface Program {
    val id: String
    val name: String

    /**
     * Set on the copies keeping what a program was on the days already lived, when it is changed
     * afterwards: the id of the program they come from. Such copies never change.
     */
    val versionOf: String?
}

/** The program followed from [from] on, until the next period starts. */
@Serializable
data class PlanPeriod(
    val planId: String,
    @Serializable(with = LocalDateIsoSerializer::class)
    val from: LocalDate,
)
