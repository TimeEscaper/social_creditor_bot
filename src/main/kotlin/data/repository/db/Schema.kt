package data.repository.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Index
import org.jetbrains.exposed.sql.Table

/**
 * Needed for explicit creation of indices
 * https://github.com/JetBrains/Exposed/issues/167#issuecomment-408922484
 * TODO: Refactor
 */
fun Table.indexR(customIndexName: String? = null, isUnique: Boolean = false, vararg columns: Column<*>): Index {
    val index = Index(columns.toList(), isUnique, customIndexName)
    (indices as MutableList).add(index)
    return index
}

fun Table.uniqueIndexR(customIndexName: String? = null, vararg columns: Column<*>): Index = indexR(customIndexName, true, *columns)

object UserNames: Table() {
    val userId = long("user_id").uniqueIndex()
    val name = varchar("name", length = 100)

    override val primaryKey = PrimaryKey(userId)
}

object Credits: Table() {
    val chat = long("chat")
    val assignee = long("assignee").references(UserNames.userId)
    val value= integer("value")

    val chatAssigneeUnique = uniqueIndexR("chat_assignee_unique_idx", chat, assignee)
    override val primaryKey = PrimaryKey(chat, assignee)
}
