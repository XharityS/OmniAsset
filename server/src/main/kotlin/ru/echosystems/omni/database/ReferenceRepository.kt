package ru.echosystems.omni.database

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceItem(val id: Long, val name: String)

class ReferenceRepository {
    suspend fun list(type: String): List<ReferenceItem> = dbQuery { c ->
        val sql = if (type == "models") {
            """
            SELECT model.id, manufacturer.name || ' ' || model.name AS name
            FROM inventory.equipment_models model
            JOIN inventory.manufacturers manufacturer ON manufacturer.id = model.manufacturer_id
            ORDER BY name, model.id
            """.trimIndent()
        } else if (type == "employees") {
            "SELECT id, full_name FROM inventory.employees WHERE is_active ORDER BY full_name, id"
        } else {
            val table = when (type) {
                "categories" -> "equipment_categories"
                "manufacturers" -> "manufacturers"
                "locations" -> "locations"
                "departments" -> "departments"
                "statuses" -> "equipment_statuses"
                else -> error("Unknown reference: $type")
            }
            "SELECT id, name FROM inventory.$table ORDER BY name, id"
        }
        c.prepareStatement(sql).use { s ->
            s.executeQuery().use { r ->
                buildList { while (r.next()) add(ReferenceItem(r.getLong(1), r.getString(2))) }
            }
        }
    }
}
