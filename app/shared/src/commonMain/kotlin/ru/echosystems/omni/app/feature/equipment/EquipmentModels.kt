package ru.echosystems.omni.app.feature.equipment

import kotlinx.serialization.Serializable

@Serializable
data class EquipmentRow(
    val id: Long,
    val inventoryNumber: String,
    val serialNumber: String? = null,
    val modelName: String,
    val manufacturerName: String,
    val categoryName: String,
    val statusCode: String,
    val statusName: String,
    val locationName: String? = null,
    val departmentName: String? = null,
    val responsibleName: String? = null
)

@Serializable
data class EquipmentPage(
    val items: List<EquipmentRow>,
    val limit: Int,
    val offset: Long,
    val hasMore: Boolean
)
