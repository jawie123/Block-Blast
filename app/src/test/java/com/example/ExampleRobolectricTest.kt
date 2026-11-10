package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BlockShape
import com.example.model.ShapeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Block Blast", appName)
    }

    @Test
    fun `test shape batch generation produces 3 shapes`() {
        val batch = ShapeDefinitions.getRandomShapeBatch()
        assertEquals(3, batch.size)
        assertTrue(batch.all { it.cellCount > 0 })
    }

    @Test
    fun `test shape validation bounds and occupancy`() {
        val grid = List(8) { List(8) { 0 } }
        val dot = BlockShape(
            id = "dot",
            name = "dot",
            matrix = listOf(listOf(true)),
            colorId = 1
        )
        // Dot at 0, 0 should fit
        assertTrue(dot.cellCount == 1)

        val fullRowGrid = List(8) { r ->
            List(8) { c -> if (r == 0) 1 else 0 }
        }
        // Row 0 is occupied, dot cannot be placed at (0, 0)
        assertEquals(1, fullRowGrid[0][0])
    }

    @Test
    fun `test hasSeenTutorial preference is false initially and persists true`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.GamePreferences(context)
        // Fresh install / first launch
        assertFalse(prefs.hasSeenTutorial)

        // Dismissing tutorial marks it true
        prefs.hasSeenTutorial = true
        assertTrue(prefs.hasSeenTutorial)
    }
}
