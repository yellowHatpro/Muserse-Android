package com.aemerse.muserse.activity.preview_player

import android.text.TextUtils
import android.util.Log

internal object Logger {
    private val TAG: String = "AudioPreview"
    private fun isDebugging(): Boolean {
        return Log.isLoggable(TAG, Log.DEBUG)
    }

    /**
     * Log a debug message
     *
     * @param tag [String]
     * @param msg [String]
     *
     * @throws IllegalArgumentException [IllegalArgumentException]
     */
    @Throws(IllegalArgumentException::class)
    fun logd(tag: String, msg: String) {
        if (TextUtils.isEmpty(tag)) {
            throw IllegalArgumentException("'tag' cannot be empty!")
        }
        if (TextUtils.isEmpty(msg)) {
            throw IllegalArgumentException("'msg' cannot be empty!")
        }
        if (isDebugging()) {
            Log.d(TAG, "$tag [ $msg ]")
        }
    }

    /**
     * Log a debug message
     *
     * @param tag [String]
     * @param msg [String]
     *
     * @throws IllegalArgumentException [IllegalArgumentException]
     */
    @Throws(IllegalArgumentException::class)
    fun loge(tag: String, msg: String?) {
        if (TextUtils.isEmpty(tag)) {
            throw IllegalArgumentException("'tag' cannot be empty!")
        }
        if (TextUtils.isEmpty(msg)) {
            throw IllegalArgumentException("'msg' cannot be empty!")
        }
        Log.e(TAG, "$tag [ $msg ]")
    }
}