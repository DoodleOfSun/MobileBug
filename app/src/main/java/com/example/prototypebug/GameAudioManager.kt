package com.example.prototypebug

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RawRes

internal enum class GameSound {
    PLAYER_HIT, ENEMY_DEFEATED, EXP_PICKUP, LEVEL_UP, BUTTON_CLICK, VICTORY, DEFEAT
}

private data class SoundEffect(
    @param:RawRes val resource: Int,
    val volume: Float,
    val cooldownMs: Long,
    val priority: Int = 1,
    val waitForLoad: Boolean = false
)

private object GameAudioConfig {
    const val EFFECT_VOLUME = 0.75f
    const val MUSIC_VOLUME = 0.30f
    val battleMusic = R.raw.battle_bgm

    val effects = mapOf(
        GameSound.PLAYER_HIT to SoundEffect(R.raw.sfx_player_hit, 0.85f, 180, priority = 2),
        GameSound.ENEMY_DEFEATED to SoundEffect(R.raw.sfx_enemy_defeated, 0.55f, 90),
        GameSound.EXP_PICKUP to SoundEffect(R.raw.sfx_exp_pickup, 0.50f, 100),
        GameSound.LEVEL_UP to SoundEffect(R.raw.sfx_level_up, 0.90f, 350, priority = 3),
        GameSound.BUTTON_CLICK to SoundEffect(R.raw.sfx_button_click, 0.65f, 80),
        GameSound.VICTORY to SoundEffect(R.raw.sfx_victory, 0.90f, 0, priority = 3, waitForLoad = true),
        GameSound.DEFEAT to SoundEffect(R.raw.sfx_defeat, 0.90f, 0, priority = 3, waitForLoad = true)
    )
}

/** Used on the main thread. Keeps the application context and no Activity references. */
internal class GameAudioManager private constructor(private val context: Context) {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var startedScreens = 0
    private var soundPool: SoundPool? = null
    private val sampleIds = mutableMapOf<Int, Int>()
    private val loadedSamples = mutableSetOf<Int>()
    private val pendingEffects = mutableSetOf<GameSound>()
    private val streams = mutableMapOf<GameSound, Int>()
    private val lastPlayed = mutableMapOf<GameSound, Long>()

    private var music: MediaPlayer? = null
    private var musicOwner: Any? = null
    private var musicPrepared = false
    private var musicRequested = false

    fun screenStarted() {
        if (++startedScreens != 1) return

        val pool = SoundPool.Builder().setMaxStreams(5).setAudioAttributes(attributes).build()
        soundPool = pool
        pool.setOnLoadCompleteListener { source, sampleId, status ->
            if (source !== soundPool) return@setOnLoadCompleteListener
            val waiting = pendingEffects.filter { sampleIds[GameAudioConfig.effects.getValue(it).resource] == sampleId }
            pendingEffects.removeAll(waiting)
            if (status == 0) {
                loadedSamples.add(sampleId)
                waiting.forEach(::play)
            } else {
                sampleIds.values.remove(sampleId)
                Log.w(TAG, "SoundPool load failed for sample $sampleId (status $status)")
            }
        }

        for (resource in GameAudioConfig.effects.values.map { it.resource }.distinct()) {
            val sampleId = pool.load(context, resource, 1)
            if (sampleId != 0) {
                sampleIds[resource] = sampleId
            } else {
                Log.w(TAG, "SoundPool could not load resource $resource")
            }
        }
    }

    fun screenPaused() {
        pendingEffects.clear()
        // Let a short navigation click finish while another app screen is opening.
        val iterator = streams.iterator()
        while (iterator.hasNext()) {
            val (sound, stream) = iterator.next()
            if (sound != GameSound.BUTTON_CLICK) {
                soundPool?.stop(stream)
                iterator.remove()
            }
        }
    }

    fun screenStopped() {
        if (--startedScreens != 0) return

        soundPool?.release()
        soundPool = null
        sampleIds.clear()
        loadedSamples.clear()
        pendingEffects.clear()
        streams.clear()
        lastPlayed.clear()
    }

    fun play(sound: GameSound) {
        val pool = soundPool ?: return
        val effect = GameAudioConfig.effects.getValue(sound)
        val sampleId = sampleIds[effect.resource] ?: return
        if (sampleId !in loadedSamples) {
            if (effect.waitForLoad) pendingEffects.add(sound)
            return
        }

        val now = SystemClock.elapsedRealtime()
        val previous = lastPlayed[sound]
        if (previous != null && now - previous < effect.cooldownMs) return

        streams.remove(sound)?.let { pool.stop(it) }
        val volume = (GameAudioConfig.EFFECT_VOLUME * effect.volume).coerceIn(0f, 1f)
        val stream = pool.play(sampleId, volume, volume, effect.priority, 0, 1f)
        if (stream != 0) {
            streams[sound] = stream
            lastPlayed[sound] = now
        }
    }

    fun resumeBattleMusic(owner: Any) {
        if (musicOwner !== owner) releaseMusic()
        musicOwner = owner
        musicRequested = true

        music?.let {
            if (musicPrepared) it.start()
            return
        }

        val player = MediaPlayer()
        music = player
        try {
            player.setAudioAttributes(attributes)
            context.resources.openRawResourceFd(GameAudioConfig.battleMusic).use { asset ->
                player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            player.setOnPreparedListener { prepared ->
                if (prepared === music) {
                    musicPrepared = true
                    prepared.isLooping = true
                    val volume = GameAudioConfig.MUSIC_VOLUME.coerceIn(0f, 1f)
                    prepared.setVolume(volume, volume)
                    if (musicRequested) prepared.start()
                }
            }
            player.setOnErrorListener { failed, what, extra ->
                if (failed === music) {
                    Log.w(TAG, "Battle music playback failed ($what, $extra)")
                    releaseMusic()
                }
                true
            }
            player.prepareAsync()
        } catch (error: Exception) {
            Log.w(TAG, "Could not prepare battle music", error)
            releaseMusic()
        }
    }

    fun pauseBattleMusic(owner: Any) {
        if (musicOwner !== owner) return
        musicRequested = false
        if (musicPrepared) music?.pause()
    }

    fun releaseBattleMusic(owner: Any) {
        if (musicOwner === owner) releaseMusic()
    }

    private fun releaseMusic() {
        music?.release()
        music = null
        musicOwner = null
        musicPrepared = false
        musicRequested = false
    }

    companion object {
        private const val TAG = "GameAudio"
        private var instance: GameAudioManager? = null

        fun from(context: Context): GameAudioManager {
            return instance ?: GameAudioManager(context.applicationContext).also { instance = it }
        }
    }
}
