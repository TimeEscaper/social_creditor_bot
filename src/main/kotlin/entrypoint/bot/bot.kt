package entrypoint.bot

import core.domain.ChatId
import core.domain.User
import core.domain.UserId
import core.usecase.AssignCreditException
import core.usecase.AssignCreditInteractor
import core.usecase.RetrieveCreditsException
import core.usecase.RetrieveCreditsInteractor
import data.repository.SQLiteAssignmentRepository
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.types.chat.abstracts.GroupChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.media.StickerContent

private const val ARG_TOKEN = "--token"
private const val ARG_DB_PATH = "--db-path"

private const val STICKER_PACK_NAME = "PoohSocialCredit"

private const val STICKER_PLUS_20 = "AgADAgADf3BGHA"
private const val STICKER_MINUS_20 = "AgADAwADf3BGHA"

private const val MESSAGE_ASSIGN_ERROR = "Something went wrong, credits not assigned!"
private const val MESSAGE_RETRIEVE_ERROR = "Something went wrong, can not obtain info!"
private const val MESSAGE_HEALTHCHECK = "Status: OK"
private const val MESSAGE_NO_DATA = "Seems that there were not any credit assignments in this chat after adding the bot!"

private const val COMMAND_GET_CREDITS = "credits"
private const val COMMAND_HEALTHCHECK = "healthcheck"

data class FilteredMessage(val chatId: Long,
                           val senderUserId: Long,
                           val replyToUserId: Long,
                           val replyToUserName: String,
                           val isPositive: Boolean)

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

fun checkGroupAndHumanMessage(message: Message): FromUserMessage? {
    // Check if message is not from group chat
    if (message.chat !is GroupChat)
        return null
    val userMessage = message.asFromUserMessage() ?: return null
    if (userMessage.user.asBot() != null)
        return null
    return userMessage
}

fun filterCreditAssignMessage(message: Message): FilteredMessage? {
    val userMessage = checkGroupAndHumanMessage(message) ?: return null
    val groupMessage = message.asGroupContentMessage() ?: return null
    val senderUserId = userMessage.user.id.chatId

    // Check if the message is the reply and reply not to bot
    val replyMessage = groupMessage.replyTo ?: return null
    val replyToUserMessage = replyMessage.asFromUserMessage() ?: return null
    if (replyToUserMessage.user.asBot() != null)
        return null
    val replyToUserId = replyToUserMessage.user.id.chatId
    var replyToUserName = "${replyToUserMessage.user.firstName} ${replyToUserMessage.user.lastName}"
    if (replyToUserMessage.user.username != null) {
        replyToUserName += " (${replyToUserMessage.user.username?.username})"
    }

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

    return FilteredMessage(message.chat.id.chatId, senderUserId,
        replyToUserId, replyToUserName, stickerId == STICKER_PLUS_20)
}

suspend fun main(vararg args: String) {
    val (botToken, dbPath) = parseArgs(args)

    val repo = SQLiteAssignmentRepository(dbPath)
    val assignInteractor = AssignCreditInteractor(repo)
    val retrieveInteractor = RetrieveCreditsInteractor(repo)

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)

    bot.buildBehaviour(scope) {
        onCommand(COMMAND_GET_CREDITS.toRegex()) { message ->
            checkGroupAndHumanMessage(message) ?: return@onCommand
            val chatId = message.chat.id.chatId
            val credits = try {
                retrieveInteractor.retrieveTotalCredits(ChatId(chatId))
            } catch (e: RetrieveCreditsException) {
                // TODO: Logging
                println("Failed to retrieve credits")
                e.printStackTrace()
                bot.reply(message, MESSAGE_RETRIEVE_ERROR)
                return@onCommand
            }
            if (credits.isEmpty()) {
                bot.reply(message, MESSAGE_NO_DATA)
                return@onCommand
            }
            val responseMessage = credits.map { "${it.key.name}: ${it.value.value} credits" }.joinToString("\n")
            bot.reply(message, responseMessage)
        }
        onCommand(COMMAND_HEALTHCHECK.toRegex()) { message ->
            bot.reply(message, MESSAGE_HEALTHCHECK)
        }
        onContentMessage {
            val message = filterCreditAssignMessage(it) ?: return@onContentMessage
            val responseMessage = try {
                if (message.isPositive)
                    assignInteractor.assignPositive(
                        ChatId(message.chatId), User(UserId(message.replyToUserId), message.replyToUserName),
                        UserId(message.senderUserId))
                else
                    assignInteractor.assignNegative(
                        ChatId(message.chatId), User(UserId(message.replyToUserId), message.replyToUserName),
                        UserId(message.senderUserId))
            } catch (e: AssignCreditException) {
                // TODO: Logging
                println("Failed to assign credits")
                e.printStackTrace()
                bot.reply(it, MESSAGE_ASSIGN_ERROR)
                return@onContentMessage
            }
            if (responseMessage != null) {
                bot.reply(it, responseMessage.text)
            }
        }
    }.join()
}