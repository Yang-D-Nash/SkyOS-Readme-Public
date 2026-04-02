package com.skydown.android.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.ProfileGalleryItem
import com.skydown.android.ui.model.ProfileMediaType
import com.skydown.shared.model.ProfileUpdateInput
import com.skydown.shared.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val currentUser: User? = null,
    val username: String = "",
    val whatsApp: String = "",
    val profileTagline: String = "",
    val profileBio: String = "",
    val instagramHandle: String = "",
    val selectedMediaType: ProfileMediaType = ProfileMediaType.Image,
    val galleryItems: List<ProfileGalleryItem> = emptyList(),
    val isEditing: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null,
) {
    val filteredItems: List<ProfileGalleryItem>
        get() = galleryItems.filter { it.type == selectedMediaType }

    val imageCount: Int
        get() = galleryItems.count { it.type == ProfileMediaType.Image }

    val canEditCurrentProfile: Boolean
        get() = currentUser?.id != null
}

class ProfileViewModel : ViewModel() {
    private val authService = AppContainer.authService
    private val repository = AppContainer.userProfileRepository
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var galleryListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            runCatching { AppContainer.refreshCurrentUser() }
        }

        viewModelScope.launch {
            AppContainer.currentUser.collectLatest { user ->
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        username = user?.username.orEmpty(),
                        whatsApp = user?.whatsApp.orEmpty(),
                        profileTagline = user?.profileTagline.orEmpty(),
                        profileBio = user?.profileBio.orEmpty(),
                        instagramHandle = user?.instagramHandle.orEmpty(),
                        errorMessage = null,
                    )
                }
                observeGallery(user?.id)
            }
        }
    }

    fun setEditing(enabled: Boolean) {
        _uiState.update { it.copy(isEditing = enabled, errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updateWhatsApp(value: String) {
        _uiState.update { it.copy(whatsApp = value) }
    }

    fun updateProfileTagline(value: String) {
        _uiState.update { it.copy(profileTagline = value) }
    }

    fun updateProfileBio(value: String) {
        _uiState.update { it.copy(profileBio = value) }
    }

    fun updateInstagramHandle(value: String) {
        _uiState.update { it.copy(instagramHandle = value) }
    }

    fun selectMediaType(type: ProfileMediaType) {
        _uiState.update { it.copy(selectedMediaType = type) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val current = _uiState.value
            val trimmedUsername = current.username.trim()
            if (trimmedUsername.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Bitte gib einen Benutzernamen ein.") }
                return@launch
            }

            _uiState.update { it.copy(isSavingProfile = true, errorMessage = null) }
            val result = authService.updateCurrentProfile(
                ProfileUpdateInput(
                    username = trimmedUsername,
                    whatsApp = current.whatsApp,
                    profileTagline = current.profileTagline,
                    profileBio = current.profileBio,
                    instagramHandle = current.instagramHandle,
                ),
            )

            if (result.isSuccess) {
                AppContainer.refreshCurrentUser()
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        isEditing = false,
                        toastMessage = "Profil gespeichert.",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Profil konnte nicht gespeichert werden.",
                    )
                }
            }
        }
    }

    fun uploadAvatar(uri: Uri, mimeType: String?) {
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, errorMessage = null) }
            val result = repository.uploadAvatar(userId = userId, uri = uri, mimeType = mimeType)
            if (result.isSuccess) {
                AppContainer.refreshCurrentUser()
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        toastMessage = "Profilbild aktualisiert.",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Profilbild konnte nicht geladen werden.",
                    )
                }
            }
        }
    }

    fun uploadMedia(type: ProfileMediaType, uri: Uri, mimeType: String?) {
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingMedia = true, errorMessage = null) }
            val result = repository.uploadGallery(
                userId = userId,
                uri = uri,
                type = type,
                mimeType = mimeType,
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isUploadingMedia = false,
                        selectedMediaType = type,
                        toastMessage = "Bild hochgeladen.",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isUploadingMedia = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Upload fehlgeschlagen.",
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        galleryListener?.remove()
    }

    private fun observeGallery(userId: String?) {
        galleryListener?.remove()
        _uiState.update { it.copy(galleryItems = emptyList()) }

        if (userId.isNullOrBlank()) return

        galleryListener = repository.observeGallery(userId) { result ->
            result.onSuccess { items ->
                _uiState.update { it.copy(galleryItems = items) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Profilgalerie konnte nicht geladen werden.")
                }
            }
        }
    }
}
