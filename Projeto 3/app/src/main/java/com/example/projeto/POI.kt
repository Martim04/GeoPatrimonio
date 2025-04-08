package com.example.projeto
import android.os.Parcel
import android.os.Parcelable

data class POI(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var distance: Float = 0f,
    val imageBase64: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readFloat(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeFloat(distance)
        parcel.writeString(imageBase64)
    }


    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<POI> {
        override fun createFromParcel(parcel: Parcel) = POI(parcel)
        override fun newArray(size: Int) = arrayOfNulls<POI>(size)
    }
}