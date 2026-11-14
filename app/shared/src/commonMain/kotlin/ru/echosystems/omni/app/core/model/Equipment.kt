package ru.echosystems.omni.app.core.model

data class Equipment(
    val id: Long,
    val inventoryNumber: String,
    val serialNumber: String?,
    val modelName: String,
    val manufacturerName: String,
    val categoryName: String,
    val statusCode: String,
    val statusName: String,
    val locationName: String?,
    val departmentName: String?,
    val responsibleName: String?
)

data class EquipmentPage(
    val items: List<Equipment>,
    val limit: Int,
    val offset: Long,
    val hasMore: Boolean
)
