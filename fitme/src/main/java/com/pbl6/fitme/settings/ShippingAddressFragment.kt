package com.pbl6.fitme.settings

import android.os.Bundle
import android.util.Log
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
import com.pbl6.fitme.databinding.FragmentShippingAddressBinding
import com.pbl6.fitme.network.UserAddressRequest
import com.pbl6.fitme.network.province.*
import com.pbl6.fitme.repository.AddressRepository
import com.pbl6.fitme.repository.UserRepository // Sử dụng UserRepository thay vì AuthRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ShippingAddressFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentShippingAddressBinding? = null
    private val binding get() = _binding!!

    // Services & Repositories
    private lateinit var apiService: ApiService     // API địa chính
    private lateinit var mapService: MapService       // API Map
    private val addressRepository = AddressRepository() // API Lưu địa chỉ
    private val userRepository = UserRepository()     // API Lấy thông tin User (Mới)

    // Map
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

    private var fetchedUserEmail: String = ""
    private var fetchedUserName: String = ""
    private var fetchedUserPhone: String = ""
    private var isUserInfoLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShippingAddressBinding.inflate(inflater, container, false)
        mapService = MapService()
        apiService = ApiClient.getClient().create(ApiService::class.java)
        binding.tvError.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(binding.mapContainer.id) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Reset UI
        resetSpinner(binding.spinnerProvince, "Select Province/City", false)
        resetSpinner(binding.spinnerDistrict, "Select District/Town", false)
        resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)

        // 1. Load danh sách tỉnh
        loadProvinces()

        // 2. Load thông tin User ngay khi vào màn hình
        fetchUserProfile()

        setupListeners()

        binding.etAddress.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) handleAddressSelectionComplete()
        }
        binding.ivBack.singleClick {
            popBackStack()
        }
    }

    // --- HÀM MỚI: LẤY THÔNG TIN USER TỪ API ---
    private fun fetchUserProfile() {
        val context = requireContext()
        val token = SessionManager.getInstance().getAccessToken(context)

        val userId = SessionManager.getInstance().getUserId(context)?.toString()
        Log.d("ShippingAddress", "Fetched userId from session: $userId")

        if (token.isNullOrBlank()) {
            displayStatusMessage("User not logged in (Token missing).", true)
            return
        }

        if (userId.isNullOrBlank()) {
            displayStatusMessage("User ID not found in session.", true)
            return
        }

        displayStatusMessage("Loading user profile...", false)

        // Gọi UserRepository để lấy chi tiết
        userRepository.getUserDetail(token, userId) { userResult ->
            activity?.runOnUiThread {
                if (userResult != null) {
                    // Lưu dữ liệu vào biến
                    fetchedUserName = userResult.fullName ?: userResult.username // Fallback nếu fullName null
                    fetchedUserEmail = userResult.email
                    fetchedUserPhone = userResult.phone ?: ""
                    isUserInfoLoaded = true

                    displayStatusMessage("", false) // Xóa thông báo loading
                    Log.d("ShippingAddress", "User info loaded: $fetchedUserName - $fetchedUserPhone")

                    // Nếu layout của bạn có EditText cho Tên/SĐT, hãy điền vào đây:
                    // binding.etName.setText(fetchedUserName)
                    // binding.etPhone.setText(fetchedUserPhone)
                } else {
                    displayStatusMessage("Failed to load user information from server.", true)
                }
            }
        }
    }

    // --- LOGIC LƯU (SỬA ĐỔI) ---
    private fun handleSaveAndShowMap() {
        val detailAddress = binding.etAddress.text.toString().trim()

        // 1. Validate địa chỉ nhập tay
        if (selectedWard == null || selectedDistrict == null || selectedProvince == null || detailAddress.isBlank()) {
            displayStatusMessage("Please select full address information (Province, District, Ward, Detail).", true)
            return
        }

        // 2. Validate thông tin User (Phải load xong mới cho lưu)
        if (!isUserInfoLoaded) {
            displayStatusMessage("User information is still loading. Please wait a moment...", false)
            fetchUserProfile() // Thử load lại
            return
        }

        // 3. Validate dữ liệu user bắt buộc (Tên, SĐT)
        if (fetchedUserName.isBlank()) {
            displayStatusMessage("User Name is missing from profile.", true)
            return
        }
        // Nếu backend bắt buộc SĐT thì bỏ comment dòng dưới
        /*
        if (fetchedUserPhone.isBlank()) {
            displayStatusMessage("User Phone is missing. Please update profile.", true)
            return
        }
        */

        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""

        // Tạo chuỗi địa chỉ đầy đủ cho addressLine2 (Backend có thể dùng để hiển thị nhanh)
        val fullRegion = "${selectedWard!!.name}, ${selectedDistrict!!.name}, ${selectedProvince!!.name}"

        // 4. Tạo Request từ dữ liệu User đã load + Địa chỉ vừa nhập
        val request = UserAddressRequest(
            userEmail = fetchedUserEmail,
            recipientName = fetchedUserName, // Lấy từ API User
            phone = fetchedUserPhone,        // Lấy từ API User
            addressLine1 = detailAddress,    // Lấy từ UI nhập tay
            addressLine2 = fullRegion,       // Lấy từ Spinner
            city = selectedProvince!!.name,
            stateProvince = selectedDistrict!!.name,
            postalCode = "70000",            // Mặc định hoặc cho nhập
            country = "Vietnam",
            isDefault = true,                // Mặc định là true hoặc lấy từ switch
            addressType = "HOME"
        )

        binding.btnSave.isEnabled = false
        displayStatusMessage("Saving address...", false)

        // 5. Gọi API Address để lưu
        addressRepository.addUserAddress(token, request) { response ->
            activity?.runOnUiThread {
                binding.btnSave.isEnabled = true
                if (response != null) {
                    displayStatusMessage("Address saved successfully!", false)
                    Toast.makeText(context, "Address Added! ID: ${response.addressId}", Toast.LENGTH_SHORT).show()

                    // Thành công -> Có thể quay lại màn hình trước
                    // hoang.dqm.codebase.base.activity.popBackStack()
                } else {
                    displayStatusMessage("Failed to save address. Please try again.", true)
                }
            }
        }
    }

    // --- Map & Helpers (Giữ nguyên) ---
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        val vietnamCenter = LatLng(14.0583, 108.2772)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnamCenter, 5f))
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun handleAddressSelectionComplete() {
        val detailAddress = binding.etAddress.text.toString()
        if (selectedWard != null && selectedDistrict != null && selectedProvince != null && detailAddress.isNotBlank()) {
            if (!isMapReady) return
            val optimalAddress = "${selectedWard!!.name}, ${selectedDistrict!!.name}, ${selectedProvince!!.name}, Vietnam"
            val fullAddressForMarker = "$detailAddress, $optimalAddress"
            geocodeAndDisplay(optimalAddress, fullAddressForMarker)
        }
    }

    private fun geocodeAndDisplay(searchAddress: String, markerTitle: String) {
        mapService.getCoordinatesFromAddress(searchAddress, object : MapService.OnLocationResult {
            override fun onLocationFound(lat: Double, lng: Double) {
                activity?.runOnUiThread {
                    if (lat != 0.0 && lng != 0.0) {
                        val latLng = LatLng(lat, lng)
                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(latLng).title(markerTitle))
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        displayStatusMessage("Location found on map!", false)
                    } else {
                        displayStatusMessage("Could not locate address on map.", true)
                    }
                }
            }
        })
    }

    private fun displayStatusMessage(message: String, isError: Boolean) {
        if (message.isBlank()) {
            binding.tvError.visibility = View.GONE
            return
        }
        binding.tvError.text = message
        binding.tvError.setTextColor(
            if (isError) ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            else ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
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
        binding.btnSave.setOnClickListener { handleSaveAndShowMap() }

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
                    selectedProvince = null; resetSpinner(binding.spinnerDistrict, "Select District/Town", false); resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)
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
                    selectedDistrict = null; resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)
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
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadProvinces() {
        apiService.getProvinces().enqueue(object : Callback<List<ProvinceModel>> {
            override fun onResponse(call: Call<List<ProvinceModel>>, response: Response<List<ProvinceModel>>) {
                if (response.isSuccessful) {
                    provinceList = response.body() ?: emptyList()
                    val names = listOf("Select Province/City") + provinceList.map { it.name }
                    binding.spinnerProvince.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    setSpinnerEnabled(binding.spinnerProvince, true)
                }
            }
            override fun onFailure(call: Call<List<ProvinceModel>>, t: Throwable) {}
        })
    }

    private fun loadDistricts(provinceCode: String) {
        apiService.getDistricts(provinceCode).enqueue(object : Callback<ProvinceDetailResponse> {
            override fun onResponse(call: Call<ProvinceDetailResponse>, response: Response<ProvinceDetailResponse>) {
                if (response.isSuccessful) {
                    districtList = response.body()?.districts ?: emptyList()
                    val names = listOf("Select District/Town") + districtList.map { it.name }
                    binding.spinnerDistrict.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    setSpinnerEnabled(binding.spinnerDistrict, true)
                }
            }
            override fun onFailure(call: Call<ProvinceDetailResponse>, t: Throwable) {}
        })
    }

    private fun loadWards(districtCode: String) {
        apiService.getWards(districtCode).enqueue(object : Callback<DistrictModel> {
            override fun onResponse(call: Call<DistrictModel>, response: Response<DistrictModel>) {
                if (response.isSuccessful) {
                    wardList = response.body()?.wards ?: emptyList()
                    val names = listOf("Select Ward/Commune") + wardList.map { it.name }
                    binding.spinnerWard.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    setSpinnerEnabled(binding.spinnerWard, true)
                }
            }
            override fun onFailure(call: Call<DistrictModel>, t: Throwable) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}