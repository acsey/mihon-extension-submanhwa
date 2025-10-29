package eu.kanade.tachiyomi.extension.en.submanhwa

import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class SubManhwaEN : ParsedHttpSource() {

    override val name = "SubManhwa (EN)"
    override val baseUrl = "https://submanhwa.com"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient = network.client

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/serie-list?page=$page")
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/serie-list?page=$page")
    override fun popularMangaSelector(): String = listItemSelector()
    override fun latestUpdatesSelector(): String = listItemSelector()

    private fun listItemSelector() = "a[href^=\"/serie/\"]"

    private fun mangaFromListElement(a: Element): SManga {
        val m = SManga.create()
        m.title = a.text().trim().ifBlank { a.attr("title").trim() }
        m.setUrlWithoutDomain(a.absUrl("href").removePrefix(baseUrl))
        m.thumbnail_url = a.parent()?.selectFirst("img[src],img[data-src]")
            ?.let { it.absUrl("src").ifBlank { it.absUrl("data-src") } }
        return m
    }
    override fun popularMangaFromElement(element: Element): SManga = mangaFromListElement(element)
    override fun latestUpdatesFromElement(element: Element): SManga = mangaFromListElement(element)
    override fun popularMangaNextPageSelector(): String = "a[rel=\"next\"], a:containsOwn(»)"
    override fun latestUpdatesNextPageSelector(): String = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val q = query.trim()
        val url = if (q.isBlank()) "$baseUrl/serie-list?page=$page" else "$baseUrl/?s=${java.net.URLEncoder.encode(q, "UTF-8")}"
        return GET(url)
    }
    override fun searchMangaSelector(): String = listItemSelector()
    override fun searchMangaFromElement(element: Element): SManga = mangaFromListElement(element)
    override fun searchMangaNextPageSelector(): String = popularMangaNextPageSelector()

    override fun mangaDetailsRequest(manga: SManga): Request = GET(baseUrl + manga.url)
    override fun mangaDetailsParse(document: Document): SManga {
        val info = SManga.create()
        info.title = document.selectFirst("h1,h2,h3")?.text()?.trim()
            ?: document.title().substringBefore(" -").trim()
        val desc = document.select("div:matchesOwn(^\\s*Summary$), p, article").joinToString("\n") { it.text() }
        info.description = desc.trim().ifBlank { null }
        info.thumbnail_url = document.selectFirst("img[src*=\"/uploads\"], img[src*=\"/wp-content\"], img[data-src]")
            ?.let { it.absUrl("src").ifBlank { it.absUrl("data-src") } }
        info.genre = document.select("a[href*=\"/category/\"], a[href*=\"/tag/\"]").eachText().joinToString().ifBlank { null }
        return info
    }

    override fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url)
    override fun chapterListSelector(): String = "a[href^=\"/serie/\"][href*=\"/\"]"
    override fun chapterFromElement(element: Element): SChapter {
        val c = SChapter.create()
        c.name = element.text().trim().ifBlank { "Chapter" }
        c.setUrlWithoutDomain(element.absUrl("href").removePrefix(baseUrl))
        val dateText = element.parent()?.previousElementSibling()?.text()?.trim()
        c.date_upload = parseDate(dateText)
        return c
    }

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val patterns = listOf("dd MMM yyyy", "dd MMM. yyyy", "dd MMM yyyy.", "dd MMM.")
        for (p in patterns) {
            runCatching {
                val fmt = SimpleDateFormat(p, Locale.ENGLISH)
                return fmt.parse(raw.replace(".", ""))?.time ?: 0L
            }
        }
        return 0L
    }

    override fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url)
    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        fun parseImgs(doc: Document) {
            var idx = pages.size
            val imgs = doc.select("img[src],img[data-src]")
            imgs.forEach {
                val url = it.absUrl("src").ifBlank { it.absUrl("data-src") }
                if (url.isNotBlank() && likelyPageImage(url)) {
                    pages += Page(idx++, "", url)
                }
            }
        }
        parseImgs(document)
        if (pages.isEmpty()) {
            val tries = listOf("?all=1", "?style=list")
            for (t in tries) {
                val u = document.location().removeSuffix("/") + t
                val resp = client.newCall(GET(u)).execute()
                resp.use { r ->
                    val doc2 = r.asJsoup()
                    parseImgs(doc2)
                    if (pages.isNotEmpty()) break
                }
            }
        }
        if (pages.isEmpty()) {
            val scripts = document.select("script").html()
            val re = Regex("""https?://[^"']+\\.(?:jpe?g|png|webp)""", RegexOption.IGNORE_CASE)
            val urls = re.findAll(scripts).map { it.value }.distinct().toList()
            pages += urls.mapIndexed { i, u -> Page(i, "", u) }
        }
        if (pages.isEmpty()) throw Exception("No page images found.")
        return pages
    }

    private fun likelyPageImage(u: String): Boolean {
        val L = u.lowercase()
        return listOf(".jpg",".jpeg",".png",".webp").any { L.contains(it) } &&
                !L.contains("logo") && !L.contains("icon") && !L.contains("avatar")
    }

    override fun imageRequest(page: Page): Request = GET(page.imageUrl!!)
    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()
}
