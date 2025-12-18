package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentShippingAddressBinding
import com.pbl6.fitme.network.UserAddressRequest
import com.pbl6.fitme.repository.AddressRepository
import com.pbl6.fitme.repository.UserRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

data class LocalProvince(
    val name: String,
    val districts: List<LocalDistrict> = emptyList()
)

data class LocalDistrict(
    val name: String,
    val wards: List<LocalWard> = emptyList()
)

data class LocalWard(
    val name: String
)

class ShippingAddressFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentShippingAddressBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapService: MapService
    private val addressRepository = AddressRepository()
    private val userRepository = UserRepository()

    private lateinit var googleMap: GoogleMap
    private var isMapReady = false

    private var provinceList: List<LocalProvince> = emptyList()
    private var districtList: List<LocalDistrict> = emptyList()
    private var wardList: List<LocalWard> = emptyList()

    private var selectedProvince: LocalProvince? = null
    private var selectedDistrict: LocalDistrict? = null
    private var selectedWard: LocalWard? = null

    private var fetchedUserEmail: String = ""
    private var fetchedUserName: String = ""
    private var fetchedUserPhone: String = ""
    private var isUserInfoLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShippingAddressBinding.inflate(inflater, container, false)
        mapService = MapService()
        binding.tvError.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(binding.mapContainer.id) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        resetSpinner(binding.spinnerProvince, "Select Province/City", false)
        resetSpinner(binding.spinnerDistrict, "Select District/Town", false)
        resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)

        fetchUserProfile()
        loadDataFromJSON()
        setupListeners()

        binding.ivBack.singleClick { popBackStack() }

        binding.btnSearchMap.setOnClickListener {
            handleAddressSelectionComplete()
        }
    }

    private fun loadDataFromJSON() {
        displayStatusMessage("Loading address data...", false)
        try {
            val jsonString = loadJSONFromRaw(R.raw.provinces)
            if (jsonString != null) {
                val gson = Gson()
                val listType = object : TypeToken<List<LocalProvince>>() {}.type

                provinceList = gson.fromJson(jsonString, listType)

                updateSpinnerData(binding.spinnerProvince, provinceList.map { it.name }, "Select Province/City")
                setSpinnerEnabled(binding.spinnerProvince, true)

                displayStatusMessage("", false)
            } else {
                displayStatusMessage("Failed to load local data file.", true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            displayStatusMessage("Error parsing data: ${e.message}", true)
        }
    }

    private fun loadJSONFromRaw(resourceId: Int): String? {
        return try {
            resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { handleSaveAndShowMap() }

        binding.spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val newProvince = provinceList[position - 1]
                    if (selectedProvince != newProvince) {
                        selectedProvince = newProvince
                        binding.etAddress.setText("")

                        districtList = newProvince.districts
                        updateSpinnerData(binding.spinnerDistrict, districtList.map { it.name }, "Select District/Town")
                        setSpinnerEnabled(binding.spinnerDistrict, true)

                        resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)
                        selectedDistrict = null
                        selectedWard = null
                    }
                } else {
                    selectedProvince = null
                    resetSpinner(binding.spinnerDistrict, "Select District/Town", false)
                    resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val newDistrict = districtList[position - 1]
                    if (selectedDistrict != newDistrict) {
                        selectedDistrict = newDistrict

                        wardList = newDistrict.wards
                        updateSpinnerData(binding.spinnerWard, wardList.map { it.name }, "Select Ward/Commune")
                        setSpinnerEnabled(binding.spinnerWard, true)

                        selectedWard = null
                    }
                } else {
                    selectedDistrict = null
                    resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerWard.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedWard = if (position > 0) {
                    wardList[position - 1]
                } else {
                    null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun handleSaveAndShowMap() {
        val detailAddress = binding.etAddress.text.toString().trim()
        if (selectedWard == null || selectedDistrict == null || selectedProvince == null || detailAddress.isBlank()) {
            displayStatusMessage("Please fill in all address fields.", true)
            return
        }
        if (!isUserInfoLoaded) {
            displayStatusMessage("Loading user info...", false)
            fetchUserProfile()
            return
        }

        val fullRegion = "${selectedWard!!.name}, ${selectedDistrict!!.name}, ${selectedProvince!!.name}"
        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""

        val request = UserAddressRequest(
            userEmail = fetchedUserEmail,
            recipientName = fetchedUserName,
            phone = fetchedUserPhone,
            addressLine1 = detailAddress,
            addressLine2 = fullRegion,
            city = selectedProvince!!.name,
            stateProvince = selectedDistrict!!.name,
            postalCode = "70000",
            country = "Vietnam",
            isDefault = true,
            addressType = "HOME"
        )

        binding.btnSave.isEnabled = false
        displayStatusMessage("Saving...", false)

        addressRepository.addUserAddress(token, request) { response ->
            activity?.runOnUiThread {
                binding.btnSave.isEnabled = true
                if (response != null) {
                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                    popBackStack()
                } else {
                    displayStatusMessage("Failed to save address.", true)
                }
            }
        }
    }

    private fun fetchUserProfile() {
        val context = requireContext()
        val token = SessionManager.getInstance().getAccessToken(context)
        val userId = SessionManager.getInstance().getUserId(context)?.toString()

        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            userRepository.getUserDetail(token, userId) { userResult ->
                activity?.runOnUiThread {
                    if (userResult != null) {
                        fetchedUserName = userResult.fullName ?: userResult.username ?: ""
                        fetchedUserEmail = userResult.email ?: ""
                        fetchedUserPhone = userResult.phone ?: ""
                        isUserInfoLoaded = true
                    }
                }
            }
        }
    }

    private fun handleAddressSelectionComplete() {
        val detailAddress = binding.etAddress.text.toString()

        if (selectedProvince == null || selectedDistrict == null || selectedWard == null) {
            displayStatusMessage("Please select Province, District and Ward.", true)
            return
        }

        if (detailAddress.isBlank()) {
            displayStatusMessage("Please enter detail address.", true)
            return
        }

        if (isMapReady) {
            val optimalAddress = "${selectedWard!!.name}, ${selectedDistrict!!.name}, ${selectedProvince!!.name}, Vietnam"
            val fullSearch = "$detailAddress, $optimalAddress"
            geocodeAndDisplay(optimalAddress, fullSearch)
        } else {
            displayStatusMessage("Map is not ready yet.", false)
        }
    }

    private fun geocodeAndDisplay(searchAddress: String, markerTitle: String) {
        displayStatusMessage("Locating...", false)
        mapService.getCoordinatesFromAddress(searchAddress, object : MapService.OnLocationResult {
            override fun onLocationFound(lat: Double, lng: Double) {
                activity?.runOnUiThread {
                    if (lat != 0.0 && lng != 0.0) {
                        val latLng = LatLng(lat, lng)
                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(latLng).title(markerTitle))
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        displayStatusMessage("Location found.", false)
                    } else {
                        displayStatusMessage("Could not locate address.", true)
                    }
                }
            }
        })
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        val vietnamCenter = LatLng(14.0583, 108.2772)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnamCenter, 5f))
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun updateSpinnerData(spinner: Spinner, data: List<String>, defaultText: String) {
        val names = listOf(defaultText) + data
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter
    }

    private fun resetSpinner(spinner: Spinner, hint: String, enabled: Boolean) {
        updateSpinnerData(spinner, emptyList(), hint)
        spinner.isEnabled = enabled
        spinner.setSelection(0)
    }

    private fun setSpinnerEnabled(spinner: Spinner, enabled: Boolean) {
        spinner.isEnabled = enabled
    }

    private fun displayStatusMessage(message: String, isError: Boolean) {
        if (message.isBlank()) {
            binding.tvError.visibility = View.GONE
            return
        }
        binding.tvError.text = message
        binding.tvError.setTextColor(if (isError) ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark) else ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}