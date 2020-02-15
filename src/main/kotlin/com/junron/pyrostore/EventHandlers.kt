package com.junron.pyrostore


private val onConnectCallbacks = mutableListOf<() -> Unit>()
fun PyroStore.onConnect(callback: () -> Unit) {
    onConnectCallbacks += callback
}

internal fun onConnect() {
    onConnectCallbacks.forEach { it() }
}

private val onProjectConnectCallbacks = mutableListOf<() -> Unit>()
fun PyroStore.onProjectConnect(callback: () -> Unit) {
    onProjectConnectCallbacks += callback
}

internal fun onProjectConnect() {
    onProjectConnectCallbacks.forEach { it() }
}

private val onDisconnectCallbacks = mutableListOf<() -> Unit>()
fun PyroStore.onDisconnect(callback: () -> Unit) {
    onDisconnectCallbacks += callback
}


internal fun onDisconnect() {
    onDisconnectCallbacks.forEach { it() }
}

