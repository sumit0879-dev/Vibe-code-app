package com.vibecode.ide.data.remote

sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Done(val finishReason: String?) : StreamEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : StreamEvent()
}
