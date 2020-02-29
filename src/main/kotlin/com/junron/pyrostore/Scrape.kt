package com.junron.pyrostore

import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Scrape {
    private lateinit var document: Document
    fun initDocument() {
        document = Jsoup.connect("https://www.moh.gov.sg/covid-19/").get()
    }

    fun getDorscon(): Dorscon {
        val dataElement = document.select("td:contains(DORSCON level)")
            .first()
            .parent()
            .select("td")[1]
        return Dorscon.valueOf(dataElement.text().toUpperCase())
    }

    fun getCaseData(): Map<String, Int> {
        val dataElements = document.selectFirst("table:contains(Confirmed cases)").select("tr")
        return dataElements.map {
            val children = it.select("td")
            val name = children.first().text()
            val data = children.last().text().toInt()
            name to data
        }.toMap()
    }

    fun getLastUpdated() = document
        .selectFirst("span:contains(Case Summary in Singapore)")
        .text()
        .substringAfter("as of ")
        .substringBefore(")")
}

@Serializable
enum class Dorscon {
    GREEN, YELLOW, ORANGE, RED
}

@Serializable
data class FullResponse(
    val dorscon: Dorscon,
    val caseData: Map<String, Int>,
    val lastUpdated: String
)
