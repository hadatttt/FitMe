package com.pbl6.fitme.network

import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.VNPayResponse
import com.pbl6.fitme.network.BaseResponse
import retrofit2.Call
import retrofit2.http.*

interface OrderApiService {
    @POST("orders")
    fun createOrder(
        @Header("Authorization") token: String,
        @Body order: Order
    ): Call<BaseResponse<Order>>

    @GET("payment/vn-pay")
    fun createVNPayPayment(
        @Header("Authorization") token: String,
        @Query("orderId") orderId: String,
        @Query("userEmail") userEmail: String
    ): Call<BaseResponse<VNPayResponse>>

    @GET("payment/vn-pay-callback")
    fun handleVNPayCallback(
        @Query("vnp_response_code") responseCode: String,
        @Query("vnp_order_info") orderInfo: String,
        @Query("vnp_transaction_status") transactionStatus: String,
        @Query("vnp_transaction_no") transactionNo: String? = null,
        @Query("vnp_pay_date") payDate: String? = null
    ): Call<BaseResponse<VNPayResponse>>

    @GET("orders/{id}")
    fun getOrderById(
        @Header("Authorization") token: String,
        @Path("id") orderId: String
    ): Call<BaseResponse<Order>>

    @GET("orders/user")
    fun getOrdersByUser(
        @Header("Authorization") token: String,
        @Query("email") email: String,
        @Query("status") status: String? = null
    ): Call<BaseResponse<List<Order>>>

    @PUT("orders/{id}/status")
    fun updateOrderStatus(
        @Header("Authorization") token: String,
        @Path("id") orderId: String,
        @Query("status") status: String
    ): Call<BaseResponse<Order>>
}