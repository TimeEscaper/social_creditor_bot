package core.domain

data class CreditAssignment(val chatId: ChatId, val assignee: User, val value: CreditValue)