package com.example.prototypebug

import android.media.AudioManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity

abstract class AudioActivity : AppCompatActivity() {
    protected open val playsBattleMusic = false

    private val audio by lazy { GameAudioManager.from(applicationContext) }
    private val musicOwner = Any()
    private var audioResumed = false

    override fun onStart() {
        super.onStart()
        volumeControlStream = AudioManager.STREAM_MUSIC
        audio.screenStarted()
    }

    override fun onResume() {
        super.onResume()
        audioResumed = true
        if (playsBattleMusic) audio.resumeBattleMusic(musicOwner)
    }

    override fun onPause() {
        audioResumed = false
        audio.screenPaused()
        if (playsBattleMusic) {
            if (isFinishing) audio.releaseBattleMusic(musicOwner)
            else audio.pauseBattleMusic(musicOwner)
        }
        super.onPause()
    }

    override fun onStop() {
        audio.screenStopped()
        super.onStop()
    }

    override fun onDestroy() {
        if (playsBattleMusic) audio.releaseBattleMusic(musicOwner)
        super.onDestroy()
    }

    internal fun playSound(sound: GameSound) {
        if (audioResumed) audio.play(sound)
    }
}

internal fun View.setGameClickListener(playSound: (GameSound) -> Unit, onClick: () -> Unit) {
    isSoundEffectsEnabled = false
    setOnClickListener {
        playSound(GameSound.BUTTON_CLICK)
        onClick()
    }
}
