package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.enums.EConfigType
import org.junit.Assert.assertEquals
import org.junit.Test

class MainServerRowModelsTest {
    @Test
    fun protectedKiraProfileDoesNotShowServerDescription() {
        val profile = ProfileItem(configType = EConfigType.VLESS).apply {
            remarks = "KIRA VPN"
            server = "private.example"
            serverPort = "443"
            kiraProtected = true
        }

        val row = buildServerRowUiModel(
            server = ServersCache(guid = "profile-guid", profile = profile),
            subscriptionRemarks = "",
        )

        assertEquals("KIRA VPN", row.remarks)
        assertEquals("", row.statistics)
    }
}
