package eu.tjacobson.kotlin.utils.sequences.catchable

/**
 * A very close reimplementation of [Sequence] of the Kotlin standard library.
 * This resolves the elements of the sequence lazily, and that, through the use of the provided
 * extension functions, will capture any exceptions that occur against each element,
 * which can then be resolved, evaluated, or dropped.
 *
 * This implementation must always return an iterator of type [CatchableEntry],
 * which can carry either a [ValidEntry] or [ErrorEntry] depending on the evaluation outcome.
 *
 * @sample sampleUsage
 *
 * @param E the type of the current entry in the sequence
 * @param O the type of the original entry in the sequence
 */
interface CatchableSequence<E, O> {
    operator fun iterator(): Iterator<CatchableEntry<E, O>>
}

inline fun <T,R> Iterator<T>.onNext(crossinline nextFunction: (T) -> R) : Iterator<R> = object : Iterator<R> {
    override fun hasNext() = this@onNext.hasNext()
    override fun next(): R = nextFunction(this@onNext.next())
}
internal class BaseCatchableSequence<O>(private val initSequence: Sequence<O>) : CatchableSequence<O, O> {
    override fun iterator(): Iterator<CatchableEntry<O, O>> = initSequence.iterator().onNext { ValidEntry(it, it) }
}
internal class TransformingCatchableSequence<N, E, O>(private val catchableSequence: CatchableSequence<E, O>, private val transformer: (E) -> N) : CatchableSequence<N, O> {
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
@Suppress("UNCHECKED_CAST")
fun <E, O> CatchableSequence<E, O>.toNotNullSequence(): Sequence<E> = Sequence { object : Iterator<E> {
    val iterator = this@toNotNullSequence.iterator()
    var nextState: Int = -1
    var nextItem: E? = null

    private fun calcNext() {
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item is ValidEntry && item.entry != null) {
                nextItem = item.entry
                nextState = 1
                return
            }
        }
        nextState = 0
    }

    override fun next(): E {
        if (nextState == -1)
            calcNext()
        if (nextState == 0)
            throw NoSuchElementException()
        val result = nextItem
        nextItem = null
        nextState = -1
        return result as E
    }

    override fun hasNext(): Boolean {
        if (nextState == -1)
            calcNext()
        return nextState == 1
    }
} }
fun <E, O> CatchableSequence<E, O>.filterSuccesses(): Sequence<E?> = (FilteringCatchableSequence(this, true) { it is ValidEntry<E,O> }).toSequence()
fun <E, O> CatchableSequence<E, O>.filterFailures(): Sequence<E?> = (FilteringCatchableSequence(this, true) { it is ErrorEntry<O> }).toSequence()

private fun sampleUsage() {
    val sequence = sequenceOf("1","a","2")
    val result = sequence.catching()
        .map { it.toInt() }
        .filterSuccesses()
        .filterNotNull()
        .toList()
    assert(result == listOf(1,2))
}
