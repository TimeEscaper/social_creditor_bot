package core.usecase

import core.domain.ChatId
import core.domain.CreditValue
import core.domain.User
import core.domain.UserId
import core.port.CreditAssignmentRepositoryException
import core.port.ICreditAssignmentRepository

class RetrieveCreditsInteractor(private val assignmentsRepo: ICreditAssignmentRepository) {

    @Throws(RetrieveCreditsException::class)
    fun retrieveTotalCredits(chatId: ChatId): Map<User, CreditValue> {
        try {
            return assignmentsRepo.getChatTotalCredits(chatId)
        } catch (e: CreditAssignmentRepositoryException) {
            throw RetrieveCreditsException("Failed to retrieve data from repository", e)
        }
    }
}