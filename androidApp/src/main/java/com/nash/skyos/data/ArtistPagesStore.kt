package com.nash.skyos.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.skydown.shared.model.User
import com.skydown.shared.model.isPlatformOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

enum class ArtistPageBrand(val rawValue: String) {
    Zweizwei("zweizwei"),
    Skydown("skydown"),
    Nicma("nicma"),
    ;

    val displayTitle: String
        get() = when (this) {
            Zweizwei -> "22"
            Skydown -> "Skydown"
            Nicma -> "Nicma"
        }
}

data class ArtistPageUi(
    val slug: String,
    val brand: ArtistPageBrand,
    val artistName: String,
    val tagline: String? = null,
    val bio: String? = null,
    val profileImageURL: String? = null,
    val heroImageURL: String? = null,
    val heroVideoURL: String? = null,
    val instagramURL: String? = null,
    val spotifyURL: String? = null,
    val youtubeURL: String? = null,
    val studioPriceList: List<StudioPriceItemUi> = emptyList(),
    val editorUids: List<String> = emptyList(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val isPlaceholder: Boolean = false,
) {
    val hasCustomPresentation: Boolean
        get() = listOf(
            tagline,
            bio,
            profileImageURL,
            heroImageURL,
            heroVideoURL,
            instagramURL,
            spotifyURL,
            youtubeURL,
            if (studioPriceList.isEmpty()) null else "studio-prices",
        ).any { !it.isNullOrBlank() }
}

data class StudioPriceItemUi(
    val title: String,
    val detail: String,
    val price: String,
)

private fun artistPageSlug(artistName: String): String {
    val normalized = artistName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    return normalized.ifBlank { "artist" }
}

private fun artistPageDocumentId(brand: ArtistPageBrand, artistName: String): String {
    return "${brand.rawValue}-${artistPageSlug(artistName)}"
}

/**
 * Firestore-Feld `artistName` ist manchmal falsch kopiert (z. B. beide "NICMA MUSIC");
 * dann würde daraus ein falscher [ArtistPageUi.slug] gebaut und `pageFor("NICMA STUDIO")` findet das Doc nicht.
 * Die **Document-ID** (wie beim [save]) ist die verlässliche Quelle.
 */
private fun canonicalArtistNameForDocument(
    brand: ArtistPageBrand,
    documentId: String,
    storedArtistName: String,
): String {
    if (brand != ArtistPageBrand.Nicma) {
        return storedArtistName
    }
    return when (documentId) {
        "nicma-nicma-music" -> "NICMA MUSIC"
        "nicma-nicma-studio" -> "NICMA STUDIO"
        else -> storedArtistName
    }
}

private fun draftArtistPage(
    brand: ArtistPageBrand,
    artistName: String,
    now: Long = System.currentTimeMillis(),
): ArtistPageUi {
    return ArtistPageUi(
        slug = artistPageDocumentId(brand, artistName),
        brand = brand,
        artistName = artistName,
        createdAtEpochMillis = now,
        updatedAtEpochMillis = now,
        isPlaceholder = true,
    )
}

object ArtistPagesStore {
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val collection
        get() = firestore.collection("artistPages")

    private val _pages = MutableStateFlow<List<ArtistPageUi>>(emptyList())
    val pages: StateFlow<List<ArtistPageUi>> = _pages.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private var listener: ListenerRegistration? = null

    init {
        startObservation()
    }

    fun pagesForBrand(brand: ArtistPageBrand): List<ArtistPageUi> {
        return pages.value
            .filter { it.brand == brand }
            .sortedBy { it.artistName.lowercase() }
    }

    fun pageFor(brand: ArtistPageBrand, artistName: String): ArtistPageUi {
        val slug = artistPageDocumentId(brand, artistName)
        return pages.value.firstOrNull { it.brand == brand && it.slug == slug }
            ?: draftArtistPage(brand = brand, artistName = artistName)
    }

    fun documentIdFor(brand: ArtistPageBrand, artistName: String): String =
        artistPageDocumentId(brand, artistName)

    fun canEdit(page: ArtistPageUi, user: User?): Boolean {
        if (user?.isPlatformOwner == true) {
            return true
        }

        val userId = user?.id?.takeIf { it.isNotBlank() } ?: return false
        return page.editorUids.contains(userId)
    }

    suspend fun save(page: ArtistPageUi): Result<Unit> {
        return runCatching {
            val documentId = artistPageDocumentId(page.brand, page.artistName)
            collection.document(documentId).set(
                mapOf(
                    "slug" to artistPageSlug(page.artistName),
                    "brand" to page.brand.rawValue,
                    "artistName" to page.artistName.trim(),
                    "tagline" to page.tagline.trimmedOrNull(),
                    "bio" to page.bio.trimmedOrNull(),
                    "profileImageURL" to page.profileImageURL.trimmedOrNull(),
                    "heroImageURL" to page.heroImageURL.trimmedOrNull(),
                    "heroVideoURL" to page.heroVideoURL.trimmedOrNull(),
                    "instagramURL" to page.instagramURL.trimmedOrNull(),
                    "spotifyURL" to page.spotifyURL.trimmedOrNull(),
                    "youtubeURL" to page.youtubeURL.trimmedOrNull(),
                    "studioPriceList" to page.studioPriceList
                        .mapNotNull { item ->
                            val title = item.title.trimmedOrNull() ?: return@mapNotNull null
                            val detail = item.detail.trimmedOrNull() ?: return@mapNotNull null
                            val price = item.price.trimmedOrNull() ?: return@mapNotNull null
                            mapOf(
                                "title" to title,
                                "detail" to detail,
                                "price" to price,
                            )
                        },
                    "editorUids" to page.editorUids
                        .mapNotNull { it.trimmedOrNull() }
                        .distinct()
                        .sorted(),
                    "createdAt" to Timestamp(page.createdAtEpochMillis / 1000, 0),
                    "updatedAt" to Timestamp.now(),
                ),
                SetOptions.merge(),
            ).await()
            syncMirroredNicmaSocialLinksIfNeeded(page)
        }
    }

    private suspend fun syncMirroredNicmaSocialLinksIfNeeded(page: ArtistPageUi) {
        if (page.brand != ArtistPageBrand.Nicma) return
        val normalizedArtistName = page.artistName.trim().lowercase()
        val targetArtistName = when (normalizedArtistName) {
            "nicma music" -> "NICMA STUDIO"
            "nicma studio" -> "NICMA MUSIC"
            else -> return
        }
        val targetDocumentId = artistPageDocumentId(ArtistPageBrand.Nicma, targetArtistName)
        collection.document(targetDocumentId).set(
            mapOf(
                "instagramURL" to page.instagramURL.trimmedOrNull(),
                "spotifyURL" to page.spotifyURL.trimmedOrNull(),
                "youtubeURL" to page.youtubeURL.trimmedOrNull(),
                "updatedAt" to Timestamp.now(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private fun startObservation() {
        listener?.remove()
        listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _lastErrorMessage.value = error.message ?: "Artist-Seiten konnten nicht geladen werden."
                return@addSnapshotListener
            }

            val remotePages = snapshot?.documents.orEmpty()
                .mapNotNull { document ->
                    val brand = ArtistPageBrand.entries.firstOrNull {
                        it.rawValue == document.getString("brand")?.trim()?.lowercase()
                    } ?: return@mapNotNull null
                    val rawArtistName = document.getString("artistName")?.trim().orEmpty()
                    if (rawArtistName.isBlank()) return@mapNotNull null
                    val artistName = canonicalArtistNameForDocument(brand, document.id, rawArtistName)

                    ArtistPageUi(
                        slug = artistPageDocumentId(brand, artistName),
                        brand = brand,
                        artistName = artistName,
                        tagline = document.getString("tagline")?.trimmedOrNull(),
                        bio = document.getString("bio")?.trimmedOrNull(),
                        profileImageURL = document.getString("profileImageURL")?.trimmedOrNull(),
                        heroImageURL = document.getString("heroImageURL")?.trimmedOrNull(),
                        heroVideoURL = document.getString("heroVideoURL")?.trimmedOrNull(),
                        instagramURL = document.readArtistSocialUrl(
                            "instagram",
                            "instagramURL", "instagramUrl", "instagram", "instagramURLString",
                        ),
                        spotifyURL = document.readArtistSocialUrl(
                            "spotify",
                            "spotifyURL", "spotifyUrl", "spotify", "spotifyUrlString",
                        ),
                        youtubeURL = document.readArtistSocialUrl(
                            "youtube",
                            "youtubeURL", "youtubeUrl", "youtube", "youtubeURLString", "youTubeURL", "youTube",
                        ),
                        studioPriceList = ((document.get("studioPriceList") as? List<*>) ?: emptyList<Any>())
                            .mapNotNull { entry ->
                                val map = entry as? Map<*, *> ?: return@mapNotNull null
                                val title = (map["title"] as? String)?.trimmedOrNull() ?: return@mapNotNull null
                                val detail = (map["detail"] as? String)?.trimmedOrNull() ?: return@mapNotNull null
                                val price = (map["price"] as? String)?.trimmedOrNull() ?: return@mapNotNull null
                                StudioPriceItemUi(title = title, detail = detail, price = price)
                            },
                        editorUids = (document.get("editorUids") as? List<*>)?.mapNotNull { (it as? String)?.trimmedOrNull() }.orEmpty(),
                        createdAtEpochMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedAtEpochMillis = document.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        isPlaceholder = false,
                    )
                }

            val merged = mergePages(remotePages)
            _pages.value = merged
            _lastErrorMessage.value = null
        }
    }

    private fun mergePages(remotePages: List<ArtistPageUi>): List<ArtistPageUi> {
        val pagesBySlug = linkedMapOf<String, ArtistPageUi>()
        remotePages.forEach { page ->
            val existing = pagesBySlug[page.slug]
            pagesBySlug[page.slug] = when {
                existing == null -> page
                existing.updatedAtEpochMillis != page.updatedAtEpochMillis ->
                    if (existing.updatedAtEpochMillis > page.updatedAtEpochMillis) existing else page
                existing.isPlaceholder != page.isPlaceholder ->
                    if (existing.isPlaceholder) page else existing
                else -> existing
            }
        }

        return pagesBySlug.values.sortedWith(
            compareBy<ArtistPageUi>({ it.brand.rawValue }, { it.artistName.lowercase() }),
        )
    }
}

/**
 * Manche aeltere oder manuell gepflegte Docs nutzen leicht abweichende Schluessel – gleich zu iOS-Video-Hub-Maps.
 */
private fun DocumentSnapshot.readFirstNonBlankString(vararg fieldNames: String): String? {
    for (name in fieldNames) {
        getString(name)?.trimmedOrNull()?.let { return it }
    }
    return null
}

/**
 * Wie [readFirstNonBlankString], zusaetzlich in `social` / `links` / `link` Maps
 * (typisch bei per Konsole gepflegten oder aelteren Shapes).
 */
private fun DocumentSnapshot.readArtistSocialUrl(
    groupKey: String,
    vararg topLevelNames: String,
): String? {
    readFirstNonBlankString(*topLevelNames)?.let { return it }
    when (val raw = get(groupKey)) {
        is String -> raw.trimmedOrNull()?.let { return it }
        is Map<*, *> -> {
            for (k in listOf("url", "link", "href", "u")) {
                (raw[k] as? String)?.trimmedOrNull()?.let { return it }
            }
        }
    }
    val nested = listOf("social", "links", "link", "${groupKey}URL")
    val nestedKeyCandidates = when (groupKey) {
        "instagram" -> listOf("instagram", "instagramURL", "instagramUrl", "ig")
        "spotify" -> listOf("spotify", "spotifyURL", "spotifyUrl", "s")
        "youtube" -> listOf("youtube", "youtubeURL", "youtubeUrl", "yt", "youTube", "youTubeURL")
        else -> emptyList()
    }
    for (mapName in nested) {
        val m = get(mapName) as? Map<*, *> ?: continue
        for (k in nestedKeyCandidates + "url" + "link" + "href") {
            (m[k] as? String)?.trimmedOrNull()?.let { return it }
        }
    }
    return null
}

private fun String?.trimmedOrNull(): String? {
    val trimmed = this?.trim().orEmpty()
    return trimmed.ifBlank { null }
}
