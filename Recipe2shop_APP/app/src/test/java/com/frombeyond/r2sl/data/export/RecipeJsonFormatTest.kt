package com.frombeyond.r2sl.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeJsonFormatTest {

    @Test
    fun `should keep image files in recipe model`() {
        val recipe = RecipeJson(
            id = "recipe-1",
            name = "Cake",
            description = "Chocolate cake",
            servings = 4,
            workTime = 10,
            prepTime = 20,
            cookTime = 30,
            totalTime = 60,
            types = listOf("dessert"),
            tags = listOf("sweet"),
            imageUrl = "recipes/images/cake_img_001.jpg",
            imageFiles = listOf("cake_img_001.jpg", "cake_img_002.jpg"),
            ingredients = emptyList(),
            steps = emptyList(),
            metadata = RecipeMetadataJson.createDefault()
        )

        val copied = recipe.copy(name = "Cake Updated")

        assertNotNull(copied.imageFiles)
        assertEquals("recipes/images/cake_img_001.jpg", copied.imageUrl)
        assertEquals(2, copied.imageFiles?.size)
        assertTrue(copied.imageFiles?.contains("cake_img_002.jpg") == true)
    }
}
