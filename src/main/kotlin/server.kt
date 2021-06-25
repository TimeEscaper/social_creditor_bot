import dev.inmo.micro_utils.coroutines.safely
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.utils.formatting.linkMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.longPolling
import dev.inmo.tgbotapi.types.ParseMode.MarkdownV2
import dev.inmo.tgbotapi.types.User
import dev.inmo.tgbotapi.types.chat.abstracts.GroupChat
import dev.inmo.tgbotapi.types.chat.abstracts.PrivateChat
import dev.inmo.tgbotapi.types.chat.abstracts.SupergroupChat
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.asChannelChat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn

/**
 * This is one of the most easiest bot - it will just print information about itself
 */
suspend fun main() {
    val botToken = "1877591727:AAF1LmgSyeOEfA2A6qWA73_U6Gx-XYV0i4o"

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)

    bot.longPolling(scope = scope) {
        messageFlow.onEach {
            safely {
                val message = it.data
                val chat = message.chat
                val answerText = "Oh, hi, " + when (chat) {
                    is PrivateChat -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                    is User -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                    is SupergroupChat -> (chat.username ?.username ?: bot.getChat(chat).inviteLink) ?.let {
                        chat.title.linkMarkdownV2(it)
                    } ?: chat.title
                    is GroupChat -> bot.getChat(chat).inviteLink ?.let {
                        chat.title.linkMarkdownV2(it)
                    } ?: chat.title
                    else -> "Unknown :(".escapeMarkdownV2Common()
                }
                bot.reply(
                    message,
                    answerText,
                    MarkdownV2
                )
            }
        }.launchIn(scope)
        channelPostFlow.onEach {
            safely {
                val chat = it.data.chat
                val message = "Hi everybody in this channel \"${(chat.asChannelChat()) ?.title}\""
                bot.sendTextMessage(chat, message, MarkdownV2)
            }
        }.launchIn(scope)
    }

    scope.coroutineContext[Job]!!.join()
}