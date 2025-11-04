package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pbl6.fitme.databinding.FragmentShippingAddressBinding
import hoang.dqm.codebase.base.activity.popBackStack
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.view.LayoutInflater

// --- GOOGLE MAPS IMPORTS ---
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.location.Geocoder
import java.util.Locale
// --- END GOOGLE MAPS IMPORTS ---

// --- CUSTOM SERVICE IMPORTS ---
import com.pbl6.fitme.network.province.*
// --- END CUSTOM SERVICE IMPORTS ---


// Thêm OnMapReadyCallback
class ShippingAddressFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentShippingAddressBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: ApiService
    private lateinit var mapService: MapService // Khai báo MapService

    // --- GOOGLE MAPS FIELDS ---
    private lateinit var googleMap: GoogleMap
    private var isMapReady = false
    // --- END GOOGLE MAPS FIELDS ---

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
        mapService = MapService() // Khởi tạo MapService
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy SupportMapFragment và đăng ký callback
        val mapFragment = childFragmentManager.findFragmentById(binding.mapContainer.id) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Reset và vô hiệu hóa các Spinner
        resetSpinner(binding.spinnerProvince, "Select Province/City", false)
        resetSpinner(binding.spinnerDistrict, "Select District/Town", false)
        resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)

        loadProvinces()
        setupListeners()
    }

    // Khởi tạo GoogleMap
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        val vietnamCenter = LatLng(14.0583, 108.2772)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnamCenter, 5f))
        googleMap.uiSettings.isZoomControlsEnabled = true
        Log.i("MAP_INIT", "Google Map đã sẵn sàng.")
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
                    val newProvince = provinceList[position - 1]
                    if (newProvince.code != selectedProvince?.code) {
                        selectedProvince = newProvince
                        loadDistricts(newProvince.code)
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
                    if (newDistrict.code != selectedDistrict?.code) {
                        selectedDistrict = newDistrict
                        loadWards(newDistrict.code)
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
                if (position > 0) {
                    selectedWard = wardList[position - 1]
                } else {
                    selectedWard = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // --- SAVE LOGIC VÀ GEOLOCATION (Sử dụng MapService) ---
    private fun handleSaveAndShowMap() {
        val detailAddress = binding.etAddress.text.toString()

        if (selectedWard == null || selectedDistrict == null || selectedProvince == null || detailAddress.isBlank()) {
            Toast.makeText(requireContext(), "Vui lòng nhập chi tiết và chọn đủ Tỉnh, Huyện, Xã.", Toast.LENGTH_LONG).show()
            return
        }

        if (!isMapReady) {
            Toast.makeText(requireContext(), "Bản đồ chưa tải xong. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show()
            return
        }

        // Chuỗi tối ưu cho Nominatim (chỉ cấp hành chính)
        val optimalAddress = "${selectedWard!!.name}, ${selectedDistrict!!.name}, ${selectedProvince!!.name}, Vietnam"
        // Chuỗi đầy đủ cho tiêu đề Marker
        val fullAddressForMarker = "$detailAddress, $optimalAddress"

        Toast.makeText(requireContext(), "Đang tìm vị trí: $fullAddressForMarker", Toast.LENGTH_SHORT).show()

        // 2. Geocode và hiển thị trên Map
        geocodeAndDisplay(optimalAddress, fullAddressForMarker)
    }

// Trong hàm geocodeAndDisplay(searchAddress: String, markerTitle: String)

    private fun geocodeAndDisplay(searchAddress: String, markerTitle: String) {
        // ... (Log và check giữ nguyên) ...

        // SỬA LỖI TẠI ĐÂY: KHÔNG DÙNG LAMBDA, MÀ DÙNG OBJECT ANONYMOUS
        mapService.getCoordinatesFromAddress(searchAddress, object : MapService.OnLocationResult {

            override fun onLocationFound(lat: Double, lng: Double) {
                // Chuyển kết quả về luồng chính (UI Thread)
                activity?.runOnUiThread {
                    if (lat != 0.0 && lng != 0.0) {
                        val latLng = LatLng(lat, lng)

                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(latLng).title(markerTitle))
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                        Toast.makeText(requireContext(), "Đã đánh dấu vị trí trên bản đồ!", Toast.LENGTH_SHORT).show()
                        Log.d("MAP_SUCCESS", "Vị trí tìm thấy: $lat, $lng")
                    } else {
                        Log.w("MAP_ERROR", "MapService không tìm thấy tọa độ cho địa chỉ: $searchAddress")
                        Toast.makeText(requireContext(), "Không tìm thấy vị trí chính xác trên bản đồ. Thử nhập địa chỉ đơn giản hơn.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }


    // --- LOAD API FUNCTIONS (Giữ nguyên) ---

    private fun loadProvinces() {
        resetSpinner(binding.spinnerProvince, "Đang tải...", false)
        apiService.getProvinces().enqueue(object : Callback<List<ProvinceModel>> {
            override fun onResponse(call: Call<List<ProvinceModel>>, response: Response<List<ProvinceModel>>) {
                if (response.isSuccessful) {
                    provinceList = response.body() ?: emptyList()
                    if (provinceList.isNotEmpty()) {
                        val names = listOf("Select Province/City") + provinceList.map { it.name }
                        binding.spinnerProvince.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                        setSpinnerEnabled(binding.spinnerProvince, true)
                        Log.d("API_SUCCESS", "Tải Tỉnh thành công: ${provinceList.size}")
                    } else {
                        Log.e("API_ERROR", "Phản hồi thành công nhưng danh sách Tỉnh rỗng. Kiểm tra Data Model.")
                        resetSpinner(binding.spinnerProvince, "Lỗi dữ liệu", false)
                        Toast.makeText(requireContext(), "Lỗi cấu trúc dữ liệu Tỉnh/Thành phố", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e("API_ERROR", "Lỗi tải Tỉnh: ${response.code()} - ${response.errorBody()?.string()}")
                    resetSpinner(binding.spinnerProvince, "Lỗi server ${response.code()}", false)
                    Toast.makeText(requireContext(), "Lỗi tải Tỉnh/Thành phố", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<ProvinceModel>>, t: Throwable) {
                Log.e("API_ERROR", "Lỗi mạng tải Tỉnh: ${t.message}")
                resetSpinner(binding.spinnerProvince, "Lỗi kết nối", false)
                Toast.makeText(requireContext(), "Lỗi kết nối mạng: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadDistricts(provinceCode: String) {
        resetSpinner(binding.spinnerDistrict, "Đang tải...", false)
        resetSpinner(binding.spinnerWard, "Select Ward/Commune", false)

        apiService.getDistricts(provinceCode).enqueue(object : Callback<ProvinceDetailResponse> {
            override fun onResponse(call: Call<ProvinceDetailResponse>, response: Response<ProvinceDetailResponse>) {
                if (response.isSuccessful) {
                    districtList = response.body()?.districts ?: emptyList()
                    val names = listOf("Select District/Town") + districtList.map { it.name }
                    binding.spinnerDistrict.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    setSpinnerEnabled(binding.spinnerDistrict, true)
                    Log.d("API_SUCCESS", "Tải Huyện thành công: ${districtList.size}")
                } else {
                    Log.e("API_ERROR", "Lỗi tải Huyện: ${response.code()}")
                    resetSpinner(binding.spinnerDistrict, "Lỗi tải Huyện", false)
                    Toast.makeText(requireContext(), "Lỗi tải Quận/Huyện", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ProvinceDetailResponse>, t: Throwable) {
                Log.e("API_ERROR", "Lỗi mạng tải Huyện: ${t.message}")
                resetSpinner(binding.spinnerDistrict, "Lỗi kết nối", false)
                Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadWards(districtCode: String) {
        resetSpinner(binding.spinnerWard, "Đang tải...", false)
        apiService.getWards(districtCode).enqueue(object : Callback<DistrictModel> {
            override fun onResponse(call: Call<DistrictModel>, response: Response<DistrictModel>) {
                if (response.isSuccessful) {
                    wardList = response.body()?.wards ?: emptyList()
                    val names = listOf("Select Ward/Commune") + wardList.map { it.name }
                    binding.spinnerWard.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                    setSpinnerEnabled(binding.spinnerWard, true)
                    Log.d("API_SUCCESS", "Tải Xã thành công: ${wardList.size}")
                } else {
                    Log.e("API_ERROR", "Lỗi tải Xã: ${response.code()}")
                    resetSpinner(binding.spinnerWard, "Lỗi tải Xã", false)
                    Toast.makeText(requireContext(), "Lỗi tải Phường/Xã", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<DistrictModel>, t: Throwable) {
                Log.e("API_ERROR", "Lỗi mạng tải Xã: ${t.message}")
                resetSpinner(binding.spinnerWard, "Lỗi kết nối", false)
                Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}