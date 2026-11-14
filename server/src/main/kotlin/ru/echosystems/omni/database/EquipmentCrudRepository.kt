package ru.echosystems.omni.database

import kotlinx.serialization.Serializable
import java.sql.Types

@Serializable
data class EquipmentDetails(
    val id: Long,
    val inventoryNumber: String,
    val serialNumber: String?,
    val modelId: Long,
    val modelName: String,
    val manufacturerName: String,
    val categoryName: String,
    val statusCode: String,
    val statusName: String,
    val locationId: Long?,
    val locationName: String?,
    val departmentId: Long?,
    val departmentName: String?,
    val responsibleName: String?,
    val purchaseDate: String?,
    val commissioningDate: String?,
    val warrantyUntil: String?,
    val purchasePrice: String?,
    val notes: String?
)

@Serializable
data class EquipmentUpsertRequest(
    val inventoryNumber: String,
    val serialNumber: String? = null,
    val modelId: Long,
    val statusId: Int,
    val locationId: Long? = null,
    val departmentId: Long? = null,
    val purchaseDate: String? = null,
    val commissioningDate: String? = null,
    val purchasePrice: String? = null,
    val warrantyUntil: String? = null,
    val specifications: String = "{}",
    val notes: String? = null
)

@Serializable
data class EquipmentHistoryItem(
    val type: String,
    val date: String,
    val comment: String?
)

@Serializable
data class StatusSummary(
    val statusCode: String,
    val statusName: String,
    val amount: Long
)

class EquipmentCrudRepository {
    suspend fun findById(id: Long): EquipmentDetails? = dbQuery { c ->
        c.prepareStatement("""
            SELECT e.id,e.inventory_number,e.serial_number,e.model_id,em.name model_name,
                   m.name manufacturer_name,ec.name category_name,es.code status_code,
                   es.name status_name,e.location_id,l.name location_name,e.department_id,
                   d.name department_name,emp.full_name responsible_name,e.purchase_date,
                   e.commissioning_date,e.warranty_until,e.purchase_price,e.notes
            FROM inventory.equipment e
            JOIN inventory.equipment_models em ON em.id=e.model_id
            JOIN inventory.manufacturers m ON m.id=em.manufacturer_id
            JOIN inventory.equipment_categories ec ON ec.id=em.category_id
            JOIN inventory.equipment_statuses es ON es.id=e.status_id
            LEFT JOIN inventory.locations l ON l.id=e.location_id
            LEFT JOIN inventory.departments d ON d.id=e.department_id
            LEFT JOIN LATERAL (SELECT employee_id FROM inventory.equipment_assignments
              WHERE equipment_id=e.id AND returned_at IS NULL LIMIT 1) a ON TRUE
            LEFT JOIN inventory.employees emp ON emp.id=a.employee_id
            WHERE e.id=?
        """.trimIndent()).use { s ->
            s.setLong(1,id)
            s.executeQuery().use { r ->
                if (!r.next()) return@dbQuery null
                EquipmentDetails(r.getLong("id"),r.getString("inventory_number"),
                    r.getString("serial_number"),r.getLong("model_id"),r.getString("model_name"),
                    r.getString("manufacturer_name"),r.getString("category_name"),
                    r.getString("status_code"),r.getString("status_name"),
                    r.getObject("location_id") as Long?,r.getString("location_name"),
                    r.getObject("department_id") as Long?,r.getString("department_name"),
                    r.getString("responsible_name"),r.getString("purchase_date"),
                    r.getString("commissioning_date"),r.getString("warranty_until"),
                    r.getBigDecimal("purchase_price")?.toPlainString(),r.getString("notes"))
            }
        }
    }

    suspend fun create(request: EquipmentUpsertRequest): Long = dbQuery { c ->
        c.prepareStatement("""
            INSERT INTO inventory.equipment(inventory_number,serial_number,model_id,status_id,
              location_id,department_id,purchase_date,commissioning_date,purchase_price,
              warranty_until,specifications,notes)
            VALUES(?,?, ?, ?, ?, ?, NULLIF(?, '')::date, NULLIF(?, '')::date,
              NULLIF(?, '')::numeric, NULLIF(?, '')::date, ?::jsonb, ?) RETURNING id
        """.trimIndent()).use { s ->
            s.setString(1,request.inventoryNumber); s.setString(2,request.serialNumber)
            s.setLong(3,request.modelId); s.setInt(4,request.statusId)
            if(request.locationId==null)s.setNull(5,Types.BIGINT) else s.setLong(5,request.locationId)
            if(request.departmentId==null)s.setNull(6,Types.BIGINT) else s.setLong(6,request.departmentId)
            s.setString(7,request.purchaseDate);s.setString(8,request.commissioningDate)
            s.setString(9,request.purchasePrice);s.setString(10,request.warrantyUntil)
            s.setString(11,request.specifications);s.setString(12,request.notes)
            s.executeQuery().use { r -> r.next(); r.getLong(1) }
        }
    }

    suspend fun update(id: Long, request: EquipmentUpsertRequest) = dbQuery { c ->
        c.prepareStatement("""
            UPDATE inventory.equipment SET inventory_number=?,serial_number=?,model_id=?,status_id=?,
              location_id=?,department_id=?,purchase_date=NULLIF(?, '')::date,
              commissioning_date=NULLIF(?, '')::date,purchase_price=NULLIF(?, '')::numeric,
              warranty_until=NULLIF(?, '')::date,specifications=?::jsonb,notes=?,updated_at=NOW()
            WHERE id=?
        """.trimIndent()).use { s ->
            s.setString(1,request.inventoryNumber);s.setString(2,request.serialNumber)
            s.setLong(3,request.modelId);s.setInt(4,request.statusId)
            if(request.locationId==null)s.setNull(5,Types.BIGINT) else s.setLong(5,request.locationId)
            if(request.departmentId==null)s.setNull(6,Types.BIGINT) else s.setLong(6,request.departmentId)
            s.setString(7,request.purchaseDate);s.setString(8,request.commissioningDate)
            s.setString(9,request.purchasePrice);s.setString(10,request.warrantyUntil)
            s.setString(11,request.specifications);s.setString(12,request.notes);s.setLong(13,id)
            check(s.executeUpdate()==1){"Equipment not found: $id"}
        }
    }

    suspend fun history(id: Long): List<EquipmentHistoryItem> = dbQuery { c ->
        c.prepareStatement("""
            SELECT 'movement' type,moved_at date,reason comment FROM inventory.equipment_movements
            WHERE equipment_id=? UNION ALL
            SELECT action,created_at,COALESCE(new_data->>'comment','') FROM inventory.audit_log
            WHERE entity_name='equipment' AND entity_id=? ORDER BY date DESC
        """.trimIndent()).use { s -> s.setLong(1,id);s.setString(2,id.toString());s.executeQuery().use { r ->
            buildList { while(r.next()) add(EquipmentHistoryItem(r.getString(1),r.getString(2),r.getString(3))) }
        }}
    }

    suspend fun summary(): List<StatusSummary> = dbQuery { c ->
        c.prepareStatement("""
            SELECT es.code,es.name,COUNT(e.id) FROM inventory.equipment_statuses es
            LEFT JOIN inventory.equipment e ON e.status_id=es.id
            GROUP BY es.id ORDER BY es.id
        """.trimIndent()).use { s -> s.executeQuery().use { r ->
            buildList { while(r.next()) add(StatusSummary(r.getString(1),r.getString(2),r.getLong(3))) }
        }}
    }
}
