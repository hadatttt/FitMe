package com.pbl6.fitme.model

enum class OrderStatus(val value: String) {
    CONFIRMED("confirmed"),
    PROCESSING("processing"),
    SHIPPED("shipped"),
    DELIVERED("delivered"),
    CANCELLED("cancelled"),
    PENDING("pending")
}