package com.danielceinos.ratatosk.stores

import mini.Action
import mini.Reducer
import mini.Store

/**
 * Created by Daniel S on 16/02/2019.
 */

data class AdvertisingAction(val advertising: Boolean) : Action

data class DiscoveringAction(val discovering: Boolean) : Action

data class RatatoskState(
    val advertising: Boolean,
    val discovering: Boolean
)

class RatatoskStore : Store<RatatoskState>() {

    @Reducer
    fun onAdvertising(action: AdvertisingAction): RatatoskState {
        return state.copy(advertising = action.advertising)
    }

    @Reducer
    fun onDiscovering(action: DiscoveringAction): RatatoskState {
        return state.copy(discovering = action.discovering)
    }
}