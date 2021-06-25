package core.port

import core.domain.ChatId
import core.domain.CreditAssignment
import core.domain.UserId

interface ICreditAssignmentRepository {

    @Throws(CreditAssignmentRepoException::class)
    fun addAssignment(assignment: CreditAssignment)

    @Throws(CreditAssignmentRepoException::class)
    fun getChatAssignments(chatId: ChatId): Map<UserId, ChatId>
}