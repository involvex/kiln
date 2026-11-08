package dev.involvex.kiln.core.common

/**
 * Cross-module result type. Prefer this over throwing across module boundaries.
 */
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.value

    fun exceptionOrNull(): Throwable? = (this as? Failure)?.cause

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    companion object {
        fun <T> success(value: T): Result<T> = Success(value)

        fun failure(message: String, cause: Throwable? = null): Result<Nothing> = Failure(message, cause)
    }
}
