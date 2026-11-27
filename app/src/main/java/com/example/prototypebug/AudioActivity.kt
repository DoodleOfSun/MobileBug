package com.example.prototypebug

import android.media.AudioManager
import android.view.View
import android.widget.SeekBar
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity

abstract class AudioActivity : AppCompatActivity() {
    @get:RawRes
    protected open val battleMusicResource: Int? = null

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
        battleMusicResource?.let { audio.resumeBattleMusic(musicOwner, it) }
    }

    override fun onPause() {
        audioResumed = false
        audio.screenPaused()
        if (battleMusicResource != null) {
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
        if (battleMusicResource != null) audio.releaseBattleMusic(musicOwner)
        super.onDestroy()
    }

    /** 설정창의 배경음·효과음 시크바를 저장된 값으로 맞추고, 움직이면 바로 적용·저장 */
    protected fun setupVolumeBars(settingsView: View) {
        val musicBar = settingsView.findViewById<SeekBar>(R.id.musicVolumeBar)
        val effectBar = settingsView.findViewById<SeekBar>(R.id.effectVolumeBar)
        musicBar.progress = audio.musicLevel
        effectBar.progress = audio.effectLevel
        musicBar.setOnSeekBarChangeListener(onLevelChanged { audio.musicLevel = it })
        effectBar.setOnSeekBarChangeListener(onLevelChanged { audio.effectLevel = it })
    }

    private fun onLevelChanged(apply: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) apply(progress)
        }
        override fun onStartTrackingTouch(bar: SeekBar) {}
        override fun onStopTrackingTouch(bar: SeekBar) {}
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
