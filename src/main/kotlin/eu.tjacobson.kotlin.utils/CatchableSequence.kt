package eu.tjacobson.kotlin.utils

data class CatchableEntry<T,R>(val entry: T?, val orig: R?, val err: Throwable? = null) {
    val hasErr = err != null
    fun <U> addEntry(newEntry: U) = CatchableEntry<U,R>(newEntry, orig, err)
    fun <U> addErr(newErr: Throwable) = CatchableEntry<U,R>(null, orig, newErr)
    fun <U> noChange() = CatchableEntry<U,R>(null,orig,err)
}

interface CatchableSequence<E,O> {
    fun iterator() : Iterator<CatchableEntry<E, O>>
}
class BaseCatchableSequence<O>(val initSequence: Sequence<O>) : CatchableSequence<O, O> {
    override fun iterator() = object : Iterator<CatchableEntry<O, O>> {
        val innerIterator = initSequence.iterator()
        override fun hasNext(): Boolean = innerIterator.hasNext()
        override fun next(): CatchableEntry<O, O> = innerIterator.next().let { next -> CatchableEntry(next,next) }
    }
}
class TransformingCatchableSequence<N,E,O>(val catchableSequence: CatchableSequence<E, O>, private val transformer: (E) -> N): CatchableSequence<N, O> {
    override fun iterator(): Iterator<CatchableEntry<N, O>> = object : Iterator<CatchableEntry<N, O>> {
        val iterator = catchableSequence.iterator()
        override fun hasNext(): Boolean = iterator.hasNext()

        override fun next(): CatchableEntry<N, O> {
            val next= iterator.next()
            return if(next.hasErr) {
                next.noChange()
            } else {
                runCatching { next.addEntry((transformer(next.entry!!))) }.getOrElse { next.addErr(it) }
            }
        }
    }
}
internal class FilteringCatchableSequence<E,O>(
    val catchableSequence: CatchableSequence<E, O>,
    private val sendWhen: Boolean = true,
    private val predicate: (CatchableEntry<E, O>) -> Boolean
): CatchableSequence<E, O> {
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

fun <N,E,O> CatchableSequence<E, O>.map(transform: (E) -> N): CatchableSequence<N, O> = TransformingCatchableSequence(this, transform)
fun <T> Sequence<T>.catching(): CatchableSequence<T,T> = BaseCatchableSequence(this)
inline fun <E,O> CatchableSequence<E, O>.defaultFailures(crossinline default: () -> E): Sequence<E> = object : Sequence<E> {
    override fun iterator() = object : Iterator<E> {
        val innerIterator = this@defaultFailures.iterator()
        override fun hasNext(): Boolean = innerIterator.hasNext()
        override fun next(): E = innerIterator.next().entry ?: default()
    }
}
inline fun <E,O> CatchableSequence<E, O>.onEachFailure(crossinline receiver: (O, Throwable) -> Unit): CatchableSequence<E, O> = object : CatchableSequence<E, O> {
    override fun iterator(): Iterator<CatchableEntry<E, O>>  = object : Iterator<CatchableEntry<E, O>> {
        val innerIterator = this@onEachFailure.iterator()
        override fun hasNext(): Boolean = innerIterator.hasNext()
        override fun next(): CatchableEntry<E, O> = innerIterator.next().also { if (it.hasErr) {receiver(it.orig!!, it.err!!)} }
    }
}
fun <E,O> CatchableSequence<E, O>.toSequence(): Sequence<E?> = Sequence { object : Iterator<E?> {
    val iterator = this@toSequence.iterator()
    override fun hasNext() = iterator.hasNext()

    override fun next() = iterator.next().entry
} }

fun <E,O> CatchableSequence<E, O>.dropFailures(): Sequence<E> = (FilteringCatchableSequence(this, false) { entry -> entry.hasErr }).toSequence().filter { it != null } as Sequence<E>
