package dev.jonaz.vured.bot.service.application

import dev.jonaz.vured.bot.service.discord.DiscordClientService
import dev.jonaz.vured.bot.application.Translation
import dev.jonaz.vured.bot.util.extensions.genericInject
import dev.jonaz.vured.bot.util.environment.Environment
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

class LogService {
    private val discordClientService by genericInject<DiscordClientService>()
    private val config by ConfigService

    fun sendLog(title: String, description: String?, member: Member?, color: Color?) {
        val channel = getTextChannel() ?: return
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
        val date = dateFormat.format(Date())

        val message = EmbedBuilder().apply {
            this.setTitle(title)

            color?.let {
                this.setColor(color)
            }

            description?.let {
                if (member == null) {
                    this.setDescription(description)
                } else {
                    this.setDescription(
                        description.replace("%v", "${member.asMention} (${member.user.asTag})")
                    )
                }
            }

            this.setFooter(Translation.LOG_DATE.replace("%v", date))
        }.run {
            this.build()
        }

        channel.sendMessageEmbeds(message).queue()
    }

    fun sendStartupMessage() = when (config.env) {
        Environment.DEV -> Translation.LOG_APPLICATION_STARTED_DEVELOPMENT
        Environment.PROD -> Translation.LOG_APPLICATION_STARTED_PRODUCTION
    }.run {
        sendLog(
            title = Translation.LOG_APPLICATION_STARTED_TITLE,
            description = this.replace("%v", getImplementationVersion() ?: ""),
            member = null,
            color = Color(209, 236, 241)
        )
    }

    fun getImplementationVersion(): String? {
        return this::class.java.`package`.implementationVersion
    }

    private fun getTextChannel(): TextChannel? {
        return discordClientService.jda.getTextChannelById(config.discord.logChannel)
    }
}
