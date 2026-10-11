package fr.martinrocca.resto.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.martinrocca.resto.BuildConfig
import fr.martinrocca.resto.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalGoogleFallbackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun missingGoogleConfigurationStillAllowsJournalAndLocalSearch() {
        // CI uses an emulator without Play Services and without any API key.
        // This read-only scenario never deletes or modifies the installed journal.
        assumeTrue(BuildConfig.GOOGLE_PLACES_API_KEY.isBlank())
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Ouvrir le Journal").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ouvrir le Journal").performScrollTo().performClick()
        compose.onNodeWithText("Rechercher dans le journal")
            .assertIsDisplayed().performTextInput("recherche locale de test")
    }
}
