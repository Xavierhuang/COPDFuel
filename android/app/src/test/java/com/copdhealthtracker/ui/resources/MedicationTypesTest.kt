package com.copdhealthtracker.ui.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationTypesTest {

    // The ids of the ten icons in Resp. Care > Medication Types.
    private val iconIds = listOf(
        "bronchodilators", "ics", "combination", "pde4", "antibiotics",
        "systemic", "methylxanthines", "mucolytics", "biologics", "nebulizer"
    )

    @Test
    fun `every medication type icon has an explanation`() {
        iconIds.forEach { id -> assertNotNull("no explanation for $id", MedicationTypes.forId(id)) }
        assertEquals(iconIds.size, MedicationTypes.ALL.size)
    }

    @Test
    fun `each explanation says what it is, how it helps, and gives examples`() {
        MedicationTypes.ALL.forEach { info ->
            assertTrue("${info.id}: what it is", info.whatItIs.length > 30)
            assertTrue("${info.id}: how it helps", info.howItHelps.length > 30)
            assertTrue("${info.id}: examples", info.examples.size >= 2)
        }
    }

    @Test
    fun `the message shown in the pop-up has all three parts and a safety note`() {
        val message = MedicationTypes.forId("bronchodilators")!!.message()

        assertTrue(message.contains("What it is"))
        assertTrue(message.contains("How it helps"))
        assertTrue(message.contains("Examples"))
        assertTrue(message.contains("Albuterol"))
        assertTrue(message.contains(MedicationTypes.SAFETY_NOTE))
    }

    @Test
    fun `an unknown id has no explanation`() {
        assertNull(MedicationTypes.forId("not-a-type"))
    }
}
