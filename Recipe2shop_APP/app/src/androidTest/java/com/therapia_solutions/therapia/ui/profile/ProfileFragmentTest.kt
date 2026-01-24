package com.therapia_solutions.therapia.ui.profile

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.therapia_solutions.therapia.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.ViewActions.*

/**
 * Tests instrumentés pour ProfileFragment
 */
@RunWith(AndroidJUnit4::class)
class ProfileFragmentTest {

    private lateinit var scenario: FragmentScenario<ProfileFragment>

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer<ProfileFragment>()
    }

    @Test
    fun testProfileFieldsAreDisplayed() {
        // Vérifier que tous les champs de profil sont affichés
        onView(withId(R.id.et_first_name))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_last_name))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_email))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_phone))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_specialization))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_experience))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_address))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_city))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_postal_code))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_country))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_bio))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.et_website))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testActionButtonsAreDisplayed() {
        // Vérifier que les boutons d'action sont affichés
        onView(withId(R.id.btn_edit_profile))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_save_profile))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEditButtonIsEnabled() {
        // Vérifier que le bouton "Modifier" est activé
        onView(withId(R.id.btn_edit_profile))
            .check(matches(isEnabled()))
    }

    @Test
    fun testSaveButtonIsInitiallyHidden() {
        // Vérifier que le bouton "Sauvegarder" est initialement masqué
        onView(withId(R.id.btn_save_profile))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun testProfileFieldsAreInitiallyDisabled() {
        // Vérifier que les champs sont initialement désactivés
        onView(withId(R.id.et_first_name))
            .check(matches(not(isEnabled())))
        
        onView(withId(R.id.et_last_name))
            .check(matches(not(isEnabled())))
        
        onView(withId(R.id.et_email))
            .check(matches(not(isEnabled())))
    }

    @Test
    fun testEditButtonClickEnablesFields() {
        // Cliquer sur le bouton "Modifier"
        onView(withId(R.id.btn_edit_profile))
            .perform(click())
        
        // Vérifier que les champs sont maintenant activés
        onView(withId(R.id.et_first_name))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.et_last_name))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.et_email))
            .check(matches(isEnabled()))
    }

    @Test
    fun testEditButtonClickShowsSaveButton() {
        // Cliquer sur le bouton "Modifier"
        onView(withId(R.id.btn_edit_profile))
            .perform(click())
        
        // Vérifier que le bouton "Sauvegarder" est maintenant visible
        onView(withId(R.id.btn_save_profile))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testProfileStatsAreDisplayed() {
        // Vérifier que les statistiques du profil sont affichées
        onView(withId(R.id.tv_profile_status))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_profile_created))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.tv_profile_updated))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testProfileFieldsHaveCorrectHints() {
        // Vérifier que les champs ont les bons hints
        onView(withId(R.id.et_first_name))
            .check(matches(withHint("Prénom")))
        
        onView(withId(R.id.et_last_name))
            .check(matches(withHint("Nom")))
        
        onView(withId(R.id.et_email))
            .check(matches(withHint("Email")))
        
        onView(withId(R.id.et_phone))
            .check(matches(withHint("Téléphone")))
    }

    @Test
    fun testFragmentLifecycle() {
        // Vérifier que le fragment se charge correctement
        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testProfileTitleIsDisplayed() {
        // Vérifier que le titre du profil est affiché
        onView(withText("Mon Profil"))
            .check(matches(isDisplayed()))
    }
}
