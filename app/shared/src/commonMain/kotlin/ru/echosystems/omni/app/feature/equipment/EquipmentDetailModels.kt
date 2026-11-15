package ru.echosystems.omni.app.feature.equipment

import kotlinx.serialization.Serializable

@Serializable
data class EquipmentDetails(
    val id: Long,
    val inventoryNumber: String,
    val serialNumber: String? = null,
    val modelId: Long,
    val modelName: String,
    val manufacturerName: String,
    val categoryName: String,
    val statusCode: String,
    val statusName: String,
    val locationId: Long? = null,
    val locationName: String? = null,
    val departmentId: Long? = null,
    val departmentName: String? = null,
    val responsibleName: String? = null,
    val purchaseDate: String? = null,
    val commissioningDate: String? = null,
    val warrantyUntil: String? = null,
    val purchasePrice: String? = null,
    val notes: String? = null,
    val statusId: Long? = null
)

@Serializable
data class EquipmentHistoryItem(val type: String, val date: String, val comment: String? = null)

@Serializable
data class ChangeEquipmentStatusRequest(val statusCode: String, val comment: String? = null)
