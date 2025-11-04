package com.pbl6.fitme.network.province;

import java.util.List;

public class DistrictModel {
    private String code;
    private String name;
    private List<WardModel> wards;

    public String getCode() { return code; }
    public String getName() { return name; }
    public List<WardModel> getWards() { return wards; }
}
