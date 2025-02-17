package com.example.projeto

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap

class ZoomViewModel : ViewModel() {
    private var googleMap: GoogleMap? = null

    fun setGoogleMap(map: GoogleMap) {
        googleMap = map
    }

    fun zoomIn() {
        googleMap?.let {
            val currentZoomLevel = it.cameraPosition.zoom
            it.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel + 1))
        }
    }

    fun zoomOut() {
        googleMap?.let {
            val currentZoomLevel = it.cameraPosition.zoom
            it.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel - 1))
        }
    }
}