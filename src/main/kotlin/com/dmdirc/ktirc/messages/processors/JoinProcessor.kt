package com.dmdirc.ktirc.messages.processors

import com.dmdirc.ktirc.events.ChannelJoinFailed
import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.messages.*
import com.dmdirc.ktirc.model.IrcMessage
import com.dmdirc.ktirc.model.User

internal class JoinProcessor : MessageProcessor {

    override val commands = arrayOf(
            "JOIN",
            ERR_TOOMANYCHANNELS,
            ERR_NOHIDING,
            ERR_CHANNELISFULL,
            ERR_INVITEONLYCHAN,
            ERR_BANNEDFROMCHAN,
            ERR_BADCHANNELKEY,
            ERR_NEEDREGGEDNICK,
            ERR_BADCHANNAME,
            ERR_THROTTLE,
            ERR_SECUREONLYCHAN,
            ERR_TOOMANYJOINS,
            ERR_ADMINONLY,
            ERR_OPERONLY)

    override fun process(message: IrcMessage) = when {
        message.command == "JOIN" ->
            message.sourceUser?.let { user ->
                user.addExtendedJoinFields(message.params)
                listOf(ChannelJoined(message.metadata, user, String(message.params[0])))
            } ?: emptyList()
        message.params.size >= 2 ->
            listOf(ChannelJoinFailed(message.metadata, String(message.params[1]), message.command.toReason()))
        else ->
            emptyList()
    }

    private fun User.addExtendedJoinFields(params: List<ByteArray>) {
        if (params.size == 3) {
            String(params[1]).let { account = if (it == "*") null else it }
            realName = String(params[2])
        }
    }

    private fun String.toReason() = when (this) {
        ERR_TOOMANYCHANNELS -> ChannelJoinFailed.JoinError.TooManyChannels
        ERR_NOHIDING -> ChannelJoinFailed.JoinError.NoHiding
        ERR_CHANNELISFULL -> ChannelJoinFailed.JoinError.ChannelFull
        ERR_INVITEONLYCHAN -> ChannelJoinFailed.JoinError.NeedInvite
        ERR_BANNEDFROMCHAN -> ChannelJoinFailed.JoinError.Banned
        ERR_BADCHANNELKEY -> ChannelJoinFailed.JoinError.NeedKey
        ERR_NEEDREGGEDNICK -> ChannelJoinFailed.JoinError.NeedRegisteredNick
        ERR_BADCHANNAME -> ChannelJoinFailed.JoinError.BadChannelName
        ERR_THROTTLE -> ChannelJoinFailed.JoinError.Throttled
        ERR_SECUREONLYCHAN -> ChannelJoinFailed.JoinError.NeedTls
        ERR_TOOMANYJOINS -> ChannelJoinFailed.JoinError.Throttled
        ERR_ADMINONLY -> ChannelJoinFailed.JoinError.NeedAdmin
        ERR_OPERONLY -> ChannelJoinFailed.JoinError.NeedOper
        else -> ChannelJoinFailed.JoinError.Unknown
    }

}

