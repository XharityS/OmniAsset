package ru.echosystems.omni

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform