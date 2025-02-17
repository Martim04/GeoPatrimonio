package com.example.projeto

import android.os.Parcel
import android.os.Parcelable

data class POI(
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    var distance: Float = 0f
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeFloat(distance)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<POI> {
        override fun createFromParcel(parcel: Parcel) = POI(parcel)
        override fun newArray(size: Int) = arrayOfNulls<POI>(size)
    }
}