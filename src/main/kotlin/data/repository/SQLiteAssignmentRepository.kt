package data.repository

import core.domain.*
import core.port.CreditAssignmentRepositoryException
import core.port.ICreditAssignmentRepository
import data.repository.db.CreditAssignments
import data.repository.db.UserNames
import data.repository.db.upsert
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
            SchemaUtils.create(UserNames)
            SchemaUtils.create(CreditAssignments)
        }
    }

    override fun addAssignment(assignment: CreditAssignment) {
        try {
            transaction {
                // TODO: Store usernames more efficiently
                UserNames.upsert(UserNames.userId) {
                    it[userId] = assignment.assignee.userId.id
                    it[name] = assignment.assignee.name
                }
                CreditAssignments.insert {
                    it[chat] = assignment.chatId.id
                    it[assignee] = assignment.assignee.userId.id
                    it[value] = assignment.value.value
                }
            }
        } catch (e: Exception) {
            throw CreditAssignmentRepositoryException("Failed to execute transaction", e)
        }
    }

    override fun getChatTotalCredits(chatId: ChatId): Map<User, CreditValue> {
        try {
             return transaction {
                (CreditAssignments innerJoin UserNames)
                    .slice(CreditAssignments.value.sum(), CreditAssignments.assignee, UserNames.name)
                    .select { CreditAssignments.chat eq chatId.id }
                    .groupBy(CreditAssignments.assignee)
                    .associate { row ->
                        User(UserId(row[CreditAssignments.assignee]), row[UserNames.name]) to
                                CreditValue(row[CreditAssignments.value.sum()] ?: 0)
                    }
//                CreditAssignments
//                    .slice(CreditAssignments.value.sum(), CreditAssignments.assignee)
//                    .select { CreditAssignments.chat eq chatId.id }
//                    .groupBy(CreditAssignments.assignee)
//                    .associate { row ->
//                        UserId(row[CreditAssignments.assignee]) to
//                                CreditValue(row[CreditAssignments.value.sum()] ?: 0)
//                    }
            }
        } catch (e: Exception) {
            throw CreditAssignmentRepositoryException("Failed to execute query", e)
        }
    }
}