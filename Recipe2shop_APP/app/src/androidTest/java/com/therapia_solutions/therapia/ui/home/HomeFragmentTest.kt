package com.therapia_solutions.therapia.ui.home

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
 * Tests instrumentés pour HomeFragment
 */
@RunWith(AndroidJUnit4::class)
class HomeFragmentTest {

    private lateinit var scenario: FragmentScenario<HomeFragment>

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer<HomeFragment>()
    }

    @Test
    fun testWelcomeMessageIsDisplayed() {
        // Vérifier que le message de bienvenue est affiché
        onView(withId(R.id.tv_welcome_message))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testQuickAccessButtonsAreDisplayed() {
        // Vérifier que tous les boutons d'accès rapide sont affichés
        onView(withId(R.id.btn_quick_patients))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_quick_agenda))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_quick_sandrine))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_quick_dashboard))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_quick_library))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_quick_profile))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testQuickAccessButtonsAreEnabled() {
        // Vérifier que tous les boutons d'accès rapide sont activés
        onView(withId(R.id.btn_quick_patients))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.btn_quick_agenda))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.btn_quick_sandrine))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.btn_quick_dashboard))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.btn_quick_library))
            .check(matches(isEnabled()))
        
        onView(withId(R.id.btn_quick_profile))
            .check(matches(isEnabled()))
    }

    @Test
    fun testQuickAccessButtonsHaveCorrectText() {
        // Vérifier que les boutons ont le bon texte
        onView(withId(R.id.btn_quick_patients))
            .check(matches(withText("Patients")))
        
        onView(withId(R.id.btn_quick_agenda))
            .check(matches(withText("Agenda")))
        
        onView(withId(R.id.btn_quick_sandrine))
            .check(matches(withText("Sandrine.AI")))
        
        onView(withId(R.id.btn_quick_dashboard))
            .check(matches(withText("Dashboard")))
        
        onView(withId(R.id.btn_quick_library))
            .check(matches(withText("Bibliothèque")))
        
        onView(withId(R.id.btn_quick_profile))
            .check(matches(withText("Profil")))
    }

    @Test
    fun testRecentActivitySectionIsDisplayed() {
        // Vérifier que la section d'activité récente est affichée
        onView(withId(R.id.tv_recent_activity))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.btn_view_all_activity))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testViewAllActivityButtonIsClickable() {
        // Vérifier que le bouton "Voir toute l'activité" est cliquable
        onView(withId(R.id.btn_view_all_activity))
            .check(matches(isClickable()))
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
    fun testWelcomeMessageContent() {
        // Vérifier que le message de bienvenue contient le texte attendu
        onView(withId(R.id.tv_welcome_message))
            .check(matches(withText(containsString("TherapIA"))))
    }

    @Test
    fun testQuickAccessButtonsHaveIcons() {
        // Vérifier que les boutons ont des icônes (test basique)
        onView(withId(R.id.btn_quick_patients))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.btn_quick_agenda))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.btn_quick_sandrine))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.btn_quick_dashboard))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.btn_quick_library))
            .check(matches(hasContentDescription()))
        
        onView(withId(R.id.btn_quick_profile))
            .check(matches(hasContentDescription()))
    }
}
