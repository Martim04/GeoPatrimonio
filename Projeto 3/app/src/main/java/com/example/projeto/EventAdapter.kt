package com.example.projeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import java.text.DecimalFormat

// Data class otimizada
data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val creationDate: String = "",
    val eventDate: String = "",
    val poiId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : android.os.Parcelable {
    constructor(parcel: android.os.Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(creationDate)
        parcel.writeString(eventDate)
        parcel.writeString(poiId)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : android.os.Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: android.os.Parcel): Event = Event(parcel)
        override fun newArray(size: Int): Array<Event?> = arrayOfNulls(size)
    }
}

class EventAdapter(
    private val userLocation: LatLng,
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val events: MutableList<Event> = mutableListOf()
    private val decimalFormat = DecimalFormat("#.##")

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.event_title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.event_description)
        val dateTextView: TextView = itemView.findViewById(R.id.event_date)
        val locationTextView: TextView = itemView.findViewById(R.id.event_location)
        val distanceTextView: TextView = itemView.findViewById(R.id.event_distance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        with(holder) {
            titleTextView.text = event.title
            descriptionTextView.text = event.description
            dateTextView.text = event.eventDate
            locationTextView.text = "Lat: ${event.latitude}, Lng: ${event.longitude}"

            // Calcular e formatar a distância
            val eventLocation = LatLng(event.latitude, event.longitude)
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                eventLocation.latitude, eventLocation.longitude,
                distance
            )
            distanceTextView.text = "${decimalFormat.format(distance[0] / 1000)} km"

            itemView.setOnClickListener { onItemClick(event)
            }
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = events.size
            override fun getNewListSize(): Int = newEvents.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                events[oldItemPosition].id == newEvents[newItemPosition].id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                events[oldItemPosition] == newEvents[newItemPosition]
        })
        events.clear()
        events.addAll(newEvents)
        diffResult.dispatchUpdatesTo(this)
    }
}