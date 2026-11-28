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
    // 효과음용 AudioAttributes (USAGE_GAME + CONTENT_TYPE_SONIFICATION: 미디어 볼륨 스트림과 동기화 및 저지연 직출력)
    private val sfxAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // 배경음(BGM)용 AudioAttributes (USAGE_GAME + CONTENT_TYPE_MUSIC)
    private val musicAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var startedScreens = 0
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(sfxAttributes)
        .build()

    private val sampleIds = mutableMapOf<Int, Int>()
    private val loadedSamples = mutableSetOf<Int>()
    private val pendingEffects = mutableSetOf<GameSound>()
    private val streams = mutableMapOf<GameSound, Int>()
    private val lastPlayed = mutableMapOf<GameSound, Long>()

    private var music: MediaPlayer? = null
    private var currentMusicResource: Int? = null
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        soundPool.setOnLoadCompleteListener { source, sampleId, status ->
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

        // 효과음 자원을 싱글톤 인스턴스 생성 시 1회 사전 로드하여 앱 실행 동안 상주시킴
        for (resource in GameAudioConfig.effects.values.map { it.resource }.distinct()) {
            val sampleId = soundPool.load(context, resource, 1)
            if (sampleId != 0) {
                sampleIds[resource] = sampleId
            } else {
                Log.w(TAG, "SoundPool could not load resource $resource")
            }
        }
    }

    /** 설정창의 배경음 크기 (0~100). 앱 전체 공용이라 어느 화면에서 바꿔도 똑같이 적용되고 저장됨 */
    var musicLevel: Int = prefs.getInt(KEY_MUSIC_LEVEL, MAX_LEVEL).coerceIn(0, MAX_LEVEL)
        set(value) {
            field = value.coerceIn(0, MAX_LEVEL)
            prefs.edit().putInt(KEY_MUSIC_LEVEL, field).apply()
            // 이미 나오고 있는 배경음도 바로 크기를 바꿈
            if (musicPrepared) music?.let { val v = musicVolume(); it.setVolume(v, v) }
        }

    /** 설정창의 효과음 크기 (0~100). 다음에 나는 효과음부터 적용됨 */
    var effectLevel: Int = prefs.getInt(KEY_EFFECT_LEVEL, MAX_LEVEL).coerceIn(0, MAX_LEVEL)
        set(value) {
            field = value.coerceIn(0, MAX_LEVEL)
            prefs.edit().putInt(KEY_EFFECT_LEVEL, field).apply()
        }

    private fun musicVolume() =
        (GameAudioConfig.MUSIC_VOLUME * musicLevel / MAX_LEVEL).coerceIn(0f, 1f)

    private var musicOwner: Any? = null
    private var musicPrepared = false
    private var musicRequested = false

    fun screenStarted() {
        startedScreens++
    }

    fun screenPaused() {
        pendingEffects.clear()
    }

    fun screenStopped() {
        if (--startedScreens <= 0) {
            startedScreens = 0
            pendingEffects.clear()
            streams.clear()
        }
    }

    fun play(sound: GameSound) {
        val effect = GameAudioConfig.effects.getValue(sound)
        val sampleId = sampleIds[effect.resource] ?: return
        if (sampleId !in loadedSamples) {
            if (effect.waitForLoad) pendingEffects.add(sound)
            return
        }

        val now = SystemClock.elapsedRealtime()
        val previous = lastPlayed[sound]
        if (previous != null && now - previous < effect.cooldownMs) return

        val volume = (GameAudioConfig.EFFECT_VOLUME * effect.volume * effectLevel / MAX_LEVEL).coerceIn(0f, 1f)
        if (volume <= 0f) return

        val stream = soundPool.play(sampleId, volume, volume, effect.priority, 0, 1f)
        if (stream != 0) {
            streams[sound] = stream
            lastPlayed[sound] = now
        }
    }

    fun resumeBattleMusic(owner: Any, @RawRes resource: Int) {
        if (musicOwner !== owner || currentMusicResource != resource) {
            releaseMusic()
        }
        musicOwner = owner
        currentMusicResource = resource
        musicRequested = true

        music?.let {
            if (musicPrepared && !it.isPlaying) it.start()
            return
        }

        val player = MediaPlayer()
        music = player
        try {
            player.setAudioAttributes(musicAttributes)
            context.resources.openRawResourceFd(resource).use { asset ->
                player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            player.setOnPreparedListener { prepared ->
                if (prepared === music) {
                    musicPrepared = true
                    prepared.isLooping = true
                    val volume = musicVolume()
                    prepared.setVolume(volume, volume)
                    if (musicRequested && !prepared.isPlaying) prepared.start()
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
        currentMusicResource = null
        musicPrepared = false
        musicRequested = false
    }

    companion object {
        private const val TAG = "GameAudio"
        private const val PREFS_NAME = "game_settings"
        private const val KEY_MUSIC_LEVEL = "music_level"
        private const val KEY_EFFECT_LEVEL = "effect_level"
        const val MAX_LEVEL = 100
        private var instance: GameAudioManager? = null

        fun from(context: Context): GameAudioManager {
            return instance ?: GameAudioManager(context.applicationContext).also { instance = it }
        }
    }
}
