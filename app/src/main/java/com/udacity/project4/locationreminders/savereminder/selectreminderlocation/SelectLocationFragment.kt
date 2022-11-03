package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.it_reminder.*
import org.koin.android.ext.android.inject

@SuppressLint("MissingPermission")
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG: String = SelectLocationFragment::class.java.simpleName

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var selectedPoi : PointOfInterest? = null
    private var lat :Double? = null
    private var lng :Double? = null
    private var locationName :String =""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        binding.addLocation.setOnClickListener {
            if (lat != null && lng != null)
                onLocationSelected()
            else
                Toast.makeText(requireContext(),"Please Select Location",Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //        TODO: add the map setup implementation
        val fragmentMap = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        fragmentMap?.getMapAsync(this)
    }

    private fun onLocationSelected() {
        //         When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        _viewModel.latitude.value = lat
        _viewModel.longitude.value = lng
        _viewModel.selectedPOI.value = selectedPoi
        _viewModel.reminderSelectedLocationStr.value = locationName
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        setupLocationButton()
        setUpMapClickListener()
        setupPoiClickListener()
        setMapStyle()
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            if (it.isSuccessful){
                if (it.result != null){
                    val currentLatlng = LatLng(it.result.latitude, it.result.longitude)
                    val marker = MarkerOptions().position(currentLatlng)
                    map.addMarker(marker)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatlng, 15f))
                }
            }else
                Log.d(TAG, "onMapReady: ${it.exception?.message}")

        }
    }

    private fun setupLocationButton() {
        map.isMyLocationEnabled = true
        map.setOnMyLocationClickListener {
            val latlng = LatLng(it.latitude,it.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15f))
        }
    }

    private fun setUpMapClickListener() {
        map.setOnMapClickListener {
            binding.addLocation.visibility = View.VISIBLE
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(it,15f))
            lat = it.latitude
            lng = it.longitude
            locationName = "{${it.latitude.toInt()}, ${it.longitude.toInt()}}"
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("{${it.latitude.toInt()}, ${it.longitude.toInt()}}")
            )
            poiMarker.showInfoWindow()
        }
    }

    private fun setupPoiClickListener(){
        map.setOnPoiClickListener { newpoi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(newpoi.latLng)
                    .title(newpoi.name)
            )
            val zoomLevel = 15f
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(newpoi.latLng, zoomLevel))
            poiMarker.showInfoWindow()
            selectedPoi = newpoi
            lat = newpoi.latLng.latitude
            lng = newpoi.latLng.longitude
            locationName = newpoi.name
            binding.addLocation.visibility = View.VISIBLE
        }

    }

    private fun setMapStyle() {
        try {
            val successful = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style
                )
            )
            if (!successful) {
                Toast.makeText(context,"Style parsing failed", Toast.LENGTH_LONG).show()
            }
        } catch (e: Resources.NotFoundException) {
            Toast.makeText(context, "error ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

