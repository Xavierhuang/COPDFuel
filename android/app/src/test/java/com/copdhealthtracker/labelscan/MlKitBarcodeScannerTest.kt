package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MlKitBarcodeScannerTest {

    @Test
    fun `extracts GTIN from GS1 Digital Link`() {
        assertEquals("014200000036", MlKitBarcodeScanner.extractGtin("https://id.gs1.org/gtin/014200000036"))
    }

    @Test
    fun `extracts GTIN from raw UPC`() {
        assertEquals("036000291452", MlKitBarcodeScanner.extractGtin("036000291452"))
    }

    @Test
    fun `returns null for non-product QR`() {
        assertNull(MlKitBarcodeScanner.extractGtin("https://example.com/coupon"))
    }
}
