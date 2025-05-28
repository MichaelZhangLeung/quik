package dev.alejandrorosas.streamlib

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object StreamEventBus {
    private val _events = MutableSharedFlow<StreamEvent>()
    val events: SharedFlow<StreamEvent> = _events.asSharedFlow()

    suspend fun emitEvent(event: StreamEvent) {
        _events.emit(event)
    }

    sealed class StreamEvent {
        data class ConnectionChanged(val isConnected: Boolean) : StreamEvent()
        data class StreamingResult(val success: Boolean, val errorCode: Int?) : StreamEvent()
    }
}
