package ru.echosystems.omni.database

import kotlinx.serialization.Serializable

@Serializable
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
    val departmentName: String?,
    val responsibleName: String?
)

@Serializable
data class EquipmentPage(
    val items: List<EquipmentRow>,
    val limit: Int,
    val offset: Long,
    val hasMore: Boolean
)

class EquipmentRepository {
    suspend fun findPage(
        search: String?,
        statusCode: String?,
        categoryId: Long?,
        locationId: Long?,
        employeeId: Long?,
        limit: Int,
        offset: Long
    ): EquipmentPage = dbQuery { connection ->
        val safeLimit = limit.coerceIn(1, 100)
        val safeOffset = offset.coerceAtLeast(0)
        val conditions = mutableListOf<String>()
        val parameters = mutableListOf<Any>()

        search?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            conditions += """
                (e.inventory_number ILIKE ? ESCAPE '!' OR e.serial_number ILIKE ? ESCAPE '!'
                 OR em.name ILIKE ? ESCAPE '!' OR m.name ILIKE ? ESCAPE '!'
                 OR (m.name || ' ' || em.name) ILIKE ? ESCAPE '!')
            """.trimIndent()
            val literal = query.replace("!", "!!").replace("%", "!%").replace("_", "!_")
            val pattern = "%$literal%"
            repeat(5) { parameters.add(pattern) }
        }

        statusCode?.trim()?.takeIf { it.isNotEmpty() }?.let {
            conditions += "es.code = ?"
            parameters += it
        }

        categoryId?.let {
            conditions += "ec.id = ?"
            parameters += it
        }

        locationId?.let {
            conditions += "e.location_id = ?"
            parameters += it
        }

        employeeId?.let {
            conditions += "active_assignment.employee_id = ?"
            parameters += it
        }

        val whereClause = if (conditions.isEmpty()) "" else
            "WHERE ${conditions.joinToString(" AND ")}"

        val sql = """
            SELECT e.id,
                   e.inventory_number,
                   e.serial_number,
                   em.name AS model_name,
                   m.name AS manufacturer_name,
                   ec.name AS category_name,
                   es.code AS status_code,
                   es.name AS status_name,
                   l.name AS location_name,
                   d.name AS department_name,
                   employee.full_name AS responsible_name
            FROM inventory.equipment e
            JOIN inventory.equipment_models em ON em.id = e.model_id
            JOIN inventory.manufacturers m ON m.id = em.manufacturer_id
            JOIN inventory.equipment_categories ec ON ec.id = em.category_id
            JOIN inventory.equipment_statuses es ON es.id = e.status_id
            LEFT JOIN inventory.locations l ON l.id = e.location_id
            LEFT JOIN inventory.departments d ON d.id = e.department_id
            LEFT JOIN LATERAL (
                SELECT ea.employee_id
                FROM inventory.equipment_assignments ea
                WHERE ea.equipment_id = e.id
                  AND ea.returned_at IS NULL
                LIMIT 1
            ) active_assignment ON TRUE
            LEFT JOIN inventory.employees employee
                ON employee.id = active_assignment.employee_id
            $whereClause
            ORDER BY e.id
            LIMIT ? OFFSET ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            var index = 1
            parameters.forEach { parameter -> statement.setObject(index++, parameter) }
            statement.setInt(index++, safeLimit + 1)
            statement.setLong(index, safeOffset)

            statement.executeQuery().use { rows ->
                val loaded = buildList {
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
                            departmentName = rows.getString("department_name"),
                            responsibleName = rows.getString("responsible_name")
                        )
                    )
                }

                EquipmentPage(
                    items = loaded.take(safeLimit),
                    limit = safeLimit,
                    offset = safeOffset,
                    hasMore = loaded.size > safeLimit
                )
            }
        }
    }
}
