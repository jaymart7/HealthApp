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

    @Test
    fun `no two characters share the same silhouette, eyes and accent`() {
        // The drawing itself can't be asserted on; a copy-pasted entry is the one failure mode
        // that would ship two buddies rendering identically.
        val looks = MascotCharacter.entries.map { Triple(it.body, it.eyes, it.accent) }
        assertEquals(MascotCharacter.entries.size, looks.toSet().size)
    }
}
