package com.danielceinos.ratatosk.extensions

import io.reactivex.Flowable

/**
 * Apply the mapping function if object is not null and if is null return the default value.
 */
inline fun <T, U> Flowable<T>.mapNull(crossinline fn: (T) -> U?, default: U): Flowable<U> {
    return flatMap {
        val mapped = fn(it)
        if (mapped == null) Flowable.just(default)
        else Flowable.just(mapped)
    }
}

/**
 * Apply the mapping function if object is not null or return default value if null, together with a distinctUntilChanged call.
 */
inline fun <T, U> Flowable<T>.select(crossinline fn: (T) -> U?, default: U): Flowable<U> =
        mapNull(fn, default).distinctUntilChanged()
