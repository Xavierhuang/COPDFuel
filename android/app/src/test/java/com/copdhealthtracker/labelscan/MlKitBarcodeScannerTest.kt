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
    fun `extracts GTIN embedded in a product page link`() {
        // Real SmartLabel address for Quaker protein oatmeal
        assertEquals(
            "030000570630",
            MlKitBarcodeScanner.extractGtin("https://smartlabel.pepsico.info/030000570630-0001-en-US/index.html")
        )
    }

    @Test
    fun `ignores long numbers in links that are not valid GTINs`() {
        assertNull(MlKitBarcodeScanner.extractGtin("https://example.com/order/123456789013"))
    }

    @Test
    fun `returns null for non-product QR`() {
        assertNull(MlKitBarcodeScanner.extractGtin("https://example.com/coupon"))
    }
}
