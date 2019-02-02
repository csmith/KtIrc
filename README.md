# KtIrc

[![Build Status](https://travis-ci.org/csmith/KtIrc.svg?branch=master)](https://travis-ci.org/csmith/KtIrc)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c01221cbf9cf413ba4d94cb8c80e334a)](https://www.codacy.com/app/csmith/KtIrc?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=csmith/KtIrc&amp;utm_campaign=Badge_Grade)
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
val client = IrcClientImpl(Server("my.server.com", 6667), Profile("nick", "realName", "userName"))
client.onEvent { event ->
    when (event) {
        is ServerWelcome ->
            client.send(joinMessage("#ktirc"))
        is MessageReceived ->
            if (event.message == "!test")
                client.send(privmsgMessage(event.target, "Test successful!"))
    }
}
client.connect()
```

## Contributing

Contributing is welcomed and encouraged! Please try to add unit tests for new features,
and maintain a code style consistent with the existing code.

## Licence

The code in this repository is released under the MIT licence. See the
[LICENCE](LICENCE) file for more info.