package ru.echosystems.omni.database

import kotlinx.serialization.Serializable

@Serializable
data class EmployeeUpsertRequest(
    val fullName: String,
    val position: String? = null,
    val department: String? = null,
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class EmployeeItem(
    val id: Long,
    val fullName: String,
    val position: String?,
    val department: String?,
    val phone: String?,
    val email: String?,
    val isActive: Boolean
)

@Serializable
data class UserItem(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val role: String,
    val isActive: Boolean
)

class DirectoryAdminRepository {
    suspend fun employees(): List<EmployeeItem> = dbQuery { c ->
        c.prepareStatement("SELECT id,full_name,position,department,phone,email,is_active FROM inventory.employees ORDER BY full_name")
            .use { s -> s.executeQuery().use { r ->
                buildList { while (r.next()) add(EmployeeItem(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getBoolean(7))) }
            }}
    }

    suspend fun createEmployee(request: EmployeeUpsertRequest): Long = dbQuery { c ->
        c.prepareStatement("INSERT INTO inventory.employees(full_name,position,department,phone,email) VALUES(?,?,?,?,?) RETURNING id")
            .use { s ->
                s.setString(1,request.fullName);s.setString(2,request.position);s.setString(3,request.department)
                s.setString(4,request.phone);s.setString(5,request.email)
                s.executeQuery().use { r -> r.next(); r.getLong(1) }
            }
    }

    suspend fun updateEmployee(id: Long, request: EmployeeUpsertRequest) = dbQuery { c ->
        c.prepareStatement("UPDATE inventory.employees SET full_name=?,position=?,department=?,phone=?,email=? WHERE id=?")
            .use { s ->
                s.setString(1,request.fullName);s.setString(2,request.position);s.setString(3,request.department)
                s.setString(4,request.phone);s.setString(5,request.email);s.setLong(6,id)
                check(s.executeUpdate() == 1) { "Employee not found" }
            }
    }

    suspend fun blockEmployee(id: Long) = dbQuery { c ->
        c.prepareStatement("UPDATE inventory.employees SET is_active=FALSE WHERE id=?").use { s ->
            s.setLong(1,id); check(s.executeUpdate() == 1) { "Employee not found" }
        }
    }

    suspend fun users(): List<UserItem> = dbQuery { c ->
        c.prepareStatement("SELECT id,username,full_name,email,role,is_active FROM inventory.app_users ORDER BY username")
            .use { s -> s.executeQuery().use { r ->
                buildList { while (r.next()) add(UserItem(r.getObject(1).toString(),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getBoolean(6))) }
            }}
    }
}
