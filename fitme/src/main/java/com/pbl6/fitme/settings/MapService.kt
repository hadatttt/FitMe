package com.pbl6.fitme.settings

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class MapService {
    interface OnLocationResult {
        fun onLocationFound(lat: Double, lng: Double)
    }

    interface OnTravelTimeResult {
        fun onTimeResult(timeStr: String?)
    }

    interface OnRouteCoordinatesResult {
        fun onRouteCoordinates(coordinates: JSONArray?)
    }

    /**
     * Lấy tọa độ tuyến đường (route coordinates) giữa hai điểm sử dụng OSRM.
     * Tọa độ được trả về dưới dạng GeoJSON.
     */
    fun getRouteCoordinatesOSRM(
        userLat: Double,
        userLng: Double,
        shopLat: Double,
        shopLng: Double,
        callback: OnRouteCoordinatesResult
    ) {
        Thread(Runnable {
            try {
                // OSRM sử dụng tọa độ theo thứ tự: kinh độ, vĩ độ (lng, lat)
                val url = String.format(
                    Locale.US,
                    "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                    userLng, userLat, shopLng, shopLat
                )

                Log.d("OSRM_URL", url)
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117 Safari/537.36"
                    )
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        callback.onRouteCoordinates(coordinates)
                    } else {
                        callback.onRouteCoordinates(null)
                    }
                } else {
                    callback.onRouteCoordinates(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onRouteCoordinates(null)
            }
        }).start()
    }

    /**
     * Chuyển đổi địa chỉ văn bản thành tọa độ (lat, lng) sử dụng Nominatim API.
     */
    fun getCoordinatesFromAddress(address: String, callback: OnLocationResult) {
        Thread(Runnable {
            try {
                val url = ("https://nominatim.openstreetmap.org/search?q="
                        + URLEncoder.encode(address, "UTF-8") + "&format=json")
                Log.d("NOMINATIM_URL", url)
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url) // Bắt buộc phải có User-Agent cho Nominatim
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117 Safari/537.36"
                    )
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body!!.string())
                    if (jsonArray.length() > 0) {
                        val location = jsonArray.getJSONObject(0)
                        val lat = location.getDouble("lat")
                        val lon = location.getDouble("lon")
                        callback.onLocationFound(lat, lon)
                    } else {
                        callback.onLocationFound(0.0, 0.0) // Không tìm thấy
                    }
                } else {
                    callback.onLocationFound(0.0, 0.0) // Lỗi HTTP
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onLocationFound(0.0, 0.0)
            }
        }).start()
    }

    /**
     * Lấy thời gian di chuyển giữa hai điểm sử dụng OSRM.
     */
    fun getTravelTimeOSRM(
        userLat: Double,
        userLng: Double,
        shopLat: Double,
        shopLng: Double,
        callback: OnTravelTimeResult
    ) {
        Thread(Runnable {
            try {
                // OSRM sử dụng tọa độ theo thứ tự: kinh độ, vĩ độ (lng, lat)
                val url = String.format(
                    Locale.US,
                    "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
                    userLng, userLat, shopLng, shopLat
                )


                Log.d("OSRM_TIME_URL", url)
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FastFood/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val durationSeconds = route.getDouble("duration")

                        val hours = (durationSeconds / 3600).toInt()
                        val minutes = ((durationSeconds % 3600) / 60).toInt()

                        val timeStr =
                            if (hours > 0) (hours.toString() + " giờ " + minutes + " phút") else (minutes.toString() + " phút")
                        callback.onTimeResult(timeStr)
                    } else {
                        callback.onTimeResult("--")
                    }
                } else {
                    callback.onTimeResult("--")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onTimeResult("--")
            }
        }).start()
    }
}