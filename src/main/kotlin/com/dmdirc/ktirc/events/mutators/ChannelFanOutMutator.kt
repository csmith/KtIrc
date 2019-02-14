package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*

/**
 * "Fans out" global events such as quits and nick changes to each channel a user is in.
 */
internal class ChannelFanOutMutator : EventMutator {

    override fun mutateEvent(client: IrcClient, event: IrcEvent) = sequence<IrcEvent> {
        yield(event)
        when (event) {
            is UserQuit -> handleQuit(client, event)
            is UserNickChanged -> handleNickChanged(client, event)
        }
    }.toList()

    private suspend fun SequenceScope<IrcEvent>.handleQuit(client: IrcClient, event: UserQuit) {
        client.channelState.forEach {
            if (it.users.contains(event.user.nickname)) {
                yield(ChannelQuit(event.time, event.user, it.name, event.reason))
            }
        }
    }

    private suspend fun SequenceScope<IrcEvent>.handleNickChanged(client: IrcClient, event: UserNickChanged) {
        client.channelState.forEach {
            it.users[event.user.nickname]?.let { chanUser ->
                chanUser.nickname = event.newNick
                yield(ChannelNickChanged(event.time, event.user, it.name, event.newNick))
            }
        }
    }

}
