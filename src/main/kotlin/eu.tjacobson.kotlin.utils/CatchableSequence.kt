package eu.tjacobson.kotlin.utils

sealed class CatchableEntry<out T, R>(val orig: R?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CatchableEntry<*, *>

        if (orig != other.orig) return false

        return true
    }

    override fun hashCode(): Int {
        return orig?.hashCode() ?: 0
    }
}
class ValidEntry<T,R>(val entry: T?, orig: R?) : CatchableEntry<T,R>(orig) {
    fun addErr(newErr: Throwable) = ErrorEntry(newErr, orig)
    fun <U> addEntry(newEntry: U) = ValidEntry(newEntry, orig)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as ValidEntry<*, *>
        return entry != other.entry
    }
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (entry?.hashCode() ?: 0)
        return result
    }

}
class ErrorEntry<R>(val err: Throwable?, orig: R?) : CatchableEntry<Nothing, R>(orig) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as ErrorEntry<*>
        return (err != other.err)
    }
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (err?.hashCode() ?: 0)
        return result
    }
}

interface CatchableSequence<E, O> {
    operator fun iterator(): Iterator<CatchableEntry<E, O>>
}

inline fun <T,R> Iterator<T>.onNext(crossinline nextFunction: (T) -> R) : Iterator<R> = object : Iterator<R> {
    override fun hasNext() = this@onNext.hasNext()
    override fun next(): R = nextFunction(this@onNext.next())
}
class BaseCatchableSequence<O>(private val initSequence: Sequence<O>) : CatchableSequence<O, O> {
    override fun iterator(): Iterator<CatchableEntry<O, O>> = initSequence.iterator().onNext { ValidEntry(it, it) }
}
class TransformingCatchableSequence<N, E, O>(private val catchableSequence: CatchableSequence<E, O>, private val transformer: (E) -> N) : CatchableSequence<N, O> {
    override fun iterator(): Iterator<CatchableEntry<N, O>> = catchableSequence.iterator().onNext { next -> when(next) { is ErrorEntry -> next; is ValidEntry -> runCatching { next.addEntry((transformer(next.entry!!))) }.getOrElse { next.addErr(it) } } }
}
internal class FilteringCatchableSequence<E, O>(
    val catchableSequence: CatchableSequence<E, O>,
    private val sendWhen: Boolean = true,
    private val predicate: (CatchableEntry<E, O>) -> Boolean
) : CatchableSequence<E, O> {
    override fun iterator(): Iterator<CatchableEntry<E, O>> = object : Iterator<CatchableEntry<E, O>> {
        val iterator = catchableSequence.iterator()
        var nextState: Int = -1
        var nextItem: CatchableEntry<E, O>? = null

        private fun calcNext() {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (predicate(item) == sendWhen) {
                    nextItem = item
                    nextState = 1
                    return
                }
            }
            nextState = 0
        }

        override fun next(): CatchableEntry<E, O> {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem
            nextItem = null
            nextState = -1
            return result as CatchableEntry<E, O>
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}

fun <N, E, O> CatchableSequence<E, O>.map(transform: (E) -> N): CatchableSequence<N, O> = TransformingCatchableSequence(this, transform)
fun <T> Sequence<T>.catching(): CatchableSequence<T, T> = BaseCatchableSequence(this)
inline fun <E, O> CatchableSequence<E, O>.defaultFailures(crossinline default: () -> E?): Sequence<E?> = object : Sequence<E?> {
    override fun iterator(): Iterator<E?> = this@defaultFailures.iterator().onNext { when(it) { is ValidEntry -> it.entry; is ErrorEntry -> default() } }
}

inline fun <E, O> CatchableSequence<E, O>.onEachFailure(crossinline receiver: (O, Throwable) -> Unit): CatchableSequence<E, O> = object : CatchableSequence<E, O> {
    override fun iterator(): Iterator<CatchableEntry<E, O>> = this@onEachFailure.iterator().onNext {  it.also { if (it is ErrorEntry) receiver(it.orig!!, it.err!!) } }
}

fun <E, O> CatchableSequence<E, O>.toSequence(): Sequence<E?> = Sequence { this.iterator().onNext { when (it) { is ValidEntry -> it.entry; else -> null } } }
fun <E, O> CatchableSequence<E, O>.dropFailures(): Sequence<E?> = (FilteringCatchableSequence(this, false) { it is ErrorEntry }).toSequence().filter { it != null }
