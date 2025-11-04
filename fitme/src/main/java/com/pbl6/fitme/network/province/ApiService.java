// File: com.pbl6.fitme.network.province.ApiService.java
package com.pbl6.fitme.network.province;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    @GET("api/v1/p")
    Call<List<ProvinceModel>> getProvinces();

    @GET("api/v1/p/{code}?depth=2")
    Call<ProvinceDetailResponse> getDistricts(@Path("code") String provinceCode);

    @GET("api/v1/d/{code}?depth=2")
    Call<DistrictModel> getWards(@Path("code") String districtCode);
}