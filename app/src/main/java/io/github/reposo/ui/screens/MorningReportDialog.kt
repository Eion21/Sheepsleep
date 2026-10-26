package io.github.reposo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.reposo.data.NightReport
import io.github.reposo.data.SleepSession
import io.github.reposo.data.SleepStyle

/** Nombre corto del estilo, para tarjetas donde "Borrega ..." no cabe. */
fun styleLabel(style: SleepStyle): String = when (style) {
    SleepStyle.SOSEGADA -> "Sosegada"
    SleepStyle.SONADORA -> "Soñadora"
    SleepStyle.INQUIETA -> "Inquieta"
}

/**
 * Reporte de la mañana: aparece al detener la noche, al estilo de los reportes
 * de Pokémon Sleep pero con datos 100 % locales. La oveja reemplaza a cualquier
 * criatura ajena: el puntaje atrae "borregas" según lo bien que se durmió.
 */
@Composable
fun MorningReportDialog(
    session: SleepSession,
    report: NightReport,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo") }
        },
        title = { Text("Buenos días ☀️") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${report.score}",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = " / 100",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                Text(
                    text = "Borrega ${styleLabel(report.style)} 🐑",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when (report.style) {
                        SleepStyle.SOSEGADA -> "Casi no te moviste en toda la noche."
                        SleepStyle.SONADORA -> "Una noche tranquila, con movimiento normal."
                        SleepStyle.INQUIETA -> "Hubo bastante movimiento esta noche."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "En la cama: ${formatDuration(session.endMs!! - session.startMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                HorizontalDivider()

                Text(
                    text = "Duración ${report.durationPart} · Calma ${report.calmPart} · " +
                        "Sin interrupciones ${report.interruptionsPart}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (report.interruptions > 0) {
                    Text(
                        text = "${report.interruptions} episodio${if (report.interruptions == 1) "" else "s"} " +
                            "de inquietud de un minuto o más.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
