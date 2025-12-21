package com.pbl6.fitme.untils

class AppConstrain {
    companion object {
        // 1. Chỉ cần sửa IP và Port ở dòng này là xong
        private const val SERVER_IP = "10.48.170.90:8080"

        // 2. Dùng cho Retrofit (kết thúc bằng /)
        // Kết quả: http://10.48.170.90:8080/api/
        const val BASE_URL = "http://$SERVER_IP/api/"

        // 3. Dùng cho load ảnh (không có /api)
        // Kết quả: http://10.48.170.90:8080
        const val DOMAIN_URL = "http://$SERVER_IP"
    }
}