package com.danielceinos.ratatosk.tasks

/**
 * Tasks represents a async operation use case that has 4 terminal states:
 *  - Idle: Neutral state
 *  - Running: The task is on going
 *  - Success: The task finishes correctly and holds data.
 *  - Error: The task has failed and contains the exception related to.
 */
@Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")
sealed class DataTask<out T> {

    fun isRunning() = this is Running

    fun isFailure() = this is Error

    fun isTerminal(): Boolean = this is Success || this is Error

    fun isSuccessful() = this is Success

    fun isIdle() = this is Idle

    data class Idle<out T>(val data: T) : DataTask<T>() {
        companion object {
            fun <T> dataTaskIdle(data: T): DataTask<T> = Idle(data)
        }
    }

    data class Running<out T>(val value: Int) : DataTask<T>() {
        companion object {
            fun <T> dataTaskRunning(value: Int = -1): DataTask<T> = Running(value)
        }
    }

    data class Success<out T>(val data: T) : DataTask<T>() {
        companion object {
            fun <T> dataTaskSuccess(data: T): DataTask<T> = Success(data)
        }
    }

    data class Error(val throwable: Throwable) : DataTask<Nothing>() {
        companion object {
            fun <T> dataTaskError(throwable: Throwable): DataTask<T> = Error(throwable)
        }
    }

    override fun toString(): String {
        return when (this) {
            is Success -> "Success[data=$data]"
            is Error -> "Error[exception=$throwable]"
            is Idle -> "Idle[value=$data]"
            is Running -> "Running[value=$value]"
        }
    }
}

//Utilities for  data task collections

/** Find the first failed task or throw an exception. */
fun <T> Iterable<DataTask<T>>.firstFailure(): DataTask.Error {
    return this.first { it is DataTask.Error } as DataTask.Error
}

/** Find the first failed task or null. */
fun <T> Iterable<DataTask<T>>.firstFailureOrNull(): DataTask.Error? {
    return this.firstOrNull { it is DataTask.Error }?.let { it as DataTask.Error }
}

/** All task are in terminal state. */
fun <T> Iterable<DataTask<T>>.allCompleted(): Boolean {
    return this.all { it.isTerminal() }
}

/** All tasks succeeded. */
fun <T> Iterable<DataTask<T>>.allSuccessful(): Boolean {
    return this.all { it.isSuccessful() }
}

/** Any tasks failed. */
fun <T> Iterable<DataTask<T>>.anyFailure(): Boolean {
    return this.any { it.isFailure() }
}

/** Any task is running. */
fun <T> Iterable<DataTask<T>>.anyRunning(): Boolean {
    return this.any { it.isRunning() }
}