package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.NicknameChangeFailed
import com.dmdirc.ktirc.events.NicknameChangeRequired
import com.dmdirc.ktirc.io.MessageEmitter
import com.dmdirc.ktirc.model.ServerStatus

internal class NickChangeRequiredMutator : EventMutator {

    override fun mutateEvent(client: IrcClient, messageEmitter: MessageEmitter, event: IrcEvent) =
            if (event is NicknameChangeFailed && client.serverState.status < ServerStatus.Ready) {
                listOf(NicknameChangeRequired(event.metadata, event.cause))
            } else {
                listOf(event)
            }

}