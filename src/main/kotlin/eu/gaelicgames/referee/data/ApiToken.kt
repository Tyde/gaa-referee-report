package eu.gaelicgames.referee.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object ApiTokens : LongIdTable() {
    val name = varchar("name", 100)
    val tokenHash = varchar("token_hash", 64).index()
    val user = reference("user", Users)
    val createdAt = datetime("created_at")
    val expiresAt = datetime("expires_at").nullable()
    val revoked = bool("revoked").default(false)
    val lastUsedAt = datetime("last_used_at").nullable()
}

class ApiToken(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ApiToken>(ApiTokens)
    var name by ApiTokens.name
    var tokenHash by ApiTokens.tokenHash
    var user by User referencedOn ApiTokens.user
    var createdAt by ApiTokens.createdAt
    var expiresAt by ApiTokens.expiresAt
    var revoked by ApiTokens.revoked
    var lastUsedAt by ApiTokens.lastUsedAt
}
