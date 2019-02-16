package com.danielceinos.ratatosk.tasks

/**
 * Tasks represents a async operation use case that has 4 terminal states:
 *  - Idle: Neutral state
 *  - Running: The task is on going
 *  - Success: The task finishes correctly and holds data.
 *  - Error: The task has failed and contains the exception related to.
 */
@Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")
sealed class EmptyDataTask {

  fun isRunning() = this is Running

  fun isFailure() = this is Error

  fun isTerminal(): Boolean = this is Success || this is Error

  fun isSuccessful() = this is Success

  fun isIdle() = this is Idle

  class Idle : EmptyDataTask() {
    companion object {
      fun emptyDataTaskIdle(): EmptyDataTask = Idle()
    }
  }

  class Running : EmptyDataTask() {
    companion object {
      fun emptyDataTaskRunning(value: Int = -1): EmptyDataTask = Running()
    }
  }

  class Success : EmptyDataTask() {
    companion object {
      fun emptyDataTaskSuccess(): EmptyDataTask = Success()
    }
  }

  data class Error(val throwable: Throwable) : EmptyDataTask() {
    companion object {
      fun emptyDataTaskError(throwable: Throwable): EmptyDataTask = Error(throwable)
    }
  }

  override fun toString(): String {
    return when (this) {
      is Success -> "Success[]"
      is Error -> "Error[exception=$throwable]"
      is Idle -> "Idle[]"
      is Running -> "Running[]"
    }
  }
}

//Utilities for  data task collections

/** Find the first failed task or throw an exception. */
fun Iterable<EmptyDataTask>.firstFailure(): EmptyDataTask.Error {
  return this.first { it is EmptyDataTask.Error } as EmptyDataTask.Error
}

/** Find the first failed task or null. */
fun Iterable<EmptyDataTask>.firstFailureOrNull(): EmptyDataTask.Error? {
  return this.firstOrNull { it is EmptyDataTask.Error }
      ?.let { it as EmptyDataTask.Error }
}

/** All task are in terminal state. */
fun Iterable<EmptyDataTask>.allCompleted(): Boolean {
  return this.all { it.isTerminal() }
}

/** All tasks succeeded. */
fun Iterable<EmptyDataTask>.allSuccessful(): Boolean {
  return this.all { it.isSuccessful() }
}

/** Any tasks failed. */
fun Iterable<EmptyDataTask>.anyFailure(): Boolean {
  return this.any { it.isFailure() }
}

/** Any task is running. */
fun Iterable<EmptyDataTask>.anyRunning(): Boolean {
  return this.any { it.isRunning() }
}