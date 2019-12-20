package eu.tjacobson.kotlin.utils.sequences.catchable

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

class ValidEntry<T,R>(val entry: T?, orig: R?) : CatchableEntry<T, R>(orig) {
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
