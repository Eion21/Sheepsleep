package io.github.reposo.player

import android.content.Intent
import android.os.Bundle
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture


/** Mantiene la reproducción en segundo plano (con la pantalla apagada) y la notificación. */
class PlaybackService : MediaSessionService() {

    private var engine: MixEngine? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val e = MixEngine(this)
        engine = e
        session = MediaSession.Builder(this, e.master).setCallback(Callback()).build()
        // El monitor de movimiento NO se arranca aquí: dependía de la reproducción, así
        // que si se empezaba la noche sin sonido no se registraba nada. Ahora lo gobierna
        // la sesión de sueño desde ReposoApp.
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        engine?.release()
        engine = null
        super.onDestroy()
    }

    private inner class Callback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(PlaybackCommands.SET_MIX)
                .add(PlaybackCommands.SET_VOLUME)
                .add(PlaybackCommands.SET_SLEEP_TIMER)
                .add(PlaybackCommands.SET_VOLUME_CAP)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                PlaybackCommands.SET_MIX.customAction -> engine?.setMix(
                    ids = args.getStringArray(PlaybackCommands.KEY_IDS)?.toList().orEmpty(),
                    volumes = args.getFloatArray(PlaybackCommands.KEY_VOLUMES)?.toList().orEmpty(),
                    title = args.getString(PlaybackCommands.KEY_TITLE) ?: "Mi mezcla",
                    fadeInMs = args.getLong(PlaybackCommands.KEY_FADE_MS, 0L),
                )
                PlaybackCommands.SET_VOLUME.customAction -> engine?.setVolume(
                    id = args.getString(PlaybackCommands.KEY_ID).orEmpty(),
                    volume = args.getFloat(PlaybackCommands.KEY_VOLUME),
                )
                PlaybackCommands.SET_SLEEP_TIMER.customAction -> engine?.setSleepTimer(
                    minutes = args.getInt(PlaybackCommands.KEY_MINUTES, 0),
                )
                PlaybackCommands.SET_VOLUME_CAP.customAction -> engine?.setNightCap(
                    cap = args.getFloat(PlaybackCommands.KEY_CAP, 1f),
                )
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
