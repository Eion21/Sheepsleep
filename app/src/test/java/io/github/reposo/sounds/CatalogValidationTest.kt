package io.github.reposo.sounds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * El catálogo llega de internet y sus valores acaban siendo nombres de archivo locales.
 * Estas pruebas fijan que una entrada rara se descarta en vez de convertirse en una ruta.
 *
 * Se ejecuta con Robolectric porque `parseCatalog` usa `org.json`, que en los tests JVM
 * de Android es un stub sin implementación.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CatalogValidationTest {

    private val validHash = "a".repeat(64)

    @Test
    fun `una entrada correcta se acepta`() {
        assertTrue(isValidCatalogEntry("lluvia_suave", "lluvia_suave.ogg", validHash))
        assertTrue(isValidCatalogEntry("rain", "rain.wav", validHash))
    }

    @Test
    fun `el hash debe tener 64 caracteres hexadecimales`() {
        assertFalse(isValidCatalogEntry("rain", "rain.ogg", ""))
        assertFalse(isValidCatalogEntry("rain", "rain.ogg", "abc123"))
        assertFalse(isValidCatalogEntry("rain", "rain.ogg", "z".repeat(64)))
    }

    @Test
    fun `el hash en mayusculas tambien es valido`() {
        assertTrue(isValidCatalogEntry("rain", "rain.ogg", "A".repeat(64)))
    }

    @Test
    fun `el nombre del archivo debe coincidir con el id`() {
        // Si no coinciden, el archivo se guardaría con un nombre que luego no se
        // encuentra al reproducir: entrada inútil y confusa.
        assertFalse(isValidCatalogEntry("rain", "otro.ogg", validHash))
    }

    @Test
    fun `se rechazan rutas y separadores en el nombre`() {
        assertFalse(isValidCatalogEntry("../evil", "../evil.ogg", validHash))
        assertFalse(isValidCatalogEntry("rain", "sub/rain.ogg", validHash))
        assertFalse(isValidCatalogEntry("rain", "..\\rain.ogg", validHash))
    }

    @Test
    fun `se rechazan extensiones no previstas`() {
        assertFalse(isValidCatalogEntry("rain", "rain.apk", validHash))
        assertFalse(isValidCatalogEntry("rain", "rain.exe", validHash))
        assertFalse(isValidCatalogEntry("rain", "rain", validHash))
    }

    @Test
    fun `se rechaza un id vacio o con mayusculas`() {
        assertFalse(isValidCatalogEntry("", ".ogg", validHash))
        assertFalse(isValidCatalogEntry("Rain", "Rain.ogg", validHash))
    }

    @Test
    fun `el parseo descarta las entradas invalidas y conserva las buenas`() {
        val json = """
            {
              "sounds": [
                { "id": "rain", "file": "rain.ogg", "sha256": "${"a".repeat(64)}", "title": "Lluvia" },
                { "id": "../evil", "file": "../evil.ogg", "sha256": "${"b".repeat(64)}" },
                { "id": "fire", "file": "otro.ogg", "sha256": "${"c".repeat(64)}" },
                { "id": "wind", "file": "wind.ogg", "sha256": "corto" }
              ]
            }
        """.trimIndent()

        val result = parseCatalog(json)

        assertEquals(1, result.size)
        assertEquals("rain", result.single().id)
        assertEquals("Lluvia", result.single().title)
    }

    @Test
    fun `un catalogo sin sonidos devuelve lista vacia`() {
        assertEquals(0, parseCatalog("""{"sounds":[]}""").size)
    }
}
