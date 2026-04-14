package com.lillesand.badetemp.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class BathingRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val url = "https://www.lillesand.kommune.no/Badeplasser.html"

    /**
     * Fetches bathing locations and temperatures by scraping the municipality page.
     * Returns a list of BathingLocation or throws on network/parse error.
     */
    suspend fun fetchLocations(): List<BathingLocation> {
        val html = fetchHtml()
        return parseLocations(html)
    }

    private fun fetchHtml(): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; LillesandBadeapp/1.0)")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("HTTP ${response.code}")
        return response.body?.string() ?: error("Empty response body")
    }

    private fun parseLocations(html: String): List<BathingLocation> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<BathingLocation>()

        // Strategy 1: find a table — the page likely uses a <table> for the data
        val tables = doc.select("table")
        for (table in tables) {
            val rows = table.select("tr")
            for (row in rows) {
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val nameText = cells[0].text().trim()
                    val tempText = cells[1].text().trim()
                    val temp = extractTemperature(tempText)
                    if (nameText.isNotEmpty() && temp != null) {
                        results.add(BathingLocation(name = nameText, temperature = temp))
                    }
                }
            }
        }
        if (results.isNotEmpty()) return results

        // Strategy 2: scan all elements for temperature pattern near a label
        val tempRegex = Regex("""(\d+[.,]\d+)\s*°C""")
        val elements = doc.select("p, li, td, div, span")
        for (el in elements) {
            val text = el.text().trim()
            val match = tempRegex.find(text)
            if (match != null) {
                // Temperature found — use text before it as name
                val before = text.substring(0, match.range.first).trim().trimEnd(':', '-', ' ')
                if (before.isNotEmpty() && before.length < 60) {
                    val temp = match.groupValues[1].replace(',', '.').toDoubleOrNull()
                    if (temp != null) {
                        results.add(BathingLocation(name = before, temperature = temp))
                    }
                }
            }
        }
        if (results.isNotEmpty()) return deduplicateByName(results)

        // Strategy 3: regex scan over raw text lines
        val lines = doc.body().text().lines()
        val lineTemp = Regex("""^(.+?)\s{2,}(\d+[.,]\d+)\s*°C""")
        for (line in lines) {
            val m = lineTemp.find(line.trim()) ?: continue
            val name = m.groupValues[1].trim()
            val temp = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: continue
            if (name.length in 3..60) {
                results.add(BathingLocation(name = name, temperature = temp))
            }
        }
        return deduplicateByName(results)
    }

    private fun extractTemperature(text: String): Double? {
        val regex = Regex("""(\d+[.,]\d+)""")
        val match = regex.find(text) ?: return null
        return match.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    private fun deduplicateByName(list: List<BathingLocation>): List<BathingLocation> {
        return list.distinctBy { it.name }
    }
}
