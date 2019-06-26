package com.aldaviva.midi

internal class SimpleEventEmitter<T> {

    private val listeners = mutableListOf<EventListener<T>>()
    private val oneTimeListeners = mutableListOf<EventListener<T>>()

    fun register(listener: EventListener<T>) {
        listeners.add(listener)
    }

    fun registerForOneEvent(listener: EventListener<T>) {
        synchronized(oneTimeListeners) {
            oneTimeListeners.add(listener)
        }
    }

    fun trigger(event: T) {
        listeners.forEach { eventListener -> eventListener.onEvent(event) }

        val oneTimeListenersSnapshot: List<EventListener<T>>
        synchronized(oneTimeListeners) {
            oneTimeListenersSnapshot = oneTimeListeners.toList()
            oneTimeListeners.clear()
        }
        oneTimeListenersSnapshot.forEach { eventListener -> eventListener.onEvent(event) }
    }

    fun unregister(listener: EventListener<T>) {
        listeners.remove(listener)
    }

    fun unregisterAll() {
        listeners.clear()
    }

}

abstract class EventListener<U> {
    abstract fun onEvent(event: U)
}