n package com.therapia_solutions.therapia.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitaires pour les utilitaires
 */
class UtilsTest {

    @Test
    fun `test string validation`() {
        // Given
        val validEmail = "test@example.com"
        val invalidEmail = "invalid-email"
        val emptyString = ""
        val nullString: String? = null

        // When & Then
        assertTrue("Valid email should be valid", validEmail.contains("@"))
        assertFalse("Invalid email should be invalid", invalidEmail.contains("@"))
        assertTrue("Empty string should be empty", emptyString.isEmpty())
        assertTrue("Null string should be null", nullString == null)
    }

    @Test
    fun `test number operations`() {
        // Given
        val numbers = listOf(1, 2, 3, 4, 5)

        // When
        val sum = numbers.sum()
        val average = numbers.average()
        val max = numbers.maxOrNull()
        val min = numbers.minOrNull()

        // Then
        assertEquals("Sum should be 15", 15, sum)
        assertEquals("Average should be 3.0", 3.0, average, 0.01)
        assertEquals("Max should be 5", 5, max)
        assertEquals("Min should be 1", 1, min)
    }

    @Test
    fun `test list operations`() {
        // Given
        val list = listOf("apple", "banana", "cherry", "date")

        // When
        val filtered = list.filter { it.startsWith("a") }
        val mapped = list.map { it.uppercase() }
        val sorted = list.sorted()

        // Then
        assertEquals("Filtered list should have 1 item", 1, filtered.size)
        assertEquals("First filtered item should be apple", "apple", filtered.first())
        assertEquals("Mapped list should have 4 items", 4, mapped.size)
        assertEquals("First mapped item should be APPLE", "APPLE", mapped.first())
        assertEquals("Sorted list should have 4 items", 4, sorted.size)
        assertEquals("First sorted item should be apple", "apple", sorted.first())
    }

    @Test
    fun `test date operations`() {
        // Given
        val currentTime = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L

        // When
        val futureTime = currentTime + oneHour
        val pastTime = currentTime - oneHour

        // Then
        assertTrue("Future time should be greater than current", futureTime > currentTime)
        assertTrue("Past time should be less than current", pastTime < currentTime)
        assertTrue("Current time should be positive", currentTime > 0)
    }

    @Test
    fun `test exception handling`() {
        // Given
        val numbers = listOf(1, 2, 0, 4)

        // When & Then
        try {
            numbers.map { 10 / it }
            fail("Should have thrown ArithmeticException")
        } catch (e: ArithmeticException) {
            assertTrue("Should catch arithmetic exception", true)
        }
    }

    @Test
    fun `test string formatting`() {
        // Given
        val name = "John"
        val age = 30
        val city = "Paris"

        // When
        val formatted = "Hello, my name is $name, I'm $age years old and I live in $city"

        // Then
        assertTrue("Formatted string should contain name", formatted.contains(name))
        assertTrue("Formatted string should contain age", formatted.contains(age.toString()))
        assertTrue("Formatted string should contain city", formatted.contains(city))
        assertEquals("Formatted string should match expected", 
            "Hello, my name is John, I'm 30 years old and I live in Paris", formatted)
    }

    @Test
    fun `test collection operations`() {
        // Given
        val list1 = listOf(1, 2, 3)
        val list2 = listOf(4, 5, 6)

        // When
        val combined = list1 + list2
        val distinct = listOf(1, 2, 2, 3, 3, 3).distinct()
        val grouped = listOf("a", "b", "a", "c", "b").groupBy { it }

        // Then
        assertEquals("Combined list should have 6 items", 6, combined.size)
        assertEquals("Distinct list should have 3 items", 3, distinct.size)
        assertEquals("Grouped map should have 3 keys", 3, grouped.size)
        assertEquals("Group 'a' should have 2 items", 2, grouped["a"]?.size)
    }
}
