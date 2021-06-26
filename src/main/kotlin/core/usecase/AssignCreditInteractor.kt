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
    fun assignPositive(chatId: ChatId, assignee: User, assignerId: UserId): BotMessage? {
        return assignCredit(chatId, assignee, assignerId, POSITIVE_CREDIT)
    }

    @Throws(AssignCreditException::class)
    fun assignNegative(chatId: ChatId, assignee: User, assignerId: UserId): BotMessage? {
        return assignCredit(chatId, assignee, assignerId, NEGATIVE_CREDIT)
    }

    @Throws(AssignCreditException::class)
    private fun assignCredit(chatId: ChatId, assignee: User, assignerId: UserId, value: CreditValue): BotMessage? {
        var valueToAssign = value
        var resultMessage: BotMessage? = null
        if (assignerId == assignee.userId) {
            valueToAssign = NEGATIVE_CREDIT
            resultMessage = MESSAGE_SELF_ASSIGN_WARNING
        }
        try {
            assignmentsRepo.addAssignment(CreditAssignment(chatId, assignee, valueToAssign))
        } catch (e: CreditAssignmentRepositoryException) {
            throw AssignCreditException("Failed to assign credit in chat $chatId to user ${assignee.userId} by $assignerId", e)
        }
        return resultMessage
    }
}