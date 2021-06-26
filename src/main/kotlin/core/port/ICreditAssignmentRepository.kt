package core.port

import core.domain.ChatId
import core.domain.CreditAssignment
import core.domain.UserId

interface ICreditAssignmentRepository {

    @Throws(CreditAssignmentRepositoryException::class)
    fun addAssignment(assignment: CreditAssignment)

    @Throws(CreditAssignmentRepositoryException::class)
    fun getChatAssignments(chatId: ChatId): Map<UserId, ChatId>
}