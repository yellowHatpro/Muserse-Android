package com.aemerse.muserse.lyricsExplore

interface OnPopularTracksReady {
    fun popularTracksReady(
        tracks: List<Track>?,
        region: String
    )
    fun error()
}