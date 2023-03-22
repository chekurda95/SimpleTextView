package com.example.simpletextview.benchmark

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBaselineProfilesApi::class)
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineRule.collectBaselineProfile(
        packageName = "com.example.simpletextview"
    ) {
        pressHome()
        startActivityAndWait()
        val button = device.findObject(By.text("SBIS"))
        button.click()
        /*repeat(10) {
            button.click()
        }*/
        device.waitForIdle()
    }
}