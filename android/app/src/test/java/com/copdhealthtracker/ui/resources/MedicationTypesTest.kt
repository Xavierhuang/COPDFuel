package com.copdhealthtracker.ui.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationTypesTest {

    // The ids of the ten icons in the Resp. Care tab.
    private val iconIds = listOf(
        "bronchodilators", "ics", "combination", "pde4", "antibiotics",
        "systemic", "methylxanthines", "mucolytics", "biologics", "nebulizer"
    )

    @Test
    fun `every medication type icon has a guide`() {
        iconIds.forEach { id -> assertNotNull("no guide for $id", MedicationTypes.forId(id)) }
        assertEquals(iconIds.size, MedicationTypes.ALL.size)
    }

    @Test
    fun `every guide fills in every section`() {
        MedicationTypes.ALL.forEach { info ->
            assertTrue("${info.id}: examples", info.examples.isNotEmpty())
            info.sections().forEach { (heading, body) ->
                assertTrue("${info.id}: '$heading' is too short", body.trim().length > 40)
            }
        }
    }

    @Test
    fun `sections appear in the order a patient reads them`() {
        val headings = MedicationTypes.forId("bronchodilators")!!.sections().map { it.first }

        assertEquals(
            listOf(
                "Examples", "What it is", "What it's for", "How it works", "Common forms",
                "How to use", "Common side effects", "Warnings", "Interactions", "Important"
            ),
            headings
        )
    }

    @Test
    fun `every guide ends with the disclaimer`() {
        MedicationTypes.ALL.forEach { info ->
            assertEquals("${info.id}", MedicationTypes.DISCLAIMER, info.sections().last().second)
        }
        assertTrue(MedicationTypes.DISCLAIMER.contains("not medical advice"))
    }

    @Test
    fun `the plain text version carries every heading and example`() {
        val message = MedicationTypes.forId("bronchodilators")!!.message()

        MedicationTypes.forId("bronchodilators")!!.sections().forEach { (heading, _) ->
            assertTrue("missing heading $heading", message.contains(heading))
        }
        assertTrue(message.contains("Albuterol"))
    }

    @Test
    fun `an unknown id has no guide`() {
        assertNull(MedicationTypes.forId("not-a-type"))
    }

    @Test
    fun `no guide mentions Fasenra`() {
        // Removed at the owner's request: benralizumab is not an approved COPD treatment.
        MedicationTypes.ALL.forEach { info ->
            val text = info.message().lowercase()
            assertTrue("${info.id} mentions Fasenra", !text.contains("fasenra") && !text.contains("benralizumab"))
        }
    }
}
