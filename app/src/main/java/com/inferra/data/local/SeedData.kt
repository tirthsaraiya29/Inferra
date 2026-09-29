package com.inferra.data.local

import com.inferra.domain.model.AiModel
import com.inferra.domain.model.DeviceTarget
import com.inferra.domain.model.DownloadJob
import com.inferra.domain.model.HardwareProfile

/**
 * Isolated test/seed data constants.
 * MUST NOT be referenced by production repositories or ViewModels.
 */
object SeedData {
    val initialHardwareProfiles: List<HardwareProfile> = emptyList()
    val initialDeviceTargets: List<DeviceTarget> = emptyList()
    val seedModels: List<AiModel> = emptyList()
    val initialDownloadJobs: List<DownloadJob> = emptyList()
}
