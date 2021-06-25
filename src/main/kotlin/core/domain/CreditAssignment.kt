package core.domain

data class CreditAssignment(val chatId: ChatId, val assignee: UserId, val value: CreditValue)