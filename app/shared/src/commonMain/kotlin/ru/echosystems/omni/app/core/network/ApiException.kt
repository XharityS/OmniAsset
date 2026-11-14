package ru.echosystems.omni.app.core.network

class ApiException(
    val statusCode: Int,
    val userMessage: String,
    val responseBody: String
) : RuntimeException("HTTP $statusCode: $responseBody")
