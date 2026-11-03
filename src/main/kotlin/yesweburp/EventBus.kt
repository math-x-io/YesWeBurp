package yesweburp


class EventBus<T> {
    private val callbacks = mutableListOf<(T) -> Unit>()

    fun listen(cb: (T) -> Unit) {
        callbacks.add(cb)
    }

    fun fire(data: T) {
        callbacks.forEach { it(data) }
    }
}
