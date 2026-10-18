package com.jabir.brot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jabir.brot.ReelCountApp
import com.jabir.brot.domain.DiagnosticsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiagnosticsViewModel : ViewModel() {
    private val app = ReelCountApp.instance
    private val diagnostics = app.trackingCoordinator.diagnostics

    private val _summary = MutableStateFlow(DiagnosticsSummary())
    val summary: StateFlow<DiagnosticsSummary> = _summary.asStateFlow()

    init {
        viewModelScope.launch {
            diagnostics.summary.collect {
                _summary.value = it
            }
        }
    }

    fun clearDiagnostics() {
        diagnostics.clear()
    }
}
