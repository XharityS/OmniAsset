package ru.echosystems.omni.database

import kotlinx.serialization.Serializable

@Serializable
data class MoveEquipmentRequest(
    val locationId: Long? = null,
    val departmentId: Long? = null,
    val employeeId: Long? = null,
    val comment: String? = null
)

@Serializable
data class ChangeEquipmentStatusRequest(
    val statusCode: String,
    val comment: String? = null
)

class EquipmentActionsRepository {
    suspend fun moveEquipment(
        equipmentId: Long,
        request: MoveEquipmentRequest
    ) = dbQuery { connection ->
        connection.prepareStatement(
            """
            SELECT e.location_id, e.department_id, es.code
            FROM inventory.equipment e
            JOIN inventory.equipment_statuses es ON es.id = e.status_id
            WHERE e.id = ?
            FOR UPDATE
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, equipmentId)
            statement.executeQuery().use { result ->
                check(result.next()) { "Equipment with id=$equipmentId was not found" }
                check(result.getString("code") != "written_off") {
                    "Written-off equipment cannot be assigned or moved"
                }

                val oldLocationId = result.getObject("location_id") as Long?
                val oldDepartmentId = result.getObject("department_id") as Long?

                connection.prepareStatement(
                    """
                    UPDATE inventory.equipment_assignments
                    SET returned_at = NOW(), return_comment = ?
                    WHERE equipment_id = ? AND returned_at IS NULL
                    """.trimIndent()
                ).use { closeAssignment ->
                    closeAssignment.setString(1, request.comment)
                    closeAssignment.setLong(2, equipmentId)
                    closeAssignment.executeUpdate()
                }

                request.employeeId?.let { employeeId ->
                    connection.prepareStatement(
                        """
                        INSERT INTO inventory.equipment_assignments(equipment_id, employee_id, assigned_at)
                        VALUES (?, ?, NOW())
                        """.trimIndent()
                    ).use { assignment ->
                        assignment.setLong(1, equipmentId)
                        assignment.setLong(2, employeeId)
                        assignment.executeUpdate()
                    }
                }

                connection.prepareStatement(
                    """
                    UPDATE inventory.equipment
                    SET location_id = ?, department_id = ?, updated_at = NOW()
                    WHERE id = ?
                    """.trimIndent()
                ).use { updateEquipment ->
                    if (request.locationId == null) updateEquipment.setNull(1, java.sql.Types.BIGINT)
                    else updateEquipment.setLong(1, request.locationId)
                    if (request.departmentId == null) updateEquipment.setNull(2, java.sql.Types.BIGINT)
                    else updateEquipment.setLong(2, request.departmentId)
                    updateEquipment.setLong(3, equipmentId)
                    updateEquipment.executeUpdate()
                }

                connection.prepareStatement(
                    """
                    INSERT INTO inventory.equipment_movements(
                        equipment_id,
                        from_location_id,
                        to_location_id,
                        from_department_id,
                        to_department_id,
                        moved_at,
                        reason
                    )
                    VALUES (?, ?, ?, ?, ?, NOW(), ?)
                    """.trimIndent()
                ).use { movement ->
                    movement.setLong(1, equipmentId)
                    if (oldLocationId == null) movement.setNull(2, java.sql.Types.BIGINT)
                    else movement.setLong(2, oldLocationId)
                    if (request.locationId == null) movement.setNull(3, java.sql.Types.BIGINT)
                    else movement.setLong(3, request.locationId)
                    if (oldDepartmentId == null) movement.setNull(4, java.sql.Types.BIGINT)
                    else movement.setLong(4, oldDepartmentId)
                    if (request.departmentId == null) movement.setNull(5, java.sql.Types.BIGINT)
                    else movement.setLong(5, request.departmentId)
                    movement.setString(6, request.comment)
                    movement.executeUpdate()
                }
            }
        }
    }

    suspend fun changeStatus(
        equipmentId: Long,
        request: ChangeEquipmentStatusRequest
    ) = dbQuery { connection ->
        connection.prepareStatement(
            """
            SELECT es.code
            FROM inventory.equipment e
            JOIN inventory.equipment_statuses es ON es.id = e.status_id
            WHERE e.id = ?
            FOR UPDATE
            """.trimIndent()
        ).use { currentStatement ->
            currentStatement.setLong(1, equipmentId)
            currentStatement.executeQuery().use { result ->
                check(result.next()) { "Equipment with id=$equipmentId was not found" }
                val oldStatus = result.getString("code")

                connection.prepareStatement(
                    """
                    UPDATE inventory.equipment
                    SET status_id = (
                        SELECT id FROM inventory.equipment_statuses WHERE code = ?
                    ),
                    updated_at = NOW()
                    WHERE id = ?
                    """.trimIndent()
                ).use { update ->
                    update.setString(1, request.statusCode)
                    update.setLong(2, equipmentId)
                    check(update.executeUpdate() == 1) {
                        "Unknown equipment status: ${request.statusCode}"
                    }
                }

                connection.prepareStatement(
                    """
                    INSERT INTO inventory.audit_log(
                        action, entity_name, entity_id, old_data, new_data, created_at
                    )
                    VALUES (
                        'status_change',
                        'equipment',
                        ?,
                        jsonb_build_object('status', ?),
                        jsonb_build_object('status', ?, 'comment', ?),
                        NOW()
                    )
                    """.trimIndent()
                ).use { audit ->
                    audit.setString(1, equipmentId.toString())
                    audit.setString(2, oldStatus)
                    audit.setString(3, request.statusCode)
                    audit.setString(4, request.comment)
                    audit.executeUpdate()
                }
            }
        }
    }
}
