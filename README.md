# KtIrc

[![Build Status](https://travis-ci.org/csmith/KtIrc.svg?branch=master)](https://travis-ci.org/csmith/KtIrc)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c01221cbf9cf413ba4d94cb8c80e334a)](https://www.codacy.com/app/csmith/KtIrc?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=csmith/KtIrc&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/csmith/KtIrc/branch/master/graph/badge.svg)](https://codecov.io/gh/csmith/KtIrc)
[![Download](https://api.bintray.com/packages/dmdirc/releases/ktirc/images/download.svg)](https://bintray.com/dmdirc/releases/ktirc/_latestVersion)

KtIrc is a Kotlin JVM library for connecting to and interacting with IRC servers.
It is still in an early stage of development.

## Setup

KtIrc is published to JCenter, so adding it to a gradle build is as simple as:

```groovy
repositories {
    jcenter()
}

dependencies {
    implementation("com.dmdirc:ktirc:<VERSION>")
}
```

## Usage

The main interface for interacting with KtIrc is the `IrcClientImpl` class. A
simple bot might look like:

```kotlin
with(IrcClientImpl(Server("my.server.com", 6667), Profile("nick", "realName", "userName"))) {
    onEvent { event ->
        when (event) {
            is ServerReady -> sendJoin("#ktirc")
            is MessageReceived ->
                if (event.message == "!test")
                    reply(event, "Test successful!")
        }
    }
    connect()
}
```

## Known issues / FAQ

### `java.lang.IllegalStateException: Check failed` when connecting to some servers

This happens when the IRC server requests an optional client certificate (for use
in SASL auth, usually). At present there is no support for client certificates in
the networking library used by KtIrc. This is tracked upstream in
[ktor#641](https://github.com/ktorio/ktor/issues/641). There is no workaround
other than using an insecure connection.

### KtIrc connects over IPv4 even when host has IPv6

This is an issue with the Java standard library. You can change its behaviour by
defining the system property `java.net.preferIPv6Addresses` to `true`, e.g. by
running Java with `-Djava.net.preferIPv6Addresses=true` or calling
`System.setProperty("java.net.preferIPv6Addresses","true");` in code.

## Developing KtIrc

### Lifecycle of a message

![architecture diagram](docs/sequence.png) 

The `LineBufferedSocket` class receives bytes from the IRC server. Whenever it
encounters a complete line (terminated by a `CR`, `LF` or `CRLF`), it passes it
to the `IrcClient` as a `ByteArray`. The `MessageParser` breaks up the line
into its component parts (tags, prefixes, commands, and parameters) and returns
them as an `IrcMessage`.
 
The `IrcMessage` is given to the `MessageHandler`, which tries to find a
processor that can handle the command in the message. The processor's job is
to convert the message into an `IrcEvent` subclass. Processors do not get
given any contextual information or state, their job is simply to convert
the message as received into an event.

The events are returned to the `MessageHandler` which then passes them on
to all registered event handlers. The job of the event handlers is twofold:
firstly, use the events to update the state of KtIrc (for example, after
receiving a `JOIN` message, the `ChannelStateHandler` will add the user
to the list of users in the channel, while the `UserStateHandler` may update
the user's hostname if we hadn't previously seen it). Secondly, the event
handlers may themselves raise events. This is useful for higher-order
events such as `ServerReady` that depend on a variety of factors and
states.

Handlers themselves may not keep state, as they will be shared across
multiple instances of `IrcClient` and won't be reset on reconnection.
State is instead stored in the various `*State` properties of the
`IrcClient` such as `serverState` and `channelState`. Fields that
should not be exposed to users of KtIrc can be placed in these
public state objects but marked as `internal`.

All the generated events (from processors or from event handlers) are
passed to the `IrcClient`, which in turn passes them to the library
user via the delegates passed to the `onEvent` method. 

### Contributing

Contributing is welcomed and encouraged! Please try to add unit tests for new features,
and maintain a code style consistent with the existing code.

### Licence

The code in this repository is released under the MIT licence. See the
[LICENCE](LICENCE) file for more info.
