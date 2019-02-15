package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.MessageEmitter
import com.dmdirc.ktirc.model.ServerStatus

/**
 * Sends a [ServerReady] event once the first line has been received post 001/005/etc.
 */
internal class ServerReadyMutator : EventMutator {

    /** Events that won't trigger a 'server ready' event to be sent. */
    private val excludedEvents = listOf(
            ServerConnecting::class,
            ServerConnected::class,
            ServerDisconnected::class,
            ServerWelcome::class,
            ServerReady::class,
            ServerFeaturesUpdated::class,

            PingReceived::class,
            ServerCapabilitiesReceived::class,
            ServerCapabilitiesAcknowledged::class,
            ServerCapabilitiesFinished::class
    )

    override fun mutateEvent(client: IrcClient, messageEmitter: MessageEmitter, event: IrcEvent): List<IrcEvent> = sequence {
        if (client.serverState.receivedWelcome
                && client.serverState.status == ServerStatus.Negotiating
                && event::class !in excludedEvents) {
            yield(ServerReady(event.metadata))
        }
        yield(event)
    }.toList()

}
