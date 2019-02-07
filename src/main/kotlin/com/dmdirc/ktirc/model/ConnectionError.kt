package com.dmdirc.ktirc.model

import java.net.ConnectException
import java.nio.channels.UnresolvedAddressException
import java.security.cert.CertificateException

/**
 * Possible types of errors that occur whilst connecting.
 */
enum class ConnectionError {
    /** An error occurred, but we don't really know what. */
    Unknown,
    /** The hostname did not resolve to an IP address. */
    UnresolvableAddress,
    /** A connection couldn't be established to the given host/port. */
    ConnectionRefused,
    /** There was an issue with the TLS certificate the server presented. */
    BadTlsCertificate,
}

internal fun Exception.toConnectionError() =
        when (this) {
            is UnresolvedAddressException -> ConnectionError.UnresolvableAddress
            is CertificateException -> ConnectionError.BadTlsCertificate
            is ConnectException -> ConnectionError.ConnectionRefused
            else -> ConnectionError.Unknown
        }
