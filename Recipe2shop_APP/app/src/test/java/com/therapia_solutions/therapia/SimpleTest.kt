package com.therapia_solutions.therapia

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests simples pour vérifier que l'environnement de test fonctionne
 */
class SimpleTest {

    @Test
    fun `test basic arithmetic`() {
        // Given
        val a = 2
        val b = 3

        // When
        val result = a + b

        // Then
        assertEquals("2 + 3 should equal 5", 5, result)
    }

    @Test
    fun `test string operations`() {
        // Given
        val str1 = "Hello"
        val str2 = "World"

        // When
        val result = "$str1 $str2"

        // Then
        assertEquals("String concatenation should work", "Hello World", result)
    }

    @Test
    fun `test list operations`() {
        // Given
        val list = listOf(1, 2, 3, 4, 5)

        // When
        val sum = list.sum()
        val size = list.size

        // Then
        assertEquals("Sum should be 15", 15, sum)
        assertEquals("Size should be 5", 5, size)
    }

    @Test
    fun `test null safety`() {
        // Given
        val nullableString: String? = null

        // When
        val result = nullableString?.length ?: 0

        // Then
        assertEquals("Null safety should work", 0, result)
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
}
