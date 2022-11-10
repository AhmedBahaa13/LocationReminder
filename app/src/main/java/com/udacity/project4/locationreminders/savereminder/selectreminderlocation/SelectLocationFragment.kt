package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

private const val TAG: String = "SelectLocationFragment"

@SuppressLint("MissingPermission")
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {


    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var selectedPoi: PointOfInterest? = null
    private var lat: Double? = null
    private var lng: Double? = null
    private var locationName: String = ""
    private val zoomLevel = 40f
    private var selectedMarker: Marker? = null
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { result -> result.value!! }) {
            //granted
            setupLocationButton()
        } else {
            //not granted
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
        }
    private val registerIntentSender =
    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
        if (it.resultCode == Activity.RESULT_OK)
            checkDeviceLocationSettingsAndAddGeofence(false)
    }

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

        val fragmentMap = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        fragmentMap?.getMapAsync(this)
        return binding.root
    }

    private fun onLocationSelected() {
        _viewModel.latitude.value = lat
        _viewModel.longitude.value = lng
        _viewModel.selectedPOI.value = selectedPoi
        _viewModel.reminderSelectedLocationStr.value = locationName
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        setupLocationButton()
        setUpMapClickListener()
        setupPoiClickListener()
        setMapStyle()
        Snackbar.make(binding.root, R.string.select_poi, Snackbar.LENGTH_LONG)
            .setAction(R.string.dismss) {
                // dismiss
            }
            .show()
    }
    private fun setupLocationButton() {
        when {
        checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
                == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED -> {
            //permission granted
            checkDeviceLocationSettingsAndAddGeofence()
        }
        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
        else -> {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    }

    private fun setUpMapClickListener() {
        val geocoder = Geocoder(activity)
        var detailedLocation: Address?
        map.setOnMapClickListener {
            selectedMarker?.remove()
            binding.addLocation.visibility = View.VISIBLE
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, zoomLevel))
            lat = it.latitude
            lng = it.longitude
            detailedLocation = geocoder.getFromLocation(it.latitude, it.longitude, 1).firstOrNull()
            locationName = "{${detailedLocation?.getAddressLine(0)}}"
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

    private fun getLastKnownLocation() {
        map.isMyLocationEnabled = true
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location.latitude,
                                location.longitude
                            ), zoomLevel
                        )
                    )
                }else{
                    val locationRequest = LocationRequest.create().apply {
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        interval = 10000L
                    }
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            locationResult ?: return
                            for (location in locationResult.locations){
                                    val latLng = LatLng(location.latitude,location.longitude)
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                }
                        }
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
                        Looper.getMainLooper())
                }
            }
    }
    private fun checkDeviceLocationSettingsAndAddGeofence(resolve: Boolean = true) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    Log.d(TAG, "checkDeviceLocationSettingsAndAddGeofence: start Intent")

                    val request: IntentSenderRequest = IntentSenderRequest
                        .Builder(exception.resolution)
                        .build()

                    registerIntentSender.launch(request)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Log.d(TAG, "checkDeviceLocationSettingsAndAddGeofence: show SnackBar")
                Snackbar.make(
                    binding.root
                    ,R.string.location_required_error
                    ,Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndAddGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "locationSettingsResponseTask is success")
                getLastKnownLocation()
            }
        }

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
}

