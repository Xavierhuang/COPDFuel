package com.copdhealthtracker.labelscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Package QR codes (e.g. SmartLabel) usually hold a short link that redirects to a product page.
 * Follows that link and returns the product's GTIN/UPC from the final address or the page itself.
 */
class ProductLinkResolver(private val fetch: (String) -> Hop = ::httpFetch) {

    /** One HTTP response: a redirect carries [location], a page carries [body]. */
    data class Hop(val status: Int, val location: String?, val body: String?)

    suspend fun resolveGtin(link: String): String? = withContext(Dispatchers.IO) {
        var current = link.trim()
        repeat(MAX_HOPS) {
            MlKitBarcodeScanner.extractGtin(current)?.let { return@withContext it }

            val hop = fetchPreferringHttps(current)
            if (hop.status in 300..399 && hop.location != null) {
                current = URL(URL(current), hop.location).toString()
            } else if (hop.status == HttpURLConnection.HTTP_OK) {
                return@withContext findGtinInHtml(hop.body.orEmpty())
            } else {
                return@withContext null
            }
        }
        MlKitBarcodeScanner.extractGtin(current)
    }

    // Short-link services still hand out http:// addresses. Most also answer over https, so try
    // that first; plain http only succeeds for hosts allowed in network_security_config.xml.
    private fun fetchPreferringHttps(url: String): Hop {
        if (!url.startsWith("http://", ignoreCase = true)) return fetch(url)
        return try {
            fetch("https" + url.substring(4))
        } catch (e: IOException) {
            fetch(url)
        }
    }

    companion object {
        // The QR on a Quaker cup takes 7 redirects to reach its product page.
        private const val MAX_HOPS = 10
        private const val TIMEOUT_MS = 8000
        private const val MAX_PAGE_CHARS = 200_000
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

        // A number only counts when the page labels it as a product code and its check digit is valid.
        private val labelledCode = Regex("(?i)(?:gtin\\d{0,2}|upc)(?:<[^>]*>|[^0-9<]){0,40}?(\\d{12,14})(?!\\d)")

        fun findGtinInHtml(html: String): String? =
            labelledCode.findAll(html)
                .map { it.groupValues[1] }
                .firstOrNull { MlKitBarcodeScanner.hasValidCheckDigit(it) }

        fun isWebLink(value: String?): Boolean =
            value != null && Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(value.trim())

        private fun httpFetch(url: String): Hop {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                val status = connection.responseCode
                val location = connection.getHeaderField("Location")
                val body = if (status == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { reader ->
                        val text = StringBuilder()
                        val buffer = CharArray(8192)
                        while (text.length < MAX_PAGE_CHARS) {
                            val read = reader.read(buffer)
                            if (read < 0) break
                            text.append(buffer, 0, read)
                        }
                        text.toString()
                    }
                } else {
                    null
                }
                return Hop(status, location, body)
            } finally {
                connection.disconnect()
            }
        }
    }
}
