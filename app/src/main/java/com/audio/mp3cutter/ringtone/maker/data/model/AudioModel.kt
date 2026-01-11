package com.audio.mp3cutter.ringtone.maker.data.model

data class AudioModel(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val size: Long,
    val path: String,
    val contentUri: String
)
