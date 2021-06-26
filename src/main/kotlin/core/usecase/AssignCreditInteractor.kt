package core.usecase

import core.domain.*
import core.port.CreditAssignmentRepositoryException
import core.port.ICreditAssignmentRepository

private val POSITIVE_CREDIT = CreditValue(20)
private val NEGATIVE_CREDIT = CreditValue(-20)

private val MESSAGE_SELF_ASSIGN_WARNING = BotMessage(
    "You can not assign or debit any credits to yourself! Have a -20 credit penalty for that!")

class AssignCreditInteractor(private val assignmentsRepo: ICreditAssignmentRepository) {

    @Throws(AssignCreditException::class)
    fun assignPositive(chatId: ChatId, assignerId: UserId, assigneeId: UserId): BotMessage? {
        return assignCredit(chatId, assignerId, assigneeId, POSITIVE_CREDIT)
    }

    @Throws(AssignCreditException::class)
    fun assignNegative(chatId: ChatId,  assignerId: UserId, assigneeId: UserId): BotMessage? {
        return assignCredit(chatId, assignerId, assigneeId, NEGATIVE_CREDIT)
    }

    @Throws(AssignCreditException::class)
    private fun assignCredit(chatId: ChatId, assignerId: UserId, assigneeId: UserId, value: CreditValue): BotMessage? {
        var valueToAssign = value
        var resultMessage: BotMessage? = null
        if (assignerId == assigneeId) {
            valueToAssign = NEGATIVE_CREDIT
            resultMessage = MESSAGE_SELF_ASSIGN_WARNING
        }
        try {
            assignmentsRepo.addAssignment(CreditAssignment(chatId, assigneeId, valueToAssign))
        } catch (e: CreditAssignmentRepositoryException) {
            throw AssignCreditException("Failed to assign credit in chat $chatId to user $assigneeId by $assignerId", e)
        }
        return resultMessage
    }
}