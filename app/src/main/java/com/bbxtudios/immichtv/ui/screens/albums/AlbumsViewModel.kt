package com.bbxtudios.immichtv.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbxtudios.immichtv.data.model.AlbumDetail
import com.bbxtudios.immichtv.data.model.AlbumItem
import com.bbxtudios.immichtv.data.repository.ImmichRepository
import com.bbxtudios.immichtv.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AlbumsUiState {
    data class Loading(val albumName: String? = null) : AlbumsUiState()
    data class AlbumList(val albums: List<AlbumItem>) : AlbumsUiState()
    data class InsideAlbum(val album: AlbumDetail) : AlbumsUiState()
    data class Error(val message: String) : AlbumsUiState()
}

class AlbumsViewModel(
    private val immichRepository: ImmichRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Loading())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private var currentAlbumId: String? = null
    private var lastExitedAlbumId: String? = null
    private var loadingJob: Job? = null

    val gridColumns = settingsRepository.gridColumns

    init {
        loadAlbums()

        viewModelScope.launch {
            combine(
                settingsRepository.groupOrder,
                settingsRepository.assetOrder,
                settingsRepository.serverUrl,
                settingsRepository.apiKey
            ) { groupOrder, assetOrder, serverUrl, apiKey ->
                listOf(groupOrder, assetOrder, serverUrl, apiKey)
            }.drop(1).collect {
                val albumId = currentAlbumId
                if (albumId != null) {
                    openAlbum(albumId)
                } else {
                    loadAlbums(isRefresh = true)
                }
            }
        }
    }

    fun loadAlbums(isRefresh: Boolean = false) {
        loadingJob?.cancel()
        currentAlbumId = null

        if (isRefresh || _uiState.value !is AlbumsUiState.AlbumList) {
            _uiState.value = AlbumsUiState.Loading()
        }

        loadingJob = viewModelScope.launch {
            try {
                val albums = immichRepository.getAlbums()
                _uiState.value = AlbumsUiState.AlbumList(albums)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (_uiState.value !is AlbumsUiState.AlbumList) {
                    _uiState.value = AlbumsUiState.Error(
                        e.message ?: "No se pudieron cargar los álbumes. Comprueba la conexión con el servidor."
                    )
                }
            }
        }
    }

    fun openAlbum(albumId: String) {
        loadingJob?.cancel()
        currentAlbumId = albumId

        _uiState.value = AlbumsUiState.Loading()

        loadingJob = viewModelScope.launch {
            try {
                val groupOrder = settingsRepository.groupOrder.first()
                val assetOrder = settingsRepository.assetOrder.first()
                val detail = immichRepository.getAlbumDetail(albumId, groupOrder, assetOrder)
                _uiState.value = AlbumsUiState.InsideAlbum(detail)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (_uiState.value !is AlbumsUiState.InsideAlbum) {
                    _uiState.value = AlbumsUiState.Error(
                        e.message ?: "No se pudo cargar el contenido del álbum."
                    )
                }
            }
        }
    }

    fun navigateUp(): Boolean {
        if (currentAlbumId != null) {
            lastExitedAlbumId = currentAlbumId
            loadAlbums(isRefresh = true)
            return true
        }
        return false
    }

    fun getLastExitedAlbumId(): String? = lastExitedAlbumId

    fun clearLastExitedAlbumId() {
        lastExitedAlbumId = null
    }

    fun reload() {
        val albumId = currentAlbumId
        if (albumId != null) {
            openAlbum(albumId)
        } else {
            loadAlbums(isRefresh = true)
        }
    }

    class Factory(
        private val immichRepository: ImmichRepository
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlbumsViewModel(
                immichRepository,
                com.bbxtudios.immichtv.AppContainer.settingsRepository
            ) as T
        }
    }
}
