package com.dmdirc.ktirc.io

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

internal class CertificateValidationTest {

    private val cert = mockk<X509Certificate>()

    @Test
    fun `checks common name`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=subdomain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain2.test.ktirc"))
        assertFalse(cert.validFor("testing"))
    }

    @Test
    fun `checks common name with suffixed wildcard`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=subdomain*.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.ktirc"))
        assertFalse(cert.validFor("foo.subdomain.test.ktirc"))
        assertFalse(cert.validFor("1subdomain.test.ktirc"))
    }

    @Test
    fun `checks common name with preixed wildcard`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=*subdomain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertTrue(cert.validFor("1subdomain.test.ktirc"))
        assertFalse(cert.validFor("foo.subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain1.test.ktirc"))
    }

    @Test
    fun `checks common name with infixed wildcard`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=sub*domain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertTrue(cert.validFor("SUB-domain.test.ktirc"))
        assertFalse(cert.validFor("foo.subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain1.test.ktirc"))
    }

    @Test
    fun `ignores wildcards in CN if they're not left-most`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=foo.*domain.test.ktirc,O=testing,L=London,C=GB"
        }

        assertFalse(cert.validFor("foo.domain.test.ktirc"))
        assertFalse(cert.validFor("foo-test.domain.test.ktirc"))
        assertFalse(cert.validFor("foo.test-domain.test.ktirc"))
    }

    @Test
    fun `ignores wildcards in CN if there are too many`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=*domain*.test.ktirc,O=testing,L=London,C=GB"
        }

        assertFalse(cert.validFor("domain.test.ktirc"))
        assertFalse(cert.validFor("subdomain.test.ktirc"))
        assertFalse(cert.validFor("domain1.test.ktirc"))
    }

    @Test
    fun `checks all sans`() {
        every { cert.subjectAlternativeNames } returns listOf(
                listOf(4, "directory.test.ktirc"),
                listOf(2, "subdomain1.test.ktirc"),
                listOf(2, "subdomain2.test.ktirc"),
                listOf(2, "subdomain3.test.ktirc")
        )

        assertTrue(cert.validFor("subdomain1.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.KTIRC"))
        assertTrue(cert.validFor("subdomain3.test.ktirc"))
        assertFalse(cert.validFor("directory.test.ktirc"))
    }

    @Test
    fun `checks wildcard sans`() {
        every { cert.subjectAlternativeNames } returns listOf(
                listOf(4, "directory.test.ktirc"),
                listOf(2, "*domain1.test.ktirc"),
                listOf(2, "subdomain*.test.ktirc"),
                listOf(2, "*foo*.test.ktirc"),
                listOf(2, "foo.*.ktirc")
        )

        assertTrue(cert.validFor("subdomain1.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.ktirc"))
        assertTrue(cert.validFor("gooddomain1.TEST.ktirc"))
        assertFalse(cert.validFor("foo.test.ktirc"))
    }

    @Test
    fun `still uses CN if sans throws`() {
        every { cert.subjectX500Principal } returns mockk {
            every { name } returns "CN=subdomain.test.ktirc,O=testing,L=London,C=GB"
        }
        every { cert.subjectAlternativeNames } throws CertificateException("Oops")

        assertTrue(cert.validFor("subdomain.test.ktirc"))
        assertFalse(cert.validFor("subdomain2.test.ktirc"))
        assertFalse(cert.validFor("testing"))
    }

    @Test
    fun `still uses sans if CN throws`() {
        every { cert.subjectX500Principal } throws CertificateException("Oops")
        every { cert.subjectAlternativeNames } returns listOf(
                listOf(4, "directory.test.ktirc"),
                listOf(2, "subdomain1.test.ktirc"),
                listOf(2, "subdomain2.test.ktirc"),
                listOf(2, "subdomain3.test.ktirc")
        )

        assertTrue(cert.validFor("subdomain1.test.ktirc"))
        assertTrue(cert.validFor("subdomain2.test.KTIRC"))
        assertTrue(cert.validFor("subdomain3.test.ktirc"))
        assertFalse(cert.validFor("directory.test.ktirc"))
    }


    @Test
    fun `fails if CN and sans missing`() {
        assertFalse(cert.validFor("subdomain1.test.ktirc"))
        assertFalse(cert.validFor("subdomain2.test.KTIRC"))
        assertFalse(cert.validFor("subdomain3.test.ktirc"))
        assertFalse(cert.validFor("directory.test.ktirc"))
    }

}