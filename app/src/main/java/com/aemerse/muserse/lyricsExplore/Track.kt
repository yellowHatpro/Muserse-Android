package com.aemerse.muserse.lyricsExplore

class Track {
    constructor(title: String, artist: String, playCount: Int, imageUrl: String) {
        this.title = title
        this.artist = artist
        this.playCount = playCount
        this.imageUrl = imageUrl
    }

    constructor()

    var title = ""
    var artist = ""
    var playCount = 0
    var imageUrl = ""
}