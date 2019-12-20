package eu.tjacobson.kotlin.utils.sequences.catchable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CatchableEntryTest {

    
    
    @BeforeEach
    fun setUp() {
    }

    @Test
    fun getHasErr_returns_false() {
        val entries: List<CatchableEntry<Any?, String>> = listOf(
            VALID_CATCHABLE_ENTRY_1,
            VALID_CATCHABLE_ENTRY_2,
            INVALID_CATCHABLE_ENTRY_1,
            VALID_CATCHABLE_ENTRY_1_PROCESSED,
            VALID_CATCHABLE_ENTRY_2_PROCESSED
            )
        for (entry in entries) {
            assertThat(entry)
                .isInstanceOf(ValidEntry::class.java)
        }
    }

    @Test
    fun getHasErr_returns_true() {
        assertThat(INVALID_CATCHABLE_ENTRY_1_PROCESSED)
            .isInstanceOf(ErrorEntry::class.java)
    }

    @Test
    fun addEntry() {
        val newEntry = VALID_CATCHABLE_ENTRY_1.addEntry(VALID_ENTRY_2)
        assertThat(newEntry)
            .isInstanceOf(ValidEntry::class.java)
            .extracting("entry", "orig")
            .containsExactly(VALID_ENTRY_2, VALID_ENTRY_1)
    }

    @Test
    fun addErr() {
        val newEntry = VALID_CATCHABLE_ENTRY_1.addErr(INVALID_ENTRY_2_ERROR)
        assertThat(newEntry)
            .isInstanceOf(ErrorEntry::class.java)
            .extracting( "err", "orig" )
            .containsExactly(INVALID_ENTRY_2_ERROR, VALID_ENTRY_1)
    }

    @Test
    fun getEntry() {
        assertThat(VALID_CATCHABLE_ENTRY_1.entry)
            .isEqualTo(VALID_ENTRY_1)
    }

    @Test
    fun getOrig() {
        assertThat(VALID_CATCHABLE_ENTRY_1.orig)
            .isEqualTo(VALID_ENTRY_1)
    }

    @Test
    fun getErr() {
        assertThat(INVALID_CATCHABLE_ENTRY_1_PROCESSED.err)
            .isEqualTo(INVALID_ENTRY_2_ERROR)
    }

}

const val VALID_ENTRY_1 = "1"
const val VALID_ENTRY_1_OUTPUT = 1
const val VALID_ENTRY_2 = "2"
const val VALID_ENTRY_2_OUTPUT = 2
const val INVALID_ENTRY_1 = "ampersand"
val INVALID_ENTRY_2_ERROR = Throwable("Some error")
val VALID_CATCHABLE_ENTRY_1 = ValidEntry(VALID_ENTRY_1, VALID_ENTRY_1)
val VALID_CATCHABLE_ENTRY_1_PROCESSED = ValidEntry(VALID_ENTRY_1_OUTPUT, VALID_ENTRY_1)
val VALID_CATCHABLE_ENTRY_2 = ValidEntry(VALID_ENTRY_2, VALID_ENTRY_2)
val VALID_CATCHABLE_ENTRY_2_PROCESSED = ValidEntry(VALID_ENTRY_2_OUTPUT, VALID_ENTRY_2)
val INVALID_CATCHABLE_ENTRY_1 = ValidEntry(INVALID_ENTRY_1, INVALID_ENTRY_1)
val INVALID_CATCHABLE_ENTRY_1_PROCESSED = ErrorEntry(INVALID_ENTRY_2_ERROR, INVALID_ENTRY_1)
