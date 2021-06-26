package entrypoint.bot

import core.domain.ChatId
import core.domain.UserId
import core.usecase.AssignCreditException
import core.usecase.AssignCreditInteractor
import data.repository.SQLiteAssignmentRepository
import dev.inmo.micro_utils.coroutines.safely
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.longPolling
import dev.inmo.tgbotapi.types.chat.abstracts.GroupChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.media.StickerContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn

private const val ARG_TOKEN = "--token"
private const val ARG_DB_PATH = "--db-path"

private const val STICKER_PACK_NAME = "PoohSocialCredit"

private const val STICKER_PLUS_20 = "AgADAgADf3BGHA"
private const val STICKER_MINUS_20 = "AgADAwADf3BGHA"

private const val MESSAGE_ERROR = "Something went wrong, credits not assigned!"

data class FilteredMessage(val chatId: Long, val senderUserId: Long, val replyToUserId: Long, val isPositive: Boolean)

fun parseArgs(args: Array<out String>): Pair<String, String> {
    if (args.size != 4)
        throw RuntimeException("Not enough input arguments")
    var token = ""
    var dbPath = ""
    if (args[0] == ARG_TOKEN) {
        token = args[1]
        if (args[2] != ARG_DB_PATH)
            throw RuntimeException("$ARG_DB_PATH argument is not present")
        dbPath = args[3]
    } else if (args[0] == ARG_DB_PATH) {
        dbPath = args[1]
        if (args[2] != ARG_TOKEN)
            throw RuntimeException("$ARG_TOKEN argument is not present")
        token = args[3]
    } else {
        throw RuntimeException("Wrong arguments")
    }
    return token to dbPath
}

fun filterMessage(message: Message): FilteredMessage? {
    // Check if message is not from group chat
    if (message.chat !is GroupChat)
        return null

    // Try to obtain group message with content and as user (not bot) message
    val groupMessage = message.asGroupContentMessage() ?: return null
    val userMessage = message.asFromUserMessage() ?: return null
    if (userMessage.user.asBot() != null)
        return null
    val senderUserId = userMessage.user.id.chatId

    // Check if the message is the reply and reply not to bot
    val replyMessage = groupMessage.replyTo ?: return null
    val replyToUserMessage = replyMessage.asFromUserMessage() ?: return null
    if (replyToUserMessage.user.asBot() != null)
        return null
    val replyToUserId = replyToUserMessage.user.id.chatId

    // Check if message includes any sticker
    if (groupMessage.content !is StickerContent)
        return null

    // Try to extract sticker info and filter it
    val stickerContent = groupMessage.content.asStickerContent() ?: return null
    val stickerPack = stickerContent.media.stickerSetName ?: return null
    if (stickerPack != STICKER_PACK_NAME)
        return null
    val stickerId = stickerContent.media.fileUniqueId
    if (stickerId != STICKER_PLUS_20 && stickerId != STICKER_MINUS_20)
        return null

    return FilteredMessage(message.chat.id.chatId, senderUserId, replyToUserId, stickerId == STICKER_PLUS_20)
}

suspend fun main(vararg args: String) {
    val (botToken, dbPath) = parseArgs(args)

    val assignInteractor = AssignCreditInteractor(SQLiteAssignmentRepository(dbPath))
    assignInteractor.assignPositive(ChatId(0), UserId(0), UserId(1))

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)

    bot.longPolling(scope = scope) {
        messageFlow.onEach {
            safely({ e ->
                // TODO: Logging
                println("Unhandled exception")
                e.printStackTrace()
            }) {
                val message = filterMessage(it.data) ?: return@safely
                try {
                    if (message.isPositive)
                        assignInteractor.assignPositive(
                            ChatId(message.chatId), UserId(message.senderUserId), UserId(message.replyToUserId)
                        )
                    else
                        assignInteractor.assignNegative(
                            ChatId(message.chatId), UserId(message.senderUserId), UserId(message.replyToUserId))
                } catch (e: AssignCreditException) {
                    // TODO: Logging
                    println("Failed to assign credits")
                    e.printStackTrace()
                    bot.reply(it.data, MESSAGE_ERROR)
                }
            }
        }.launchIn(scope)
    }

    scope.coroutineContext[Job]!!.join()
}