package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.pbl6.fitme.databinding.FragmentShippingAddressBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.pbl6.fitme.network.province.*

class ShippingAddressFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentShippingAddressBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: ApiService
    private lateinit var mapService: MapService

    private lateinit var googleMap: GoogleMap
    private var isMapReady = false

    // Data List
    private var provinceList: List<ProvinceModel> = emptyList()
    private var districtList: List<DistrictModel> = emptyList()
    private var wardList: List<WardModel> = emptyList()

    // Selected Objects
    private var selectedProvince: ProvinceModel? = null
    private var selectedDistrict: DistrictModel? = null
    private var selectedWard: WardModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShippingAddressBinding.inflate(inflater, container, false)
        apiService = ApiClient.getClient().create(ApiService::class.java)
        mapService = MapService()

        // Initialize TextView as GONE
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

        loadProvinces()
        setupListeners()

        binding.etAddress.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                handleAddressSelectionComplete()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        val vietnamCenter = LatLng(14.0583, 108.2772)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnamCenter, 5f))
        googleMap.uiSettings.isZoomControlsEnabled = true
        displayStatusMessage("Map system is ready.", isError = false)
    }

    /**
     * Displays a status or error message in the dedicated TextView.
     * @param message The text to display.
     * @param isError True if the message is an error (will be displayed in red/dark red).
     */
    private fun displayStatusMessage(message: String, isError: Boolean) {
        if (message.isBlank()) {
            binding.tvError.visibility = View.GONE
            return
        }

        binding.tvError.text = message
        binding.tvError.setTextColor(
            if (isError) {
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            } else {
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            }
        )
        binding.tvError.visibility = View.VISIBLE
    }

    private fun resetSpinner(spinner: Spinner, hint: String, enabled: Boolean) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(hint))
        spinner.adapter = adapter
        spinner.isEnabled = enabled
        spinner.setSelection(0, false)
    }

    private fun setSpinnerEnabled(spinner: Spinner, enabled: Boolean) {
        spinner.isEnabled = enabled
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            handleSaveAndShowMap()
        }

        binding.spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    binding.etAddress.setText("")
                    val newProvince = provinceList[position - 1]
                    if (newProvince.code != selectedProvince?.code) {
                        selectedProvince = newProvince
                        loadDistricts(newProvince.code)
                    }
                } else {
                    selectedProvince = null
                    resetSpinner(binding.spinnerDistrict, "Select District/Town", false)
                    resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)
                    if (position == 0 && provinceList.isNotEmpty()) {
                        displayStatusMessage("Please select a Province/City.", isError = true)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val newDistrict = districtList[position - 1]
                    if (newDistrict.code != selectedDistrict?.code) {
                        selectedDistrict = newDistrict
                        loadWards(newDistrict.code)
                    }
                } else {
                    selectedDistrict = null
                    resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)
                    if (position == 0 && districtList.isNotEmpty()) {
                        displayStatusMessage("Please select a District/Town.", isError = true)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerWard.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedWard = wardList[position - 1]
                    handleAddressSelectionComplete()
                } else {
                    selectedWard = null
                    if (position == 0 && wardList.isNotEmpty()) {
                        displayStatusMessage("Please select a Ward/Commune.", isError = true)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun handleAddressSelectionComplete() {
        val detailAddress = binding.etAddress.text.toString()

        if (selectedWard == null || selectedDistrict == null || selectedProvince == null || detailAddress.isBlank()) {
            displayStatusMessage("ERROR: Please fill in the detail address and select all Province, District, and Ward before saving.", isError = true)
            return
        }

        if (!isMapReady) {
            displayStatusMessage("Map is currently loading. Please wait.", isError = false)
            return
        }

        val optimalAddress = "${selectedWard!!.name}, ${selectedDistrict!!.name}, ${selectedProvince!!.name}, Vietnam"
        val fullAddressForMarker = "$detailAddress, $optimalAddress"

        displayStatusMessage("Searching for location automatically...", isError = false)

        geocodeAndDisplay(optimalAddress, fullAddressForMarker)
    }

    // --- SAVE LOGIC (Save Button) ---
    private fun handleSaveAndShowMap() {

        val detailAddress = binding.etAddress.text.toString()

        if (selectedWard == null || selectedDistrict == null || selectedProvince == null || detailAddress.isBlank()) {
            displayStatusMessage("ERROR: Please fill in the detail address and select all Province, District, and Ward before saving.", isError = true)
            return
        }

        displayStatusMessage("Save successful! (Mock)", isError = false)

        // Update map after save attempt
        handleAddressSelectionComplete()
    }

    // Geocode and Display
    private fun geocodeAndDisplay(searchAddress: String, markerTitle: String) {
        mapService.getCoordinatesFromAddress(searchAddress, object : MapService.OnLocationResult {

            override fun onLocationFound(lat: Double, lng: Double) {
                activity?.runOnUiThread {
                    if (lat != 0.0 && lng != 0.0) {
                        val latLng = LatLng(lat, lng)

                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(latLng).title(markerTitle))
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                        displayStatusMessage("Marker placed on map!", isError = false)
                        Log.d("MAP_SUCCESS", "Location found: $lat, $lng")
                    } else {
                        Log.w("MAP_ERROR", "MapService could not find coordinates for address: $searchAddress")
                        displayStatusMessage("Precise location not found. Try simplifying the detail address.", isError = true)
                    }
                }
            }
        })
    }


    // --- LOAD API FUNCTIONS (Error handling updated for TextView) ---

    private fun loadProvinces() {
        resetSpinner(binding.spinnerProvince, "Loading...", false)
        apiService.getProvinces().enqueue(object : Callback<List<ProvinceModel>> {
            override fun onResponse(call: Call<List<ProvinceModel>>, response: Response<List<ProvinceModel>>) {
                if (response.isSuccessful) {
                    provinceList = response.body() ?: emptyList()
                    if (provinceList.isNotEmpty()) {
                        val names = listOf("Select Province/City") + provinceList.map { it.name }
                        binding.spinnerProvince.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                        setSpinnerEnabled(binding.spinnerProvince, true)
                        displayStatusMessage("Province/City list loaded successfully.", isError = false)
                    } else {
                        resetSpinner(binding.spinnerProvince, "Data Error", false)
                        displayStatusMessage("🔴 ERROR: Empty Province/City data structure.", isError = true)
                    }
                } else {
                    resetSpinner(binding.spinnerProvince, "Server Error ${response.code()}", false)
                    displayStatusMessage("🔴 Server Error: Could not load Province/City list.", isError = true)
                }
            }
            override fun onFailure(call: Call<List<ProvinceModel>>, t: Throwable) {
                resetSpinner(binding.spinnerProvince, "Connection Error", false)
                displayStatusMessage("📶 Connection Error: Failed to load Province/City list. Check network.", isError = true)
            }
        })
    }

    private fun loadDistricts(provinceCode: String) {
        resetSpinner(binding.spinnerDistrict, "Loading...", false)
        resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)

        apiService.getDistricts(provinceCode).enqueue(object : Callback<ProvinceDetailResponse> {
            override fun onResponse(call: Call<ProvinceDetailResponse>, response: Response<ProvinceDetailResponse>) {
                if (response.isSuccessful) {
                    districtList = response.body()?.districts ?: emptyList()
                    val names = listOf("Select District/Town") + districtList.map { it.name }
                    binding.spinnerDistrict.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    setSpinnerEnabled(binding.spinnerDistrict, true)
                } else {
                    resetSpinner(binding.spinnerDistrict, "District Load Error", false)
                    displayStatusMessage("ERROR: Could not load District/Town list.", isError = true)
                }
            }
            override fun onFailure(call: Call<ProvinceDetailResponse>, t: Throwable) {
                resetSpinner(binding.spinnerDistrict, "Connection Error", false)
                displayStatusMessage("📶 Connection Error: Failed to load District/Town list.", isError = true)
            }
        })
    }

    private fun loadWards(districtCode: String) {
        resetSpinner(binding.spinnerWard, "Loading...", false)
        apiService.getWards(districtCode).enqueue(object : Callback<DistrictModel> {
            override fun onResponse(call: Call<DistrictModel>, response: Response<DistrictModel>) {
                if (response.isSuccessful) {
                    wardList = response.body()?.wards ?: emptyList()
                    val names = listOf("Select Ward/Commune") + wardList.map { it.name }
                    binding.spinnerWard.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    setSpinnerEnabled(binding.spinnerWard, true)
                } else {
                    resetSpinner(binding.spinnerWard, "Ward Load Error", false)
                    displayStatusMessage("ERROR: Could not load Ward/Commune list.", isError = true)
                }
            }
            override fun onFailure(call: Call<DistrictModel>, t: Throwable) {
                resetSpinner(binding.spinnerWard, "Connection Error", false)
                displayStatusMessage("📶 Connection Error: Failed to load Ward/Commune list.", isError = true)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}