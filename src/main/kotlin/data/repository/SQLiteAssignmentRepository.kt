package data.repository

import core.domain.*
import core.port.CreditAssignmentRepositoryException
import core.port.ICreditAssignmentRepository
import data.repository.db.Credits
import data.repository.db.UserNames
import net.dzikoysk.exposed.upsert.upsert
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
            SchemaUtils.create(Credits)
        }
    }

    override fun addAssignment(assignment: CreditAssignment) {
        try {
            transaction {
                // TODO: Store usernames more efficiently
                UserNames.upsert(UserNames.userId, insertBody = {
                    it[userId] = assignment.assignee.userId.id
                    it[name] = assignment.assignee.name
                }, updateBody = {
                    with(SqlExpressionBuilder) {
                        it.update(name, name.apply { assignment.assignee.name })
                    }
                })
                Credits.upsert(conflictIndex = Credits.chatAssigneeUnique, insertBody = {
                    it[chat] = assignment.chatId.id
                    it[assignee] = assignment.assignee.userId.id
                    it[value] = assignment.value.value
                }, updateBody = {
                    with(SqlExpressionBuilder) {
                        it.update(value, value + assignment.value.value)
                    }
                })
            }
        } catch (e: Exception) {
            throw CreditAssignmentRepositoryException("Failed to execute transaction", e)
        }
    }

    override fun getChatTotalCredits(chatId: ChatId): Map<User, CreditValue> {
        try {
             return transaction {
                (Credits innerJoin UserNames)
                    .slice(Credits.value, Credits.assignee, UserNames.name)
                    .select { Credits.chat eq chatId.id }
                    .groupBy(Credits.assignee)
                    .associate { row ->
                        User(UserId(row[Credits.assignee]), row[UserNames.name]) to
                                CreditValue(row[Credits.value])
                    }
            }
        } catch (e: Exception) {
            throw CreditAssignmentRepositoryException("Failed to execute query", e)
        }
    }
}