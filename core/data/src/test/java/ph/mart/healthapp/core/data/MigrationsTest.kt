package ph.mart.healthapp.core.data

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Holds [STEPS] against the exported schemas it was derived from. No emulator here, so this cannot
 * run the SQL — what it can do is catch the one mistake that actually happens: the database version
 * gets bumped, a schema is exported, and the migration for it is forgotten. It also fails on a
 * *removed* table or column, which the additive SQL in [STEPS] cannot express and which needs a
 * hand-written rebuild step instead.
 */
class MigrationsTest {

    private val schemaDir = File("schemas/ph.mart.healthapp.core.data.AppDatabase")

    private fun schema(version: Int): Map<String, Set<String>> {
        val db = Json.parseToJsonElement(File(schemaDir, "$version.json").readText())
            .jsonObject.getValue("database").jsonObject
        return db.getValue("entities").jsonArray.associate { entity ->
            val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
            table to entity.jsonObject.getValue("fields").jsonArray.map {
                it.jsonObject.getValue("columnName").jsonPrimitive.content
            }.toSet()
        }
    }

    private val latest: Int =
        schemaDir.listFiles().orEmpty().mapNotNull { it.nameWithoutExtension.toIntOrNull() }.max()

    @Test
    fun `every exported version has a step out of it`() {
        assertEquals((1 until latest).toSet(), STEPS.keys)
        assertEquals(STEPS.keys.map { it + 1 }.toSet(), MIGRATIONS.map { it.endVersion }.toSet())
    }

    @Test
    fun `each step creates the tables and columns that version added`() {
        for ((from, sql) in STEPS) {
            val before = schema(from)
            val after = schema(from + 1)
            for ((table, columns) in after) {
                // A rebuilt table is created under a temporary name and renamed back.
                val created = sql.filter {
                    it.startsWith("CREATE TABLE IF NOT EXISTS `$table`") ||
                        it.startsWith("CREATE TABLE IF NOT EXISTS `${table}_new`")
                }
                val old = before[table]
                if (old == null) {
                    assertTrue("$from -> ${from + 1}: no CREATE TABLE for $table", created.isNotEmpty())
                    continue
                }
                for (column in columns - old) {
                    val added = sql.any { it.startsWith("ALTER TABLE `$table` ADD COLUMN `$column` ") } ||
                        created.any { it.contains("`$column`") }
                    assertTrue("$from -> ${from + 1}: $table.$column is never added", added)
                }
            }
        }
    }

    @Test
    fun `no version drops a table or a column`() {
        for (from in 1 until latest) {
            val before = schema(from)
            val after = schema(from + 1)
            assertEquals("$from -> ${from + 1} drops a table", emptySet<String>(), before.keys - after.keys)
            for ((table, columns) in before) {
                val kept = after[table] ?: continue
                assertEquals("$from -> ${from + 1} drops a column", emptySet<String>(), columns - kept)
            }
        }
    }
}
