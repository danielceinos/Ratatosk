package com.danielceinos.ratatosk.extensions

/**
 * Created by Daniel S on 11/11/2018.
 */


fun <K, V> Map<K, V>.replace(key: K, value: V?): Map<K, V> {
    if (value == null) return this
    val mutable = this.toMutableMap()
    mutable[key] = value
    return mutable
}
