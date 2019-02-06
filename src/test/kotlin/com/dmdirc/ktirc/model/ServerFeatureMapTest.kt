package com.dmdirc.ktirc.model

import com.dmdirc.ktirc.io.CaseMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ServerFeatureMapTest {

    @Test
    fun `should return defaults for unspecified features`() {
        val featureMap = ServerFeatureMap()
        assertEquals(200, featureMap[ServerFeature.MaximumChannelNameLength])
    }

    @Test
    fun `should return null for unspecified features with no default`() {
        val featureMap = ServerFeatureMap()
        assertNull(featureMap[ServerFeature.ChannelModes])
    }

    @Test
    fun `should return previously set value for features`() {
        val featureMap = ServerFeatureMap()
        featureMap[ServerFeature.MaximumChannels] = 123
        assertEquals(123, featureMap[ServerFeature.MaximumChannels])
    }

    @Test
    fun `should return default set value for features that were reset`() {
        val featureMap = ServerFeatureMap()
        featureMap[ServerFeature.MaximumChannels] = 123
        featureMap.reset(ServerFeature.MaximumChannels)
        assertNull(featureMap[ServerFeature.MaximumChannels])
    }

    @Test
    fun `should throw if a feature is set with the wrong type`() {
        val featureMap = ServerFeatureMap()
        assertThrows(IllegalArgumentException::class.java) {
            featureMap[ServerFeature.MaximumChannels] = "123"
        }
    }

    @Test
    fun `sets all features from another map`() {
        val featureMap1 = ServerFeatureMap()
        val featureMap2 = ServerFeatureMap()
        featureMap2[ServerFeature.WhoxSupport] = true
        featureMap2[ServerFeature.ChannelModes] = arrayOf("abc", "def")
        featureMap1.setAll(featureMap2)

        assertEquals(true, featureMap1[ServerFeature.WhoxSupport])
        assertArrayEquals(arrayOf("abc", "def"), featureMap1[ServerFeature.ChannelModes])
    }

    @Test
    fun `resets features reset in another map`() {
        val featureMap1 = ServerFeatureMap()
        val featureMap2 = ServerFeatureMap()
        featureMap1[ServerFeature.ServerCaseMapping] = CaseMapping.RfcStrict
        featureMap2.reset(ServerFeature.ServerCaseMapping)
        featureMap1.setAll(featureMap2)

        assertEquals(CaseMapping.Rfc, featureMap1[ServerFeature.ServerCaseMapping])
    }

    @Test
    fun `clear removes all features`() {
        val featureMap = ServerFeatureMap()
        featureMap[ServerFeature.MaximumChannels] = 123
        featureMap[ServerFeature.Network] = "testnet"
        featureMap.clear()
        assertTrue(featureMap.isEmpty())
    }

    @Test
    fun `isEmpty returns true if empty`() {
        val featureMap = ServerFeatureMap()
        assertTrue(featureMap.isEmpty())
        featureMap[ServerFeature.MaximumChannels] = 123
        assertFalse(featureMap.isEmpty())
    }
    
}
