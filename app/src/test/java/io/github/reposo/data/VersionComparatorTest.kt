package io.github.reposo.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comparación de versiones: es lo que decide si se ofrece una actualización, así que
 * un fallo aquí o molesta con avisos falsos o deja la app sin actualizarse nunca.
 */
class VersionComparatorTest {

    @Test
    fun `una version mayor es mas nueva`() {
        assertTrue(VersionComparator.isNewer("1.1.0", "1.0.0"))
        assertTrue(VersionComparator.isNewer("2.0.0", "1.9.9"))
        assertTrue(VersionComparator.isNewer("1.0.1", "1.0.0"))
    }

    @Test
    fun `la misma version no es mas nueva`() {
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.0"))
        // Los ceros de relleno no cuentan como diferencia.
        assertFalse(VersionComparator.isNewer("1.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.0.0", "1.0.0"))
    }

    @Test
    fun `una version menor no es mas nueva`() {
        assertFalse(VersionComparator.isNewer("0.9.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun `el prefijo v de las etiquetas de git se ignora`() {
        assertTrue(VersionComparator.isNewer("v1.2.0", "1.1.0"))
        assertFalse(VersionComparator.isNewer("v1.0.0", "1.0.0"))
    }

    @Test
    fun `un prelanzamiento no se ofrece como actualizacion de la estable`() {
        // v1.0.0-beta no debe reemplazar a 1.0.0 ya instalada.
        assertFalse(VersionComparator.isNewer("1.0.0-beta", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.0-rc1", "1.0.0"))
    }

    @Test
    fun `un prelanzamiento si es mas nuevo que la version anterior`() {
        assertTrue(VersionComparator.isNewer("1.1.0-beta", "1.0.0"))
    }

    @Test
    fun `una estable gana al prelanzamiento de la misma base`() {
        assertTrue(VersionComparator.isNewer("1.0.0", "1.0.0-beta"))
    }

    @Test
    fun `segmentos no numericos cuentan como cero`() {
        assertFalse(VersionComparator.isNewer("1.x.0", "1.0.0"))
    }
}
