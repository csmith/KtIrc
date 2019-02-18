package com.dmdirc.ktirc.events.mutators

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.BatchFinished
import com.dmdirc.ktirc.events.BatchReceived
import com.dmdirc.ktirc.events.BatchStarted
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.io.MessageEmitter
import com.dmdirc.ktirc.model.Batch

internal class BatchMutator : EventMutator {

    override fun mutateEvent(client: IrcClient, messageEmitter: MessageEmitter, event: IrcEvent): List<IrcEvent> {
        when {
            event is BatchStarted -> startBatch(client, event)
            event is BatchFinished -> return finishBatch(client, event)
            event.metadata.batchId != null -> addToBatch(client, messageEmitter, event)
            else -> return listOf(event)
        }
        return emptyList()
    }

    private fun startBatch(client: IrcClient, event: BatchStarted) {
        client.serverState.batches[event.referenceId] =
                Batch(event.batchType, event.params.asList(), event.metadata, mutableListOf())
    }

    private fun finishBatch(client: IrcClient, event: BatchFinished): List<IrcEvent> {
        client.serverState.batches.remove(event.referenceId)?.let {
            val batch = BatchReceived(it.events[0].metadata, it.type, it.arguments.toTypedArray(), it.events)
            if (it.metadata.batchId == null) {
                return listOf(batch)
            } else {
                client.serverState.batches[it.metadata.batchId]?.events?.add(batch)
            }
        }

        return emptyList()
    }

    private fun addToBatch(client: IrcClient, messageEmitter: MessageEmitter, event: IrcEvent) {
        client.serverState.batches[event.metadata.batchId]?.let {
            it.events += event
            messageEmitter.handleEvent(client, event, true)
        }
    }

}
