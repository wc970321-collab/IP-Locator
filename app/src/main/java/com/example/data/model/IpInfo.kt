package com.example.data.model

import com.squareup.moshi.JsonClass

data class IpInfo(
    val ip: String,
    val ipVersion: String = "IPv4",
    val country: String = "Unknown",
    val countryCode: String = "UN",
    val region: String = "Unknown",
    val city: String = "Unknown",
    val zip: String = "N/A",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timezone: String = "UTC",
    val isp: String = "Unknown Carrier",
    val org: String = "Unknown Org",
    val asn: String = "N/A",
    val isProxy: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class IpApiCoResponse(
    val ip: String?,
    val version: String?,
    val city: String?,
    val region: String?,
    val region_code: String?,
    val country_name: String?,
    val country_code: String?,
    val postal: String?,
    val latitude: Double?,
    val longitude: Double?,
    val timezone: String?,
    val org: String?,
    val asn: String?
)

@JsonClass(generateAdapter = true)
data class FreeIpApiResponse(
    val ipVersion: Int?,
    val ipAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val countryName: String?,
    val countryCode: String?,
    val timeZone: String?,
    val zipCode: String?,
    val cityName: String?,
    val regionName: String?
)

@JsonClass(generateAdapter = true)
data class IpInfoIoResponse(
    val ip: String?,
    val city: String?,
    val region: String?,
    val country: String?,
    val loc: String?, // Contains lat,lon as "37.4223,-122.0847"
    val org: String?, // Contains ISP/ASN network name (e.g., "AS15169 Google LLC")
    val postal: String?,
    val timezone: String?
)
