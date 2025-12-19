package com.eazymile.app.models

data class Fleet(
    val batteryLevel: String? = "",
    val brand: String? = "",
    val imgUrl: String? = "",
    val model: String? = "",
    val power: String? = "",
    val pricePerDay: Double? = 0.0,
    val pricePerHour: Double? = 0.0,
    val rangeMileage: String? = "",
    val status: String? = "",
    val type: String? ="",
    val vehicle: String? = "",

    // Static fields to show in adapter
    val zeroDeposit: String = "Zero Deposit",
    val kmPackages: String = "KM Packages"
)
