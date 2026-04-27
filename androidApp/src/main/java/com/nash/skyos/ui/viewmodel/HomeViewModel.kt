package com.nash.skyos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nash.skyos.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nash.skyos.data.ExternalMediaProvider
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.model.FeaturedVideoHighlight
import com.nash.skyos.ui.model.HomeUiState
import com.nash.skyos.ui.model.ProductivityNoteItem
import com.nash.skyos.ui.model.ProductivityReminderItem
import com.nash.skyos.ui.model.ProductivityTaskItem
import com.nash.skyos.productivity.ProductivityReminderNotificationCenter
import com.skydown.shared.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val musicService = AppContainer.musicService
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var refreshGeneration: Long = 0

    /** SkyOS Home notes auto-expire (align with creator-tier AI note retention messaging). */
    private val noteRetentionMillis: Long = 7L * 24L * 60L * 60L * 1000L

    private fun resString(resId: Int) = getApplication<Application>().getString(resId)

    private fun resString(resId: Int, vararg formatArgs: Any) =
        getApplication<Application>().getString(resId, *formatArgs)

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(app) as T
                }
                throw IllegalArgumentException(
                    app.getString(
                        R.string.home_error_unknown_viewmodel_class,
                        modelClass,
                    ),
                )
            }
        }
    }

    private val featuredArtists = listOf(
        "JANNO",
        "Yang D. Nash",
        "ThaDude",
        "MAVE",
        "TANGAJOE007",
    )

    init {
        refresh()
    }

    fun refresh() {
        val generation = ++refreshGeneration
        viewModelScope.launch {
            supervisorScope {
                launch {
                    val latestTrack = loadLatestTrack()
                    if (!isCurrentRefresh(generation)) return@launch
                    applyFeaturedTrackUpdate(generation, latestTrack)
                }

                launch {
                    val latestVideo = loadLatestVideo()
                    if (!isCurrentRefresh(generation)) return@launch
                    applyFeaturedVideoUpdate(generation, latestVideo)
                }

                launch {
                    loadRuntimeSignals(generation)
                }

                launch {
                    loadProductivitySnapshot(generation)
                }
            }
        }
    }

    fun createReminder(title: String, dueAt: Date) {
        val uid = auth.currentUser?.uid ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            runCatching {
                val ref = firestore.collection("users").document(uid).collection("reminders").document()
                ref.set(
                    mapOf(
                        "title" to normalizedTitle,
                        "scheduledAt" to Timestamp(dueAt),
                        "timezone" to TimeZone.getDefault().id,
                        "status" to "scheduled",
                        "source" to "manual",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                ProductivityReminderNotificationCenter.schedule(app, ref.id, normalizedTitle, dueAt)
            }
            refresh()
        }
    }

    fun createTask(title: String, description: String, dueAt: Date? = null) {
        val uid = auth.currentUser?.uid ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val payload = buildMap {
                    put("title", normalizedTitle)
                    put("description", description.trim())
                    put("status", "open")
                    put("priority", "normal")
                    put("source", "manual")
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                    if (dueAt != null) {
                        put("dueAt", Timestamp(dueAt))
                    }
                }
                firestore.collection("users").document(uid).collection("tasks")
                    .add(payload)
                    .await()
            }
            refresh()
        }
    }

    fun createNote(title: String, content: String) {
        val uid = auth.currentUser?.uid ?: return
        val normalizedTitle = title.trim()
        val normalizedContent = content.trim()
        if (normalizedTitle.isBlank() && normalizedContent.isBlank()) return
        val safeTitle = normalizedTitle.ifBlank { "Untitled" }
        val expiresAt = Date(System.currentTimeMillis() + noteRetentionMillis)
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).collection("notes")
                    .add(
                        mapOf(
                            "title" to safeTitle,
                            "content" to normalizedContent,
                            "source" to "manual",
                            "expiresAt" to Timestamp(expiresAt),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
            }
            refresh()
        }
    }

    fun updateReminderTitle(reminderId: String, title: String) {
        val uid = auth.currentUser?.uid ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).collection("reminders")
                    .document(reminderId)
                    .set(
                        mapOf(
                            "title" to normalizedTitle,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    ).await()
            }
            refresh()
        }
    }

    fun deleteReminder(reminderId: String) {
        val uid = auth.currentUser?.uid ?: return
        val app = getApplication<Application>()
        viewModelScope.launch {
            runCatching {
                ProductivityReminderNotificationCenter.cancel(app, reminderId)
                firestore.collection("users").document(uid).collection("reminders")
                    .document(reminderId)
                    .delete()
                    .await()
            }
            refresh()
        }
    }

    fun updateTaskTitle(taskId: String, title: String) {
        val uid = auth.currentUser?.uid ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).collection("tasks")
                    .document(taskId)
                    .set(
                        mapOf(
                            "title" to normalizedTitle,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    ).await()
            }
            refresh()
        }
    }

    fun deleteTask(taskId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).collection("tasks")
                    .document(taskId)
                    .delete()
                    .await()
            }
            refresh()
        }
    }

    fun updateNoteTitle(noteId: String, title: String) {
        val uid = auth.currentUser?.uid ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).collection("notes")
                    .document(noteId)
                    .set(
                        mapOf(
                            "title" to normalizedTitle,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    ).await()
            }
            refresh()
        }
    }

    fun deleteNote(noteId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).collection("notes")
                    .document(noteId)
                    .delete()
                    .await()
            }
            refresh()
        }
    }

    private suspend fun loadProductivitySnapshot(generation: Long) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update {
                if (!isCurrentRefresh(generation)) return@update it
                it.copy(
                    dueTodayReminders = emptyList(),
                    upcomingReminders = emptyList(),
                    openTasks = emptyList(),
                    recentNotes = emptyList(),
                )
            }
            return
        }

        val now = Date()
        val dayStart = Calendar.getInstance().apply {
            time = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = dayStart + 24 * 60 * 60 * 1000

        val reminders = runCatching {
            firestore.collection("users").document(uid).collection("reminders").limit(80).get().await()
        }.getOrNull()?.documents.orEmpty().mapNotNull { document ->
            val title = listOf("title", "text", "message")
                .asSequence()
                .mapNotNull { key -> document.getString(key)?.trim()?.takeIf { it.isNotEmpty() } }
                .firstOrNull()
                ?: return@mapNotNull null
            val dueAt = resolveDate(
                document.get("dueAt"),
                document.get("scheduledAt"),
                document.get("scheduledFor"),
                document.get("remindAt"),
                document.get("triggerAt"),
                document.get("date"),
            )
            ProductivityReminderItem(document.id, title, dueAt)
        }.filter { it.dueAt == null || !it.dueAt.before(now) }
            .sortedBy { it.dueAt ?: Date(Long.MAX_VALUE) }

        val dueToday = reminders.filter { item ->
            val dueMs = item.dueAt?.time ?: return@filter false
            dueMs in dayStart until endOfDay
        }.take(5)

        val upcoming = reminders.filter { item ->
            val dueMs = item.dueAt?.time ?: return@filter false
            dueMs >= endOfDay
        }.take(5)

        val taskDocs = runCatching {
            firestore.collection("users").document(uid).collection("tasks").limit(40).get().await()
        }.getOrNull()?.documents.orEmpty()

        for (document in taskDocs) {
            val dueAt = resolveDate(document.get("dueAt"), document.get("scheduledAt"))
            if (dueAt != null && dueAt.before(now)) {
                runCatching {
                    firestore.collection("users").document(uid).collection("tasks").document(document.id)
                        .delete()
                        .await()
                }
            }
        }

        val tasks = taskDocs.mapNotNull { document ->
            val title = document.getString("title")?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val status = document.getString("status")?.trim()?.lowercase().orEmpty()
            if (status == "completed") return@mapNotNull null
            val dueAt = resolveDate(document.get("dueAt"), document.get("scheduledAt"))
            if (dueAt != null && dueAt.before(now)) return@mapNotNull null
            ProductivityTaskItem(
                id = document.id,
                title = title,
                dueAt = dueAt,
            )
        }.sortedWith(
            compareBy<ProductivityTaskItem>({ it.dueAt ?: Date(Long.MAX_VALUE) }, { it.title.lowercase() }),
        ).take(6)

        val noteDocs = runCatching {
            firestore.collection("users").document(uid).collection("notes").limit(40).get().await()
        }.getOrNull()?.documents.orEmpty()

        for (document in noteDocs) {
            val data = document.data ?: emptyMap()
            val expiresAt = resolveDate(data["expiresAt"])
            val createdAt = resolveDate(data["createdAt"])
            val effectiveCutoff = when {
                expiresAt != null -> expiresAt
                createdAt != null -> Date(createdAt.time + noteRetentionMillis)
                else -> null
            }
            if (effectiveCutoff != null && !effectiveCutoff.after(now)) {
                runCatching {
                    firestore.collection("users").document(uid).collection("notes").document(document.id)
                        .delete()
                        .await()
                }
            }
        }

        val notes = noteDocs.mapNotNull { document ->
            val data = document.data ?: return@mapNotNull null
            val title = document.getString("title")?.trim().orEmpty()
            val content = document.getString("content")?.trim().orEmpty()
            val display = if (title.isNotBlank()) title else content.take(48).trim()
            if (display.isBlank()) return@mapNotNull null
            val expiresAt = resolveDate(data["expiresAt"])
            val createdAt = resolveDate(data["createdAt"])
            val effectiveCutoff = when {
                expiresAt != null -> expiresAt
                createdAt != null -> Date(createdAt.time + noteRetentionMillis)
                else -> null
            }
            if (effectiveCutoff != null && !effectiveCutoff.after(now)) return@mapNotNull null
            ProductivityNoteItem(
                id = document.id,
                title = display,
                updatedAt = resolveDate(document.get("updatedAt"), document.get("createdAt")),
            )
        }.sortedByDescending { it.updatedAt ?: Date(0) }
            .take(6)

        _uiState.update {
            if (!isCurrentRefresh(generation)) return@update it
            it.copy(
                dueTodayReminders = dueToday,
                upcomingReminders = upcoming,
                openTasks = tasks,
                recentNotes = notes,
            )
        }
    }

    private suspend fun loadRuntimeSignals(generation: Long) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            applyRuntimeSignals(
                generation = generation,
                aiUsageWarning = null,
                creatorLimitZone = false,
                agentRunning = false,
                workflowWaiting = false,
                commerceSignal = null,
                recoverableError = null,
                newDataAvailable = false,
            )
            return
        }

        var aiUsageWarning: String? = null
        var creatorLimitZone = false
        var agentRunning = false
        var workflowWaiting = false
        var commerceSignal: String? = null
        var recoverableError: String? = null
        var newDataAvailable = false

        runCatching {
            val usageSnapshot = firestore.collection("users")
                .document(uid)
                .collection("aiUsage")
                .orderBy("createdAt")
                .limit(1)
                .get()
                .await()
            val usageData = usageSnapshot.documents.firstOrNull()?.data.orEmpty()
            val warningLevel = (usageData["warningLevel"] as? String).orEmpty().lowercase()
            if (warningLevel == "critical" || warningLevel == "warning") {
                aiUsageWarning = resString(R.string.home_signal_usage_level, warningLevel.uppercase())
            }
            val remaining = (usageData["remainingForKind"] as? Number)?.toInt() ?: -1
            val limit = (usageData["limitForKind"] as? Number)?.toInt() ?: -1
            creatorLimitZone = limit > 0 && remaining >= 0 && (limit - remaining).toDouble() / limit.toDouble() >= 0.8
        }

        runCatching {
            val runsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("agentRuns")
                .orderBy("createdAt")
                .limit(1)
                .get()
                .await()
            val runData = runsSnapshot.documents.firstOrNull()?.data.orEmpty()
            val runStatus = (runData["status"] as? String).orEmpty().lowercase()
            agentRunning = runStatus == "running" || runStatus == "processing"
            workflowWaiting = runStatus == "queued" || runStatus == "waiting"
        }

        runCatching {
            val userDoc = firestore.collection("users").document(uid).get().await().data.orEmpty()
            val role = (userDoc["role"] as? String).orEmpty().lowercase()
            if (role == "owner") {
                val orderSnapshot = firestore.collection("orders")
                    .orderBy("timestamp")
                    .limit(1)
                    .get()
                    .await()
                val orderData = orderSnapshot.documents.firstOrNull()?.data.orEmpty()
                val paymentStatus = (orderData["paymentStatus"] as? String).orEmpty()
                val fulfillmentStatus = (orderData["fulfillmentStatus"] as? String).orEmpty()
                commerceSignal = when {
                    paymentStatus.equals("pending", ignoreCase = true) -> resString(R.string.home_signal_open_payment_review)
                    fulfillmentStatus.isNotBlank() -> resString(R.string.home_signal_shipping_update, fulfillmentStatus)
                    orderData.isNotEmpty() -> resString(R.string.home_signal_new_order_activity)
                    else -> null
                }
            }
        }

        runCatching {
            val runtimeConfig = firestore.collection("system").document("runtimeConfig").get().await().data.orEmpty()
            val lockdown = runtimeConfig["lockdown"] as? Boolean ?: false
            val uploadsEnabled = runtimeConfig["uploadsEnabled"] as? Boolean ?: true
            val userWritesEnabled = runtimeConfig["userWritesEnabled"] as? Boolean ?: true
            val registrationsEnabled = runtimeConfig["registrationsEnabled"] as? Boolean ?: true
            val featurePausedCount = listOf(uploadsEnabled, userWritesEnabled, registrationsEnabled).count { !it }
            newDataAvailable = !lockdown && featurePausedCount == 0
            if (featurePausedCount > 0) {
                recoverableError = resString(R.string.home_signal_system_reduced_mode)
            }
        }

        applyRuntimeSignals(
            generation = generation,
            aiUsageWarning = aiUsageWarning,
            creatorLimitZone = creatorLimitZone,
            agentRunning = agentRunning,
            workflowWaiting = workflowWaiting,
            commerceSignal = commerceSignal,
            recoverableError = recoverableError,
            newDataAvailable = newDataAvailable,
        )
    }

    private fun isCurrentRefresh(generation: Long): Boolean = generation == refreshGeneration

    private fun applyRuntimeSignals(
        generation: Long,
        aiUsageWarning: String?,
        creatorLimitZone: Boolean,
        agentRunning: Boolean,
        workflowWaiting: Boolean,
        commerceSignal: String?,
        recoverableError: String?,
        newDataAvailable: Boolean,
    ) {
        _uiState.update { state ->
            if (!isCurrentRefresh(generation)) return@update state
            val updatedState = state.copy(
                aiUsageWarning = aiUsageWarning,
                creatorLimitZone = creatorLimitZone,
                agentRunning = agentRunning,
                workflowWaiting = workflowWaiting,
                commerceSignal = commerceSignal,
                syncPaused = !newDataAvailable,
                recoverableError = recoverableError,
                newDataAvailable = newDataAvailable,
            )
            updatedState.copy(contentSignal = buildContentSignal(updatedState))
        }
    }

    private fun applyFeaturedTrackUpdate(generation: Long, track: Track?) {
        _uiState.update {
            if (!isCurrentRefresh(generation)) return@update it
            val updatedState = it.copy(
                featuredTrack = track,
                homeTrackMessage = if (track == null) {
                    resString(R.string.home_track_placeholder)
                } else {
                    null
                },
            )
            updatedState.copy(contentSignal = buildContentSignal(updatedState))
        }
    }

    private fun applyFeaturedVideoUpdate(generation: Long, video: FeaturedVideoHighlight?) {
        _uiState.update {
            if (!isCurrentRefresh(generation)) return@update it
            val updatedState = it.copy(
                featuredVideo = video,
                homeVideoMessage = if (video == null) {
                    resString(R.string.home_video_placeholder)
                } else {
                    null
                },
            )
            updatedState.copy(contentSignal = buildContentSignal(updatedState))
        }
    }

    private fun buildContentSignal(state: HomeUiState): String? {
        val track = state.featuredTrack
        if (track != null) {
            return resString(R.string.home_content_signal_new_drop, track.trackName)
        }
        val video = state.featuredVideo
        if (video != null) {
            return resString(R.string.home_content_signal_video_activity, video.title)
        }
        return null
    }

    private suspend fun loadLatestTrack(): Track? {
        val tracks = supervisorScope {
            featuredArtists
                .map { artist ->
                    async {
                        musicService.fetchTracks(artist).getOrNull().orEmpty()
                    }
                }
                .awaitAll()
                .flatten()
        }

        if (tracks.isEmpty()) return null

        return tracks.sortedWith(::compareTracksForHomePriority).firstOrNull()
    }

    private fun compareTracksForHomePriority(lhs: Track, rhs: Track): Int {
        val lhsHasPreview = !lhs.previewUrl.isNullOrBlank()
        val rhsHasPreview = !rhs.previewUrl.isNullOrBlank()
        if (lhsHasPreview != rhsHasPreview) {
            return if (lhsHasPreview) -1 else 1
        }

        val lhsHasFallbackTarget = trackHasHomeFallbackTarget(lhs)
        val rhsHasFallbackTarget = trackHasHomeFallbackTarget(rhs)
        if (lhsHasFallbackTarget != rhsHasFallbackTarget) {
            return if (lhsHasFallbackTarget) -1 else 1
        }

        val lhsDate = parsedTrackReleaseDate(lhs.releaseDate)
        val rhsDate = parsedTrackReleaseDate(rhs.releaseDate)

        if (lhsDate != rhsDate) {
            return when {
                lhsDate == null -> 1
                rhsDate == null -> -1
                lhsDate > rhsDate -> -1
                else -> 1
            }
        }

        return lhs.trackName.lowercase().compareTo(rhs.trackName.lowercase())
    }

    private fun trackHasHomeFallbackTarget(track: Track): Boolean {
        return !track.spotifyTrackId.isNullOrBlank() ||
            !track.spotifyArtistId.isNullOrBlank() ||
            !track.externalUrl.isNullOrBlank()
    }

    private fun parsedTrackReleaseDate(value: String?): Long? {
        val rawValue = value?.trim().orEmpty()
        if (rawValue.isBlank()) return null

        return runCatching { java.time.Instant.parse(rawValue).toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(rawValue).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.LocalDate.parse(rawValue).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.YearMonth.parse(rawValue).atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.Year.parse(rawValue).atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    }

    private suspend fun loadLatestVideo(): FeaturedVideoHighlight? {
        val featuredSnapshot = firestore.collection("videographyHub")
            .whereEqualTo("isPublic", true)
            .whereEqualTo("isHomeFeatured", true)
            .limit(1)
            .get()
            .await()

        featuredSnapshot.documents.firstOrNull()
            ?.let(::mapFeaturedVideo)
            ?.let { return it }

        val snapshot = firestore.collection("videographyHub")
            .whereEqualTo("isPublic", true)
            .limit(12)
            .get()
            .await()

        val latestDocument = snapshot.documents
            .sortedByDescending(::documentTimestamp)
            .firstOrNull()
            ?: return null

        return mapFeaturedVideo(latestDocument)
    }

    private fun mapFeaturedVideo(document: com.google.firebase.firestore.DocumentSnapshot): FeaturedVideoHighlight? {
        val title = document.getString("title").orEmpty()
        if (title.isBlank()) return null

        return FeaturedVideoHighlight(
            id = document.id,
            title = title,
            projectName = document.getString("projectName").orEmpty().ifBlank { resString(R.string.home_video_project_fallback) },
            notes = document.getString("notes").orEmpty(),
            downloadUrl = document.getString("downloadURL").orEmpty(),
            externalUrl = document.getString("externalURL").orEmpty(),
            embedUrl = document.getString("embedURL").orEmpty(),
            sourceProvider = document.getString("sourceProvider")
                ?: ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
        )
    }

    private fun documentTimestamp(document: com.google.firebase.firestore.DocumentSnapshot): Long {
        return when (val createdAt = document.get("createdAt")) {
            is Timestamp -> createdAt.toDate().time
            is java.util.Date -> createdAt.time
            is Number -> createdAt.toLong()
            else -> 0L
        }
    }

    private fun resolveDate(vararg candidates: Any?): Date? {
        candidates.forEach { candidate ->
            when (candidate) {
                is Timestamp -> return candidate.toDate()
                is Date -> return candidate
                is Number -> {
                    val value = candidate.toLong()
                    return Date(if (value > 10_000_000_000L) value else value * 1000)
                }
            }
        }
        return null
    }
}
