package com.skydown.android.data

import com.google.firebase.Timestamp
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
            Zweizwei -> "ZweiZwei"
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
    val instagramURL: String? = null,
    val spotifyURL: String? = null,
    val youtubeURL: String? = null,
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
            instagramURL,
            spotifyURL,
            youtubeURL,
        ).any { !it.isNullOrBlank() }
}

private data class ArtistPageSeed(
    val brand: ArtistPageBrand,
    val artistName: String,
) {
    val slug: String
        get() = artistPageSlug(artistName)
}

private fun artistPageSlug(artistName: String): String {
    val normalized = artistName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    return normalized.ifBlank { "artist" }
}

private fun ArtistPageSeed.toPlaceholder(now: Long = System.currentTimeMillis()): ArtistPageUi {
    return ArtistPageUi(
        slug = slug,
        brand = brand,
        artistName = artistName,
        createdAtEpochMillis = now,
        updatedAtEpochMillis = now,
        isPlaceholder = true,
    )
}

object ArtistPagesStore {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("artistPages")
    private val seedPages = listOf(
        ArtistPageSeed(ArtistPageBrand.Zweizwei, "JANNO"),
        ArtistPageSeed(ArtistPageBrand.Zweizwei, "Yang D. Nash"),
        ArtistPageSeed(ArtistPageBrand.Zweizwei, "ThaDude"),
        ArtistPageSeed(ArtistPageBrand.Zweizwei, "MAVE"),
        ArtistPageSeed(ArtistPageBrand.Zweizwei, "TANGAJOE007"),
        ArtistPageSeed(ArtistPageBrand.Skydown, "Yang D. Nash"),
        ArtistPageSeed(ArtistPageBrand.Skydown, "ThaDude"),
        ArtistPageSeed(ArtistPageBrand.Skydown, "MAVE"),
        ArtistPageSeed(ArtistPageBrand.Skydown, "JANNO"),
        ArtistPageSeed(ArtistPageBrand.Skydown, "TANGAJOE007"),
        ArtistPageSeed(ArtistPageBrand.Nicma, "NICMA MUSIC"),
    )

    private val _pages = MutableStateFlow(seedPages.map { it.toPlaceholder() })
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
        val slug = artistPageSlug(artistName)
        return pages.value.firstOrNull { it.brand == brand && it.slug == slug }
            ?: ArtistPageSeed(brand, artistName).toPlaceholder()
    }

    fun canEdit(page: ArtistPageUi, user: User?): Boolean {
        if (user?.isPlatformOwner == true) {
            return true
        }

        val userId = user?.id?.takeIf { it.isNotBlank() } ?: return false
        return page.editorUids.contains(userId)
    }

    suspend fun save(page: ArtistPageUi): Result<Unit> {
        return runCatching {
            collection.document(page.slug).set(
                mapOf(
                    "slug" to page.slug,
                    "brand" to page.brand.rawValue,
                    "artistName" to page.artistName.trim(),
                    "tagline" to page.tagline.trimmedOrNull(),
                    "bio" to page.bio.trimmedOrNull(),
                    "profileImageURL" to page.profileImageURL.trimmedOrNull(),
                    "heroImageURL" to page.heroImageURL.trimmedOrNull(),
                    "instagramURL" to page.instagramURL.trimmedOrNull(),
                    "spotifyURL" to page.spotifyURL.trimmedOrNull(),
                    "youtubeURL" to page.youtubeURL.trimmedOrNull(),
                    "editorUids" to page.editorUids
                        .mapNotNull { it.trimmedOrNull() }
                        .distinct()
                        .sorted(),
                    "createdAt" to Timestamp(page.createdAtEpochMillis / 1000, 0),
                    "updatedAt" to Timestamp.now(),
                ),
                SetOptions.merge(),
            ).await()
        }
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
                    val artistName = document.getString("artistName")?.trim().orEmpty()
                    if (artistName.isBlank()) return@mapNotNull null

                    ArtistPageUi(
                        slug = document.id,
                        brand = brand,
                        artistName = artistName,
                        tagline = document.getString("tagline")?.trimmedOrNull(),
                        bio = document.getString("bio")?.trimmedOrNull(),
                        profileImageURL = document.getString("profileImageURL")?.trimmedOrNull(),
                        heroImageURL = document.getString("heroImageURL")?.trimmedOrNull(),
                        instagramURL = document.getString("instagramURL")?.trimmedOrNull(),
                        spotifyURL = document.getString("spotifyURL")?.trimmedOrNull(),
                        youtubeURL = document.getString("youtubeURL")?.trimmedOrNull(),
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
        val pagesBySlug = remotePages.associateBy { it.slug }.toMutableMap()
        seedPages.forEach { seed ->
            if (pagesBySlug[seed.slug] == null) {
                pagesBySlug[seed.slug] = seed.toPlaceholder()
            }
        }

        return pagesBySlug.values.sortedWith(
            compareBy<ArtistPageUi>({ it.brand.rawValue }, { it.artistName.lowercase() }),
        )
    }
}

private fun String?.trimmedOrNull(): String? {
    val trimmed = this?.trim().orEmpty()
    return trimmed.ifBlank { null }
}
