package com.fitstudio.mobile

data class Named(val id: Int, val label: String) {
    override fun toString() = label
}

data class Client(
    val clientId: Int,
    val fullName: String,
    val phone: String,
    val email: String?,
    val birthDate: String,
    val registeredAt: String
)

data class Plan(val planId: Int, val name: String, val price: Double, val visitLimit: Int?) {
    override fun toString(): String {
        val visits = if (visitLimit == null) "безлимит" else "$visitLimit посещ."
        return "$name · ${price.toInt()} ₽ · $visits"
    }
}

data class Membership(
    val membershipId: Int,
    val planName: String,
    val price: Double,
    val startDate: String,
    val endDate: String,
    val visitsLeft: Int?,
    val status: String
)

data class GymClass(
    val classId: Int,
    val title: String,
    val trainerId: Int,
    val trainerName: String,
    val hallId: Int,
    val hallName: String,
    val startsAt: String,
    val durationMinutes: Int,
    val maxParticipants: Int,
    val taken: Int
)

data class Enrollment(
    val enrollmentId: Int,
    val clientId: Int,
    val clientName: String,
    val status: String,
    val enrolledAt: String
)

fun isoDate(value: String) = value.take(10)

fun isoDateTime(value: String): String {
    val text = value.replace('T', ' ')
    return if (text.length >= 16) text.take(16) else text
}
