package data.repository.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Index
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * Upsert is not implemented in Exposed, so use solution:
 * https://github.com/JetBrains/Exposed/issues/167#issuecomment-408922484
 * TODO: Refactor
 */
class UpsertStatement<Key : Any>(table: Table, conflictColumn: Column<*>? = null, conflictIndex: Index? = null)
    : InsertStatement<Key>(table, false) {
    val indexName: String
    val indexColumns: List<Column<*>>

    init {
        when {
            conflictIndex != null -> {
                indexName = conflictIndex.indexName
                indexColumns = conflictIndex.columns
            }
            conflictColumn != null -> {
                indexName = conflictColumn.name
                indexColumns = listOf(conflictColumn)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun prepareSQL(transaction: Transaction) = buildString {
        append(super.prepareSQL(transaction))
        append(" ON CONFLICT(")
        append(indexName)
        append(") DO UPDATE SET ")
        values.keys.filter { it !in indexColumns }.joinTo(this) { "${transaction.identity(it)}=EXCLUDED.${transaction.identity(it)}" }
    }

}

inline fun <T : Table> T.upsert1(conflictColumn: Column<*>? = null, conflictIndex: Index? = null, body: T.(UpsertStatement<Number>) -> Unit) =
    UpsertStatement<Number>(this, conflictColumn, conflictIndex).apply {
        body(this)
        execute(TransactionManager.current())
    }

fun Table.indexR(customIndexName: String? = null, isUnique: Boolean = false, vararg columns: Column<*>): Index {
    val index = Index(columns.toList(), isUnique, customIndexName)
    (indices as MutableList).add(index)
    return index
}

fun Table.uniqueIndexR(customIndexName: String? = null, vararg columns: Column<*>): Index = indexR(customIndexName, true, *columns)