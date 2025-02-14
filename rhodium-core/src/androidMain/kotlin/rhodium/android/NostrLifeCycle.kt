package rhodium.android

import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.State

class NostrLifeCycle: LifecycleOwner {
    private var lifecycleState = State.INITIALIZED
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle = lifecycleRegistry

    fun currentState(): Lifecycle.State {
        return lifecycleState
    }


    fun changeState(newState: Lifecycle.State){
        lifecycleState = newState
    }


    inner class NostrEventObserver() : DefaultLifecycleObserver, LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val state = source.lifecycle.currentState
            this@NostrLifeCycle.changeState(state)
            this@NostrLifeCycle.lifecycleRegistry.handleLifecycleEvent(event)
        }

        override fun onCreate(owner: LifecycleOwner) {
            this@NostrLifeCycle.lifecycleRegistry.addObserver(this)
            super.onCreate(owner)
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
        }

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            this@NostrLifeCycle.lifecycleRegistry.removeObserver(this)
            super.onDestroy(owner)
        }
    }

}