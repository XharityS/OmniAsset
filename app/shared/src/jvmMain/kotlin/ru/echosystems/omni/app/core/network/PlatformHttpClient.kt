package ru.echosystems.omni.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun platformHttpClient(): HttpClient = HttpClient(CIO)
