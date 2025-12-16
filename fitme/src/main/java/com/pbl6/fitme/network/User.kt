package com.pbl6.fitme.network

import com.google.gson.annotations.SerializedName


data class UserDetailResponse(
    val code: Int,
    val message: String,
    val result: UserResult?
)

data class UserResult(
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String?,  // nếu JSON là "fullName"
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("dateOfBirth") val dateOfBirth: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("roles") val roles: List<Role>?
)

data class Role(
    val roleName: String,
    val description: String?,
    val permissions: List<Permission>?
)

data class Permission(
    val name: String,
    val description: String?
)