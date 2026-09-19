package com.copdhealthtracker.labelscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProductLinkResolverTest {

    @Test
    fun `finds the UPC printed on a SmartLabel page`() {
        // Snippet from the live Quaker SmartLabel page
        val html = """<div><span class="Upc"> UPC </span> 030000570630</div>"""

        assertEquals("030000570630", ProductLinkResolver.findGtinInHtml(html))
    }

    @Test
    fun `finds a schema org gtin in page markup`() {
        val html = """<script type="application/ld+json">{"@type":"Product","gtin13":"0030000570630"}</script>"""

        assertEquals("0030000570630", ProductLinkResolver.findGtinInHtml(html))
    }

    @Test
    fun `ignores numbers that are not labelled as a product code`() {
        val html = """<p>Call 1-800-367-6287 or order 030000570630 today</p>"""

        assertNull(ProductLinkResolver.findGtinInHtml(html))
    }

    @Test
    fun `ignores labelled numbers with a bad check digit`() {
        val html = """<span>UPC</span> 030000570631"""

        assertNull(ProductLinkResolver.findGtinInHtml(html))
    }

    @Test
    fun `only web links are resolvable`() {
        assertTrue(ProductLinkResolver.isWebLink("https://qrs.ly/abc123"))
        assertTrue(ProductLinkResolver.isWebLink("HTTP://scn.by/x"))
        assertFalse(ProductLinkResolver.isWebLink("WIFI:S:home;T:WPA;P:secret;;"))
        assertFalse(ProductLinkResolver.isWebLink(null))
    }

    // The real redirect chain behind the QR on a Quaker oatmeal cup (http://pepsico.info/490lz7).
    private val quakerChain = mapOf(
        "http://pepsico.info/490lz7" to ProductLinkResolver.Hop(301, "https://pepsi.scb.ai/490lz7", null),
        "https://pepsi.scb.ai/490lz7" to ProductLinkResolver.Hop(302, "http://app.scanlife.com/resolver/shorturl/490lz7", null),
        "https://app.scanlife.com/resolver/shorturl/490lz7" to ProductLinkResolver.Hop(302, "http://app.scanlife.com/resolver/shorturl/490lz7?proxy=false", null),
        "https://app.scanlife.com/resolver/shorturl/490lz7?proxy=false" to ProductLinkResolver.Hop(302, "http://app.scanlife.com/resolver/dw/490lz7?proxy=false", null),
        "https://app.scanlife.com/resolver/dw/490lz7?proxy=false" to ProductLinkResolver.Hop(302, "http://app.scanlife.com/resolver/codeexec?barcode=f51a843&rd=1", null),
        "https://app.scanlife.com/resolver/codeexec?barcode=f51a843&rd=1" to ProductLinkResolver.Hop(302, "https://menu.myproduct.info/5125a12d/index.html?cname=00030000570630_32865706304_BEM_Quaker_BR&scantime=2026-09-19T05%3A09%3A09Z", null)
    )

    private fun fakeFetch(requested: MutableList<String>) = { url: String ->
        requested.add(url)
        quakerChain[url] ?: throw java.io.IOException("no route to $url")
    }

    @Test
    fun `follows the real seven hop SmartLabel chain to the UPC`() = runBlocking {
        val requested = mutableListOf<String>()

        val gtin = ProductLinkResolver(fakeFetch(requested)).resolveGtin("http://pepsico.info/490lz7")

        assertEquals("00030000570630", gtin)
    }

    @Test
    fun `falls back to plain http when the https version of a link fails`() = runBlocking {
        val requested = mutableListOf<String>()

        ProductLinkResolver(fakeFetch(requested)).resolveGtin("http://pepsico.info/490lz7")

        // pepsico.info has an expired certificate, so https is tried first and http second.
        assertEquals(listOf("https://pepsico.info/490lz7", "http://pepsico.info/490lz7"), requested.take(2))
        // app.scanlife.com is linked over http but works over https, so it is never fetched in the clear.
        assertTrue(requested.none { it.startsWith("http://app.scanlife.com") })
    }
}
