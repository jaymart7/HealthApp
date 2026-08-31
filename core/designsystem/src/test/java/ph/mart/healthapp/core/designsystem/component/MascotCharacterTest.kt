package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class MascotCharacterTest {

    @Test
    fun `every character round-trips through its own name`() {
        MascotCharacter.entries.forEach { character ->
            assertEquals(character, mascotCharacterOf(character.name))
        }
    }

    @Test
    fun `an absent or unrecognised name is the default`() {
        assertEquals(MascotCharacter.Bibo, mascotCharacterOf(null))
        assertEquals(MascotCharacter.Bibo, mascotCharacterOf(""))
        // A pick from a newer build, or a hand-edited export.
        assertEquals(MascotCharacter.Bibo, mascotCharacterOf("Nonexistent"))
        assertEquals(MascotCharacter.Bibo, mascotCharacterOf("bibo"))
    }
}
