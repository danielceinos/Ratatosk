package com.danielceinos.ratatosk.extensions

import io.reactivex.disposables.Disposable
import mini.Store
import mini.select

/**
 * Execute code block for each task state
 *
 * @param dataTask select data task from current store's state
 * @param onRunning code block executed on task running
 * @param onSuccess code block executed on task's success
 * @param onError code block executed on task's failure
 *
 * Returns a [Disposable]
 */
inline fun <S : Any, K : Any> Store<S>.renderTask(
    crossinline dataTask: (S) -> DataTask<K>?,
    crossinline onIdle: () -> Unit,
    crossinline onProgress: () -> Unit,
    crossinline onSuccess: (K) -> Unit,
    crossinline onError: (Throwable) -> Unit
): Disposable {

    return flowable()
        .select(dataTask, dataTaskError(Error("task not found")))
        .subscribe {
            when (it) {
                is DataTask.Idle -> onIdle()
                is DataTask.Running -> onProgress()
                is DataTask.Success -> onSuccess(it.data)
                is DataTask.Error -> onError(it.throwable)
            }
        }
}

/**
 * Execute code block for each task state
 *
 * @param dataTasks select data task iterable from current store's state
 * @param onRunning code block executed when any task is running
 * @param onSuccess code block executed when all tasks are success
 * @param onError code block executed when any task is failure
 *
 * Returns a [Disposable]
 */
inline fun <S : Any, K : Any> Store<S>.renderTaskList(
    crossinline dataTasks: (S) -> Iterable<DataTask<K>>,
    crossinline onProgress: () -> Unit,
    crossinline onSuccess: (Iterable<DataTask<K>>) -> Unit,
    crossinline onError: (Throwable) -> Unit
): Disposable {
    return flowable()
        .select(dataTasks)
        .subscribe {
            when {
                it.allSuccessful() -> onSuccess(it)
                it.anyFailure() -> onError(it.firstFailure().throwable)
                it.anyRunning() -> onProgress()
            }
        }
}

/**
 * Execute code block for each task state
 *
 * @param dataTask select data task from current store's state
 * @param onRunning code block executed on task running
 * @param onSuccess code block executed on task's success
 * @param onError code block executed on task's failure
 *
 * Returns a [Disposable]
 */
inline fun <S : Any> Store<S>.renderEmptyTask(
    crossinline dataTask: (S) -> EmptyDataTask?,
    crossinline onIdle: () -> Unit,
    crossinline onProgress: () -> Unit,
    crossinline onSuccess: () -> Unit,
    crossinline onError: (Throwable) -> Unit
): Disposable {
    return flowable()
        .select(dataTask)
        .subscribe {
            when (it) {
                is EmptyDataTask.Idle -> onIdle()
                is EmptyDataTask.Running -> onProgress()
                is EmptyDataTask.Success -> onSuccess()
                is EmptyDataTask.Error -> onError(it.throwable)
            }
        }
}

/**
 * Execute code block for each task state
 *
 * @param dataTasks select data task iterable from current store's state
 * @param onRunning code block executed when any task is running
 * @param onSuccess code block executed when all tasks are success
 * @param onError code block executed when any task is failure
 *
 * Returns a [Disposable]
 */
inline fun <S : Any> Store<S>.renderEmptyTaskList(
    crossinline dataTasks: (S) -> Iterable<EmptyDataTask>,
    crossinline onProgress: () -> Unit,
    crossinline onSuccess: (Iterable<EmptyDataTask>) -> Unit,
    crossinline onError: (Throwable) -> Unit
): Disposable {
    return flowable()
        .select(dataTasks)
        .subscribe {
            when {
                it.allSuccessful() -> onSuccess(it)
                it.anyFailure() -> onError(it.firstFailure().throwable)
                it.anyRunning() -> onProgress()
            }
        }
}