package com.eazymile.app.models

import java.io.Serializable

data class Booking(
    var bookingId: String = "",
    var fleetName: String = "",
    var fleetModel: String = "",
    var date: String = "",
    var duration: String = "",
    var status: String = "Pending",
    var price: String = "",
    var selectedPrice: String = ""
) : Serializable {


}
