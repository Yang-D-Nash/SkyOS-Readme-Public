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
    val tagline: String? = null,
    val bio: String? = null,
    val instagramURL: String? = null,
    val spotifyURL: String? = null,
    val youtubeURL: String? = null,
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
        tagline = tagline,
        bio = bio,
        instagramURL = instagramURL,
        spotifyURL = spotifyURL,
        youtubeURL = youtubeURL,
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
    private val seedPages = listOf(
        ArtistPageSeed(
            ArtistPageBrand.Zweizwei,
            "JANNO",
            tagline = "Melodic street energy und klare Hooks.",
            bio = "JANNO bringt Druck, Gefuehl und direkte Hook-Momente zusammen. Auf dieser Seite laufen Releases, Top Songs und die wichtigsten Links direkt fuer neue Hoerer zusammen.",
            instagramURL = "https://www.instagram.com/janno_official_/",
            spotifyURL = "https://open.spotify.com/artist/7hpiHzP9aLLb5liDLxtwhM",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Zweizwei,
            "Yang D. Nash",
            tagline = "Zwischen Skydown, Vision und Release-Fokus.",
            bio = "Yang D. Nash verbindet Artist-Energie mit Creative Direction. Songs, Visuals und der ganze Skydown-Kosmos laufen hier zusammen.",
            instagramURL = "https://www.instagram.com/y.d.nash/",
            spotifyURL = "https://open.spotify.com/artist/63Sh0kQAWW3ZWn2aKDksbo",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Zweizwei,
            "ThaDude",
            tagline = "Roh, direkt und mit klarer Attitude.",
            bio = "ThaDude steht fuer druckvolle Tracks, direkte Delivery und den schnellen Weg von der Hook in den Kopf. Hier finden User Songs, Profil und Links an einem Ort.",
            instagramURL = "https://www.instagram.com/thadude_offizielle/",
            spotifyURL = "https://open.spotify.com/artist/0Jmb7DXFkKxxRjqD70vi0e",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Zweizwei,
            "MAVE",
            tagline = "Melodien, Atmosphaere und naechtlicher Zug.",
            bio = "MAVE bringt melodische Momente, dunklere Stimmungen und eine klare Release-Aesthetik zusammen. Die Artist-Page ist der direkte Einstieg fuer neue Hoerer.",
            instagramURL = "https://www.instagram.com/mave__official/",
            spotifyURL = "https://open.spotify.com/artist/0GXymtRaIk2ngbXSkcHtsp",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Zweizwei,
            "TANGAJOE007",
            tagline = "Raw voice, klare Kante, direkter Vibe.",
            bio = "TANGAJOE007 steht fuer direkte Energie und eine praesente Stimme. Hier landen Songs, Profil und Socials gebuendelt in einem starken Artist-Entrance.",
            instagramURL = "https://www.instagram.com/tangajoe007/",
            spotifyURL = "https://open.spotify.com/artist/0OA5dgpVdwzI8K82m8FPxN",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Skydown,
            "Yang D. Nash",
            tagline = "Skydown founder energy trifft Release-Fokus.",
            bio = "Yang D. Nash verbindet Music, Creative Direction und Storytelling. Die Seite gibt neuen Usern direkt einen sauberen Einstieg in Songs, Releases und Kanaele.",
            instagramURL = "https://www.instagram.com/y.d.nash/",
            spotifyURL = "https://open.spotify.com/artist/63Sh0kQAWW3ZWn2aKDksbo",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Skydown,
            "ThaDude",
            tagline = "Direkter Rap mit Kante und Haltung.",
            bio = "ThaDude liefert rohe Energie und markante Delivery. Diese Seite fuehrt direkt zu Songs, Profil und den wichtigsten Plattformen.",
            instagramURL = "https://www.instagram.com/thadude_offizielle/",
            spotifyURL = "https://open.spotify.com/artist/0Jmb7DXFkKxxRjqD70vi0e",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Skydown,
            "MAVE",
            tagline = "Melodic mood und klare Release-Atmosphaere.",
            bio = "MAVE bringt Stimmung, Timing und melodische Flaechen zusammen. User bekommen hier den schnellen Einstieg in Songs und Socials.",
            instagramURL = "https://www.instagram.com/mave__official/",
            spotifyURL = "https://open.spotify.com/artist/0GXymtRaIk2ngbXSkcHtsp",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Skydown,
            "JANNO",
            tagline = "Melodic street energy und klare Hooks.",
            bio = "JANNO verbindet Druck, Gefuehl und direkte Hook-Momente. Diese Seite holt neue Hoerer direkt in die Releases und Songs rein.",
            instagramURL = "https://www.instagram.com/janno_official_/",
            spotifyURL = "https://open.spotify.com/artist/7hpiHzP9aLLb5liDLxtwhM",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Skydown,
            "TANGAJOE007",
            tagline = "Roh, direkt und voller Praesenz.",
            bio = "TANGAJOE007 bringt Stimme, Kante und Attitude in jeden Track. Songs und Links sind hier bewusst direkt erreichbar.",
            instagramURL = "https://www.instagram.com/tangajoe007/",
            spotifyURL = "https://open.spotify.com/artist/0OA5dgpVdwzI8K82m8FPxN",
        ),
        ArtistPageSeed(
            ArtistPageBrand.Nicma,
            "NICMA MUSIC",
            tagline = "Studio, Production und Sound-Handwerk.",
            bio = "NICMA MUSIC ist die Producer- und Studio-Seite fuer Recording, Mix, Master und Sound-Entwicklung. Hier finden User Sound, Referenzen und direkte Kontaktwege.",
            instagramURL = "https://www.instagram.com/nicma.music/",
            spotifyURL = "https://open.spotify.com/artist/0OoRIo7pJjtLgg3qyf1oDS",
        ),
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
