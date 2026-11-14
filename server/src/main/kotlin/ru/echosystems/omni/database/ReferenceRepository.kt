package ru.echosystems.omni.database

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceItem(val id: Long, val name: String)

class ReferenceRepository {
    suspend fun list(type: String): List<ReferenceItem> = dbQuery { c ->
        val table = when (type) {
            "categories" -> "equipment_categories"
            "manufacturers" -> "manufacturers"
            "locations" -> "locations"
            "departments" -> "departments"
            "employees" -> "employees"
            "statuses" -> "equipment_statuses"
            else -> error("Unknown reference: $type")
        }
        c.prepareStatement("SELECT id, name FROM inventory.$table ORDER BY name").use { s ->
            s.executeQuery().use { r ->
                buildList { while (r.next()) add(ReferenceItem(r.getLong(1), r.getString(2))) }
            }
        }
    }
}
