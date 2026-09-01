package com.bbxtudios.immichtv.ui.screens.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.bbxtudios.immichtv.data.model.ViewAsset
import com.bbxtudios.immichtv.data.repository.ImmichRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
sealed interface RandomUiState {
    @Immutable
    data object Loading : RandomUiState
    @Immutable
    data class Success(val assets: List<ViewAsset>) : RandomUiState
    @Immutable
    data class Error(val message: String) : RandomUiState
}

class RandomViewModel(
    private val immichRepository: ImmichRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RandomUiState>(RandomUiState.Loading)
    val uiState: StateFlow<RandomUiState> = _uiState.asStateFlow()

    init {
        loadRandomAssets()
    }

    fun loadRandomAssets() {
        viewModelScope.launch {
            _uiState.value = RandomUiState.Loading
            try {
                val assets = immichRepository.getRandomAssets(200)
                _uiState.value = RandomUiState.Success(assets)
            } catch (e: Exception) {
                _uiState.value = RandomUiState.Error(
                    e.message ?: "No se pudieron obtener fotos aleatorias."
                )
            }
        }
    }

    class Factory(
        private val immichRepository: ImmichRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RandomViewModel(immichRepository) as T
        }
    }
}


