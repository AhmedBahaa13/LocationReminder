package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
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
import org.koin.android.ext.android.inject
import kotlin.math.ln

@SuppressLint("MissingPermission")
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val ACCESS_LOCATION_PERMISSION: Int = 501
    private val TAG: String = SelectLocationFragment::class.java.simpleName

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var selectedPoi: PointOfInterest? = null
    private var lat: Double? = null
    private var lng: Double? = null
    private var locationName: String = ""
    private val zoomLevel = 40f
    private var selectedMarker : Marker? = null

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

        binding.addLocation.setOnClickListener {
            if (lat != null && lng != null)
                onLocationSelected()
            else
                Toast.makeText(requireContext(), "Please Select Location", Toast.LENGTH_SHORT)
                    .show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            if (it.isSuccessful) {
                if (it.result != null) {
                    val currentLatlng = LatLng(it.result.latitude, it.result.longitude)
                    map.addMarker(MarkerOptions().position(currentLatlng)).apply {
                        selectedMarker = this
                    }
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatlng, zoomLevel))
                }
            } else
                Log.d(TAG, "onMapReady: ${it.exception?.message}")

        }
    }

    private fun setupLocationButton() {
        if (ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),ACCESS_LOCATION_PERMISSION)
        }else{
            map.isMyLocationEnabled = true
        }
        map.setOnMyLocationClickListener {
            val latlng = LatLng(it.latitude, it.longitude)
            Log.d(TAG, "setupLocationButton: Location $it \n Lat and Lng $latlng")
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoomLevel))
        }
    }

    private fun setUpMapClickListener() {
        val geocoder = Geocoder(activity)
        var detailedLocation:Address
        map.setOnMapClickListener {
            selectedMarker?.remove()
            binding.addLocation.visibility = View.VISIBLE
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, zoomLevel))
            lat = it.latitude
            lng = it.longitude
            detailedLocation = geocoder.getFromLocation(it.latitude, it.longitude, 1)[0]
            locationName = "{${detailedLocation.getAddressLine(0)}}"
            map.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(locationName)
            ).apply {
                selectedMarker = this
                showInfoWindow()
            }

        }
    }

    private fun setupPoiClickListener() {
        map.setOnPoiClickListener { newpoi ->
            selectedMarker?.remove()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(newpoi.latLng)
                    .title(newpoi.name)
            )
            selectedMarker = poiMarker
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
                Toast.makeText(context, "Style parsing failed", Toast.LENGTH_LONG).show()
            }
        } catch (e: Resources.NotFoundException) {
            Toast.makeText(context, "error ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACCESS_LOCATION_PERMISSION){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                map.isMyLocationEnabled = true
            }else{
                _viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
            }
        }
    }
}

