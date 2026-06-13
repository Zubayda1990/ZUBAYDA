package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun app_renders_without_crashing() {
        composeTestRule.setContent {
            GlowLogicMasterApp()
        }
        
        // click on tabs
        composeTestRule.onNodeWithText("الفحص").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("الصيدلية").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("التقرير").performClick()
        composeTestRule.waitForIdle()
    }
}
