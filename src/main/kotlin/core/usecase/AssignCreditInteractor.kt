package core.usecase

import core.domain.ChatId
import core.domain.CreditAssignment
import core.domain.CreditValue
import core.domain.UserId
import core.port.CreditAssignmentRepoException
import core.port.ICreditAssignmentRepository

private val POSITIVE_CREDIT = CreditValue(20)
private val NEGATIVE_CREDIT = CreditValue(-20)

class AssignCreditInteractor(private val assignmentsRepo: ICreditAssignmentRepository) {

    @Throws(AssignCreditException::class)
    fun assignPositive(chatId: ChatId, assigneeId: UserId) {
        assignCredit(chatId, assigneeId, POSITIVE_CREDIT)
    }

    @Throws(AssignCreditException::class)
    fun assignNegative(chatId: ChatId, assigneeId: UserId) {
        assignCredit(chatId, assigneeId, NEGATIVE_CREDIT)
    }

    @Throws(AssignCreditException::class)
    private fun assignCredit(chatId: ChatId, assigneeId: UserId, value: CreditValue) {
        try {
            assignmentsRepo.addAssignment(CreditAssignment(chatId, assigneeId, value))
        } catch (e: CreditAssignmentRepoException) {
            throw AssignCreditException("Failed to assign credit in chat $chatId to user $assigneeId", e)
        }
    }
}