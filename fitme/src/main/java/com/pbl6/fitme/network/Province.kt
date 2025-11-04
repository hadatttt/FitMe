// File: com/pbl6/fitme/network/DataModels.kt (hoặc giữ nguyên Province.kt và thêm các lớp khác)

package com.pbl6.fitme.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET

// --- Phường / Xã / Thị trấn ---
data class Ward(
    @SerializedName("code")
    val code: String,

    @SerializedName("name")
    val name: String,

    // Các trường khác như codename, division_type có thể được thêm vào nếu cần
)

// --- Quận / Huyện / Thị xã ---
data class District(
    @SerializedName("code")
    val code: String,

    @SerializedName("name")
    val name: String,

    // API depth=2 thường không trả về wards, nhưng nếu API trả về wards thì thêm vào đây
    @SerializedName("wards")
    val wards: List<Ward>?
)

// --- Tỉnh / Thành phố ---
data class Province(
    @SerializedName("code")
    val code: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("codename")
    val codename: String,

    @SerializedName("division_type")
    val divisionType: String,

    // Thêm danh sách Quận/Huyện, cần thiết khi gọi API với depth > 1
    @SerializedName("districts")
    val districts: List<District>?
)
interface VietnamApiService {
    /**
     * Endpoint để lấy danh sách tất cả Tỉnh/Thành phố kèm theo Quận/Huyện/Thị xã.
     * Sử dụng depth=2: /api/p/?depth=2
     */
    @GET("p/?depth=2")
    fun getProvincesWithDistricts(): Call<List<Province>>

    /* // Nếu bạn cần gọi riêng Quận/Huyện theo mã Tỉnh, bạn sẽ cần hàm này:
    @GET("p/{provinceCode}?depth=2")
    fun getProvinceDetail(@Path("provinceCode") code: String): Call<Province>
    */

    /*
    // Nếu bạn cần lấy danh sách Phường/Xã theo mã Huyện/Quận:
    @GET("d/{districtCode}?depth=2")
    fun getDistrictDetail(@Path("districtCode") code: String): Call<District>
    */
}