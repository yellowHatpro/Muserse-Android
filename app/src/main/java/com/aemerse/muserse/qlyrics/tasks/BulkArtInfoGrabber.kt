package com.aemerse.muserse.qlyrics.tasks


//this thread is started from Music library once all artists are loaded
object BulkArtInfoGrabber : Thread() {
    //make sure only one instance of this thread runs at time
    private val artInfoGrabberThreadRunning: Boolean = false

    //makes sure artist info is downloaded one after another
    private val artistInfoThreadRunning: Boolean = false

    //if thread runs more than HALF HOUR, kill it
    private val THREAD_TIMEOUT: Long = (30 * 60 * 1000).toLong()
}