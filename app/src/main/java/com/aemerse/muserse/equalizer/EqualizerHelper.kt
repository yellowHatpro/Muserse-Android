package com.aemerse.muserse.equalizer

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.audiofx.*
import com.google.gson.Gson
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.dbHelper.DbHelperEqualizer

class EqualizerHelper constructor(private val mContext: Context, audioSessionId: Int, equalizerEnabled: Boolean) {

    private var equalizer = Equalizer(0, audioSessionId)
    private var virtualizer = Virtualizer(0, audioSessionId)
    private var bassBoost = BassBoost(0, audioSessionId)
    private var presetReverb = PresetReverb(0, audioSessionId)
    private var loudnessEnhancer = LoudnessEnhancer(audioSessionId)
    private var isEqualizerSupported: Boolean = true
    private var dbHelperEqualizer = DbHelperEqualizer(mContext)

    fun releaseEqObjects() {
        equalizer.release()
        virtualizer.release()
        bassBoost.release()
        presetReverb.release()
    }

    fun getEqualizer(): Equalizer {
        return equalizer
    }

    fun getVirtualizer(): Virtualizer {
        return virtualizer
    }

    fun getBassBoost(): BassBoost {
        return bassBoost
    }

    fun getEnhancer(): LoudnessEnhancer {
        return loudnessEnhancer
    }

    fun getPresetReverb(): PresetReverb {
        return presetReverb
    }

    //general equ setting is stored in shared preference to reemove db complications and calls
    fun storeLastEquSetting(equSetting: EqualizerSetting?) {
        val jsonSetting: String = Gson().toJson(equSetting)
        ApplicationClass.getPref().edit().putString(mContext.getString(R.string.pref_last_equ_setting), jsonSetting).apply()
    }

    fun getLastEquSetting(): EqualizerSetting? {
        val jsonString = ApplicationClass.getPref().getString(mContext.getString(R.string.pref_last_equ_setting), "")
        return Gson().fromJson(jsonString, EqualizerSetting::class.java)
    }

    @SuppressLint("Range")
    fun getPresetList(): List<CharSequence?> {
        val cursor: Cursor = dbHelperEqualizer.readableDatabase.rawQuery("select * from " + DbHelperEqualizer.TABLE_NAME, null)
        val size: Int = cursor.count
        val array: Array<String?> = arrayOfNulls(size)
        var index = 0
        while (cursor.moveToNext()) {
            array[index] = cursor.getString(cursor.getColumnIndex(DbHelperEqualizer.EQU_PRESET_NAME))
            index++
        }
        cursor.close()
        return array.toList()
    }

    fun insertPreset(preset_name: String?, equalizerSetting: EqualizerSetting?) {
        val values = ContentValues()
        values.put(DbHelperEqualizer.EQU_PRESET_NAME, preset_name)
        values.put(DbHelperEqualizer.EQU_SETTING_STRING, Gson().toJson(equalizerSetting))
        dbHelperEqualizer.writableDatabase.insert(DbHelperEqualizer.TABLE_NAME, null, values)
    }

    @SuppressLint("Range")
    fun getPreset(preset_name: String): EqualizerSetting {
        val condition = DbHelperEqualizer.EQU_PRESET_NAME + "=" + "'" + preset_name.replace("'", "''") + "'"
        val columnsToReturn = arrayOf(DbHelperEqualizer.EQU_PRESET_NAME, DbHelperEqualizer.EQU_SETTING_STRING)
        val cursor = dbHelperEqualizer.readableDatabase.query(DbHelperEqualizer.TABLE_NAME, columnsToReturn, condition, null, null, null, null)
        return when {
            cursor != null && cursor.count > 0 -> {
                cursor.moveToFirst()
                val jsonString: String =
                    cursor.getString(cursor.getColumnIndex(DbHelperEqualizer.EQU_SETTING_STRING))
                val equalizerSetting: EqualizerSetting =
                    Gson().fromJson(jsonString,
                        EqualizerSetting::class.java)
                cursor.close()
                equalizerSetting
            }
            else -> {
                cursor?.close()
                EqualizerSetting()
            }
        }
    }

    fun isEqualizerSupported(): Boolean {
        return isEqualizerSupported
    }

    fun setEqualizerSupported(equalizerSupported: Boolean) {
        isEqualizerSupported = equalizerSupported
    }

    init {
        equalizer.enabled = equalizerEnabled
        virtualizer.enabled = equalizerEnabled
        bassBoost.enabled = equalizerEnabled
        presetReverb.enabled = equalizerEnabled
        loudnessEnhancer.enabled = equalizerEnabled
    }
}