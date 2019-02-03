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
            is ServerWelcome -> sendJoin("#ktirc")
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

## Contributing

Contributing is welcomed and encouraged! Please try to add unit tests for new features,
and maintain a code style consistent with the existing code.

## Licence

The code in this repository is released under the MIT licence. See the
[LICENCE](LICENCE) file for more info.