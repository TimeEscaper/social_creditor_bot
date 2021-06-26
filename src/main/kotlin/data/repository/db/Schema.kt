package data.repository.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object CreditAssignments: Table() {
    val chat: Column<Long> = long("chat")
    val assignee: Column<Long> = long("assignee")
    val value: Column<Int> = integer("value")
}