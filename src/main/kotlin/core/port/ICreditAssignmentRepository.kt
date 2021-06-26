package core.port

import core.domain.*

interface ICreditAssignmentRepository {

    @Throws(CreditAssignmentRepositoryException::class)
    fun addAssignment(assignment: CreditAssignment)

    @Throws(CreditAssignmentRepositoryException::class)
    fun getChatTotalCredits(chatId: ChatId): Map<User, CreditValue>
}