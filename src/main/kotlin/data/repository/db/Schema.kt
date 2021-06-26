package data.repository.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Index
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

object UserNames: Table() {
    val userId = long("user_id").uniqueIndex()
    val name = varchar("name", length = 100)

    override val primaryKey = PrimaryKey(userId)
}

object CreditAssignments: Table() {
    val chat = long("chat")
    val assignee = long("assignee").references(UserNames.userId)
    val value= integer("value")
}
