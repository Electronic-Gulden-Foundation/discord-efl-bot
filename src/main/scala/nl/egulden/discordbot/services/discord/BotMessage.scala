package nl.egulden.discordbot.services.discord

import net.dv8tion.jda.api.entities.Message

case class BotMessage(message: Message, config: CommandConfig)
