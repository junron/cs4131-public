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
        val dataElements = document.selectFirst("table:contains(ACTIVE CASES)").select("td")
        val keys = mutableListOf<String>()
        val values = mutableListOf<Int>()
        dataElements.forEach {
            if(it.text().toIntOrNull() == null){
                keys += it.text()
            }else{
                values += it.text().toInt()
            }
        }
        return keys.mapIndexed { index, s -> s to values[index] }.toMap()
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
