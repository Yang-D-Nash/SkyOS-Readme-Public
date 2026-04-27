package com.nash.skyos.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.model.ProfileGalleryItem
import com.nash.skyos.ui.model.ProfileMediaType
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
    val galleryItems: List<ProfileGalleryItem> = emptyList(),
    val isEditing: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null,
) {
    val filteredItems: List<ProfileGalleryItem>
        get() = galleryItems

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
            AppContainer.currentUser.collectLatest { user ->
                _uiState.update { state ->
                    val shouldRefreshDrafts = !state.isEditing || state.currentUser?.id != user?.id
                    state.copy(
                        currentUser = user,
                        username = if (shouldRefreshDrafts) user?.username.orEmpty() else state.username,
                        whatsApp = if (shouldRefreshDrafts) user?.whatsApp.orEmpty() else state.whatsApp,
                        profileTagline = if (shouldRefreshDrafts) user?.profileTagline.orEmpty() else state.profileTagline,
                        profileBio = if (shouldRefreshDrafts) user?.profileBio.orEmpty() else state.profileBio,
                        instagramHandle = if (shouldRefreshDrafts) user?.instagramHandle.orEmpty() else state.instagramHandle,
                        isEditing = state.isEditing && user != null,
                        errorMessage = null,
                    )
                }
                observeGallery(user?.id)
            }
        }
    }

    fun setEditing(enabled: Boolean) {
        _uiState.update { state ->
            if (enabled && state.canEditCurrentProfile) {
                state.copy(
                    isEditing = true,
                    errorMessage = null,
                )
            } else {
                state.copy(
                    isEditing = false,
                    username = state.currentUser?.username.orEmpty(),
                    whatsApp = state.currentUser?.whatsApp.orEmpty(),
                    profileTagline = state.currentUser?.profileTagline.orEmpty(),
                    profileBio = state.currentUser?.profileBio.orEmpty(),
                    instagramHandle = state.currentUser?.instagramHandle.orEmpty(),
                    errorMessage = null,
                )
            }
        }
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

    fun saveProfile() {
        viewModelScope.launch {
            val current = _uiState.value
            val trimmedUsername = current.username.trim()
            if (trimmedUsername.isEmpty()) {
                postError("Bitte gib einen Benutzernamen ein.")
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
                _uiState.update { it.copy(isSavingProfile = false, isEditing = false) }
                postSuccess("Profil gespeichert.")
            } else {
                failProfileSave(result.exceptionOrNull()?.message ?: "Profil konnte nicht gespeichert werden.")
            }
        }
    }

    fun uploadAvatar(uri: Uri, contentResolver: ContentResolver) {
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            setAvatarUploadLoading()
            val result = repository.uploadAvatar(
                userId = userId,
                uri = uri,
                contentResolver = contentResolver,
            )
            if (result.isSuccess) {
                AppContainer.refreshCurrentUser()
                finalizeAvatarUpload()
                postSuccess("Profilbild aktualisiert.")
            } else {
                failAvatarUpload(result.exceptionOrNull()?.message ?: "Profilbild konnte nicht geladen werden.")
            }
        }
    }

    fun uploadMedia(type: ProfileMediaType, uri: Uri, contentResolver: ContentResolver) {
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            setMediaUploadLoading()
            val result = repository.uploadGallery(
                userId = userId,
                uri = uri,
                contentResolver = contentResolver,
                type = type,
            )

            if (result.isSuccess) {
                finalizeMediaUpload()
                postSuccess("Bild hochgeladen.")
            } else {
                failMediaUpload(result.exceptionOrNull()?.message ?: "Upload fehlgeschlagen.")
            }
        }
    }

    fun deleteAvatar() {
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            setAvatarUploadLoading()
            val result = repository.deleteAvatar(userId)
            if (result.isSuccess) {
                AppContainer.refreshCurrentUser()
                finalizeAvatarUpload()
                postSuccess("Profilbild entfernt.")
            } else {
                failAvatarUpload(result.exceptionOrNull()?.message ?: "Profilbild konnte nicht entfernt werden.")
            }
        }
    }

    fun deleteGalleryItem(item: ProfileGalleryItem) {
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            setMediaUploadLoading()
            val result = repository.deleteGalleryItem(userId, item)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isUploadingMedia = false,
                        galleryItems = it.galleryItems.filterNot { galleryItem -> galleryItem.id == item.id },
                    )
                }
                postSuccess("Bild entfernt.")
            } else {
                failMediaUpload(result.exceptionOrNull()?.message ?: "Bild konnte nicht entfernt werden.")
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
                postError(error.message ?: "Profilgalerie konnte nicht geladen werden.")
            }
        }
    }

    private fun postSuccess(message: String) {
        _uiState.update { it.copy(toastMessage = message, errorMessage = null) }
    }

    private fun postError(message: String) {
        _uiState.update { it.copy(errorMessage = message, toastMessage = null) }
    }

    private fun failProfileSave(message: String) {
        _uiState.update {
            it.copy(
                isSavingProfile = false,
                errorMessage = message,
            )
        }
    }

    private fun setAvatarUploadLoading() {
        _uiState.update { it.copy(isUploadingAvatar = true, errorMessage = null) }
    }

    private fun finalizeAvatarUpload() {
        _uiState.update { it.copy(isUploadingAvatar = false) }
    }

    private fun failAvatarUpload(message: String) {
        _uiState.update {
            it.copy(
                isUploadingAvatar = false,
                errorMessage = message,
            )
        }
    }

    private fun setMediaUploadLoading() {
        _uiState.update { it.copy(isUploadingMedia = true, errorMessage = null) }
    }

    private fun finalizeMediaUpload() {
        _uiState.update { it.copy(isUploadingMedia = false) }
    }

    private fun failMediaUpload(message: String) {
        _uiState.update {
            it.copy(
                isUploadingMedia = false,
                errorMessage = message,
            )
        }
    }
}
