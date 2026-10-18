package io.github.reposo.data

/**
 * Comparación de versiones semver relajada, extraída de [AppUpdateChecker] para poder
 * probarla sin red ni Android.
 *
 * Los segmentos no numéricos cuentan como 0, y un sufijo de prelanzamiento
 * (`-beta`, `-rc1`) hace que la versión sea **anterior** a la estable equivalente:
 * si no, `v1.0.0-beta` se ofrecería como actualización de `1.0.0`.
 */
internal object VersionComparator {

    /** Devuelve >0 si [a] es más nueva que [b], 0 si son equivalentes, <0 si es anterior. */
    fun compare(a: String, b: String): Int {
        val preA = preRelease(a)
        val preB = preRelease(b)
        val baseA = base(a).split(".").map { it.toIntOrNull() ?: 0 }
        val baseB = base(b).split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(baseA.size, baseB.size)
        for (i in 0 until len) {
            val va = baseA.getOrElse(i) { 0 }
            val vb = baseB.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        // Misma base: la estable gana a cualquier prelanzamiento.
        return when {
            preA == null && preB == null -> 0
            preA == null -> 1
            preB == null -> -1
            else -> preA.compareTo(preB)
        }
    }

    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    private fun base(version: String): String =
        version.removePrefix("v").substringBefore('-').substringBefore('+')

    private fun preRelease(version: String): String? =
        version.removePrefix("v").substringAfter('-', "").substringBefore('+').ifEmpty { null }
}
