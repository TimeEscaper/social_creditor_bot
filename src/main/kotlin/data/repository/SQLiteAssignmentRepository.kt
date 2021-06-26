package data.repository

import core.domain.ChatId
import core.domain.CreditAssignment
import core.domain.CreditValue
import core.domain.UserId
import core.port.CreditAssignmentRepositoryException
import core.port.ICreditAssignmentRepository
import data.repository.db.CreditAssignments
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

class SQLiteAssignmentRepository(dbPath: String): ICreditAssignmentRepository {

    private val dbConnection: Database = Database.connect("jdbc:sqlite:${dbPath}", "org.sqlite.JDBC")

    init {
        TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_SERIALIZABLE
        dbConnection.useNestedTransactions = true
        transaction {
            SchemaUtils.create(CreditAssignments)
        }
    }

    override fun addAssignment(assignment: CreditAssignment) {
        try {
            transaction {
                CreditAssignments.insert {
                    it[chat] = assignment.chatId.id
                    it[assignee] = assignment.assignee.id
                    it[value] = assignment.value.value
                }
            }
        } catch (e: Exception) {
            throw CreditAssignmentRepositoryException("Failed to execute transaction", e)
        }
    }

    override fun getChatAssignments(chatId: ChatId): Map<UserId, CreditValue> {
        try {
             return transaction {
                CreditAssignments
                    .slice(CreditAssignments.value.sum(), CreditAssignments.assignee)
                    .select { CreditAssignments.chat eq chatId.id }
                    .groupBy(CreditAssignments.assignee)
                    .associate { row ->
                        UserId(row[CreditAssignments.assignee]) to
                                CreditValue(row[CreditAssignments.value.sum()] ?: 0)
                    }
            }
        } catch (e: Exception) {
            throw CreditAssignmentRepositoryException("Failed to execute query", e)
        }
    }
}