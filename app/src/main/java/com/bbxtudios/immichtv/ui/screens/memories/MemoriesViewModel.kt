package com.bbxtudios.immichtv.ui.screens.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bbxtudios.immichtv.data.model.MemoryGroup
import com.bbxtudios.immichtv.data.model.ViewAsset
import androidx.compose.runtime.Immutable
import com.bbxtudios.immichtv.data.repository.ImmichRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
sealed interface MemoriesUiState {
    @Immutable
    data object Loading : MemoriesUiState
    @Immutable
    data class Success(
        val groups: List<MemoryGroup>,
        val flatAssets: List<ViewAsset>
    ) : MemoriesUiState
    @Immutable
    data class Error(val message: String) : MemoriesUiState
}

class MemoriesViewModel(
    private val immichRepository: ImmichRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemoriesUiState>(MemoriesUiState.Loading)
    val uiState: StateFlow<MemoriesUiState> = _uiState.asStateFlow()

    init {
        loadMemories()
    }

    fun loadMemories() {
        viewModelScope.launch {
            _uiState.value = MemoriesUiState.Loading
            try {
                val groups = immichRepository.getMemories()
                val flatAssets = groups.flatMap { it.assets }
                _uiState.value = MemoriesUiState.Success(groups, flatAssets)
            } catch (e: Exception) {
                _uiState.value = MemoriesUiState.Error(
                    e.message ?: "No se pudieron obtener los recuerdos de hoy."
                )
            }
        }
    }

    class Factory(
        private val immichRepository: ImmichRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MemoriesViewModel(immichRepository) as T
        }
    }
}
