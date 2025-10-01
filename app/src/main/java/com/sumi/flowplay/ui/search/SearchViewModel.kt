package com.sumi.flowplay.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sumi.flowplay.data.model.TrackDto
import com.sumi.flowplay.data.datastore.SearchPreferencesDataStore
import com.sumi.flowplay.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewViewModel @Inject constructor(
    private val repository: TrackRepository,
    private val searchDataStore: SearchPreferencesDataStore
) : ViewModel() {

    private val _query = MutableStateFlow("") // 현재 검색어
    val query: StateFlow<String> = _query.asStateFlow()

    val tracks: Flow<PagingData<TrackDto>> = _query.flatMapLatest { query ->
        repository.searchTracks(query)
    }.cachedIn(viewModelScope)

//    val tracks: StateFlow<PagingData<TrackDto>> = _query
//        .flatMapLatest { q ->
//            repository.searchTracks(q)
//        }
//        .cachedIn(viewModelScope)
//        .stateIn(viewModelScope, SharingStarted.Lazily, PagingData.empty())

    val recentSearches: StateFlow<List<String>> =
        searchDataStore.recentSearches.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    fun onTextChanged(newText: String) {
        _query.value = newText  // TextField 상태 업데이트
    }

    fun search() {
        if (_query.value.isNotEmpty()) {
            viewModelScope.launch {
                searchDataStore.addSearch(_query.value)
            }
        }
    }

    fun clearQuery() {
        _query.value = ""
    }
}
