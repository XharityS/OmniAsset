package ru.echosystems.omni.database

data class EquipmentRow(
    val id: Long,
    val inventoryNumber: String,
    val serialNumber: String?,
    val modelName: String,
    val manufacturerName: String,
    val categoryName: String,
    val statusCode: String,
    val statusName: String,
    val locationName: String?,
    val departmentName: String?
)

class EquipmentRepository {
    suspend fun findPage(limit: Int, offset: Long): List<EquipmentRow> = dbQuery { connection ->
        connection.prepareStatement(
            """
            SELECT e.id,
                   e.inventory_number,
                   e.serial_number,
                   em.name AS model_name,
                   m.name AS manufacturer_name,
                   ec.name AS category_name,
                   es.code AS status_code,
                   es.name AS status_name,
                   l.name AS location_name,
                   d.name AS department_name
            FROM inventory.equipment e
            JOIN inventory.equipment_models em ON em.id = e.model_id
            JOIN inventory.manufacturers m ON m.id = em.manufacturer_id
            JOIN inventory.equipment_categories ec ON ec.id = em.category_id
            JOIN inventory.equipment_statuses es ON es.id = e.status_id
            LEFT JOIN inventory.locations l ON l.id = e.location_id
            LEFT JOIN inventory.departments d ON d.id = e.department_id
            ORDER BY e.id
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, limit.coerceIn(1, 100))
            statement.setLong(2, offset.coerceAtLeast(0))
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        EquipmentRow(
                            id = rows.getLong("id"),
                            inventoryNumber = rows.getString("inventory_number"),
                            serialNumber = rows.getString("serial_number"),
                            modelName = rows.getString("model_name"),
                            manufacturerName = rows.getString("manufacturer_name"),
                            categoryName = rows.getString("category_name"),
                            statusCode = rows.getString("status_code"),
                            statusName = rows.getString("status_name"),
                            locationName = rows.getString("location_name"),
                            departmentName = rows.getString("department_name")
                        )
                    )
                }
            }
        }
    }
}
