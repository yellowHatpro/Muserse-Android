package com.aemerse.muserse.activity

import android.annotation.SuppressLint
import android.content.Context
import android.media.audiofx.PresetReverb
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.customViews.VerticalSeekBar
import com.aemerse.muserse.equalizer.EqualizerSetting
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.uiElementHelper.ColorHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.InputCallback
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.ItemListener
import com.afollestad.materialdialogs.list.listItems
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class ActivityEqualizer : AppCompatActivity() {
    //views
    @JvmField
    @BindView(R.id.equalizerScrollView)
    var mScrollView: ScrollView? = null

    // 50Hz equalizer controls.
    @JvmField
    @BindView(R.id.equalizer50Hz)
    var equalizer50HzSeekBar: VerticalSeekBar? = null

    @JvmField
    @BindView(R.id.text50HzGain)
    var text50HzGainTextView: TextView? = null


    // 130Hz equalizer controls.
    @JvmField
    @BindView(R.id.equalizer130Hz)
    var equalizer130HzSeekBar: VerticalSeekBar? = null

    @JvmField
    @BindView(R.id.text130HzGain)
    var text130HzGainTextView: TextView? = null

    @JvmField
    @BindView(R.id.text130Hz)
    var text130Hz: TextView? = null

    // 320Hz equalizer controls.
    @JvmField
    @BindView(R.id.equalizer320Hz)
    var equalizer320HzSeekBar: VerticalSeekBar? = null

    @JvmField
    @BindView(R.id.text320HzGain)
    var text320HzGainTextView: TextView? = null

    // 800 Hz equalizer controls.
    @JvmField
    @BindView(R.id.equalizer800Hz)
    var equalizer800HzSeekBar: VerticalSeekBar? = null

    @JvmField
    @BindView(R.id.text800HzGain)
    var text800HzGainTextView: TextView? = null

    // 2 kHz equalizer controls.
    @JvmField
    @BindView(R.id.equalizer2kHz)
    var equalizer2kHzSeekBar: VerticalSeekBar? = null

    @JvmField
    @BindView(R.id.text2kHzGain)
    var text2kHzGainTextView: TextView? = null

    // 5 kHz equalizer controls.
    @JvmField
    @BindView(R.id.equalizer5kHz)
    var equalizer5kHzSeekBar: VerticalSeekBar? = null

    @JvmField
    @BindView(R.id.text5kHzGain)
    var text5kHzGainTextView: TextView? = null


    // 12.5 kHz equalizer controls.
    @JvmField
    @BindView(R.id.equalizer12_5kHz)
    var equalizer12_5kHzSeekBar: VerticalSeekBar? = null

    @JvmField
    @BindView(R.id.text12_5kHzGain)
    var text12_5kHzGainTextView: TextView? = null

    // Equalizer preset controls.
    @JvmField
    @BindView(R.id.loadPresetButton)
    var loadPresetButton: RelativeLayout? = null

    @JvmField
    @BindView(R.id.saveAsPresetButton)
    var saveAsPresetButton: RelativeLayout? = null

    @JvmField
    @BindView(R.id.resetAllButton)
    var resetAllButton: RelativeLayout? = null


    // Temp variables that hold the equalizer's settings.
    private var fiftyHertzLevel = 16
    private var oneThirtyHertzLevel = 16
    private var threeTwentyHertzLevel = 16
    private var eightHundredHertzLevel = 16
    private var twoKilohertzLevel = 16
    private var fiveKilohertzLevel = 16
    private var twelvePointFiveKilohertzLevel = 16

    // Temp variables that hold audio fx settings.
    private var virtualizerLevel = 0
    private var bassBoostLevel = 0
    private var enhancementLevel = 0
    private var reverbSetting = 0

    //Audio FX elements.
    @JvmField
    @BindView(R.id.virtualizer_seekbar)
    var virtualizerSeekBar: SeekBar? = null

    @JvmField
    @BindView(R.id.bass_boost_seekbar)
    var bassBoostSeekBar: SeekBar? = null

    @JvmField
    @BindView(R.id.enhancer_seekbar)
    var enhanceSeekBar: SeekBar? = null

    @JvmField
    @BindView(R.id.reverb_spinner)
    var reverbSpinner: Spinner? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorHelper.setStatusBarGradiant(this)
        when (ApplicationClass.getPref().getInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.LIGHT)) {
            Constants.PRIMARY_COLOR.DARK -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.GLOSSY -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.LIGHT -> setTheme(R.style.AppThemeLight)
        }
        setContentView(R.layout.activity_equalizer)

        //action bar
        val toolbar: Toolbar = findViewById(R.id.toolbar_)
        toolbar.setTitle(R.string.equalizer_title)
        setSupportActionBar(toolbar)

        // add back arrow to toolbar
        if (supportActionBar != null) {
            //getSupportActionBar().setBackgroundDrawable(ColorHelper.GetGradientDrawableToolbar());
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        //mScrollView.setBackgroundDrawable(ColorHelper.GetGradientDrawableDark());

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ColorHelper.GetStatusBarColor());
        }*/

        //Init reverb presets.
        val reverbPresets = ArrayList<String>()
        reverbPresets.add(getString(R.string.preset_none))
        reverbPresets.add(getString(R.string.preset_large_hall))
        reverbPresets.add(getString(R.string.preset_large_room))
        reverbPresets.add(getString(R.string.preset_medium_hall))
        reverbPresets.add(getString(R.string.preset_medium_room))
        reverbPresets.add(getString(R.string.preset_small_room))
        reverbPresets.add(getString(R.string.preset_plate))
        val dataAdapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, reverbPresets)
        dataAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        reverbSpinner!!.adapter = dataAdapter

        //Set the max values for the seekbars.
        virtualizerSeekBar!!.max = 1000
        bassBoostSeekBar!!.max = 1000
        enhanceSeekBar!!.max = 1000
        resetAllButton!!.setOnClickListener { //Reset all sliders to 0.
            equalizer50HzSeekBar!!.setProgressAndThumb(16)
            equalizer130HzSeekBar!!.setProgressAndThumb(16)
            equalizer320HzSeekBar!!.setProgressAndThumb(16)
            equalizer800HzSeekBar!!.setProgressAndThumb(16)
            equalizer2kHzSeekBar!!.setProgressAndThumb(16)
            equalizer5kHzSeekBar!!.setProgressAndThumb(16)
            equalizer12_5kHzSeekBar!!.setProgressAndThumb(16)
            virtualizerSeekBar!!.progress = 0
            bassBoostSeekBar!!.progress = 0
            enhanceSeekBar!!.progress = 0
            reverbSpinner!!.setSelection(0, false)

            //Apply the new setings to the service.
            applyCurrentEQSettings()

            //Show a confirmation toast.
            Toast.makeText(applicationContext, R.string.equ_reset_toast, Toast.LENGTH_SHORT)
                .show()
        }
        loadPresetButton!!.setOnClickListener { showLoadPresetDialog() }
        saveAsPresetButton!!.setOnClickListener { showSavePresetDialog() }
        equalizer50HzSeekBar!!.setOnSeekBarChangeListener(equalizer50HzListener)
        equalizer130HzSeekBar!!.setOnSeekBarChangeListener(equalizer130HzListener)
        equalizer320HzSeekBar!!.setOnSeekBarChangeListener(equalizer320HzListener)
        equalizer800HzSeekBar!!.setOnSeekBarChangeListener(equalizer800HzListener)
        equalizer2kHzSeekBar!!.setOnSeekBarChangeListener(equalizer2kHzListener)
        equalizer5kHzSeekBar!!.setOnSeekBarChangeListener(equalizer5kHzListener)
        equalizer12_5kHzSeekBar!!.setOnSeekBarChangeListener(equalizer12_5kHzListener)
        virtualizerSeekBar!!.setOnSeekBarChangeListener(virtualizerListener)
        bassBoostSeekBar!!.setOnSeekBarChangeListener(bassBoostListener)
        reverbSpinner!!.onItemSelectedListener = reverbListener
        enhanceSeekBar!!.setOnSeekBarChangeListener(enhanceListener)
        AsyncInitSlidersTask().execute(ApplicationClass.getService()?.getEqualizerHelper()?.getLastEquSetting())
        equalizer50HzSeekBar!!.setOnTouchListener(listener)
        equalizer130HzSeekBar!!.setOnTouchListener(listener)
        equalizer320HzSeekBar!!.setOnTouchListener(listener)
        equalizer800HzSeekBar!!.setOnTouchListener(listener)
        equalizer2kHzSeekBar!!.setOnTouchListener(listener)
        equalizer5kHzSeekBar!!.setOnTouchListener(listener)
        equalizer12_5kHzSeekBar!!.setOnTouchListener(listener)
    }

    var listener: View.OnTouchListener = View.OnTouchListener { v, event ->
        Log.d("ActivityEqualizer", "onTouch: $event")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mScrollView!!.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                mScrollView!!.requestDisallowInterceptTouchEvent(false)
            }
        }
        false
    }

    override fun onResume() {
        super.onResume()
        ApplicationClass.isAppVisible = true
    }

    override fun onPause() {
        try {
            val equalizerSetting = currentEquSetting
            ApplicationClass.getService()?.getEqualizerHelper()?.storeLastEquSetting(equalizerSetting)
            Log.d("ActivityEqualizer", "onPause: stored equ setting : $equalizerSetting")
            ApplicationClass.isAppVisible = false
        } catch (ignore: Exception) {
        }
        super.onPause()
    }

    private val currentEquSetting: EqualizerSetting get() {
            val equalizerSetting = EqualizerSetting()
            equalizerSetting.setFiftyHertz(fiftyHertzLevel)
            equalizerSetting.setOneThirtyHertz(oneThirtyHertzLevel)
            equalizerSetting.setThreeTwentyHertz(threeTwentyHertzLevel)
            equalizerSetting.setEightHundredHertz(eightHundredHertzLevel)
            equalizerSetting.setTwoKilohertz(twoKilohertzLevel)
            equalizerSetting.setFiveKilohertz(fiveKilohertzLevel)
            equalizerSetting.setTwelvePointFiveKilohertz(twelvePointFiveKilohertzLevel)
            equalizerSetting.setVirtualizer(virtualizerLevel)
            equalizerSetting.setBassBoost(bassBoostLevel)
            equalizerSetting.setEnhancement(enhancementLevel)
            equalizerSetting.setReverb(reverbSetting)
            return equalizerSetting
        }

    /**
     * 50 Hz equalizer seekbar listener.
     */
    private val equalizer50HzListener: SeekBar.OnSeekBarChangeListener = object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, seekBarLevel: Int, changedByUser: Boolean) {
            Log.d("ActivityEqualizer", "onProgressChanged : ")
            try {
                //Get the appropriate equalizer band.
                val sixtyHertzBand = ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.getBand(50000)

                //Set the gain level text based on the slider position.
                if(sixtyHertzBand!=null) {
                    when {
                        seekBarLevel == 16 -> {
                            text50HzGainTextView!!.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(sixtyHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    text50HzGainTextView!!.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(sixtyHertzBand, (-1500).toShort())
                                }
                                else -> {
                                    text50HzGainTextView!!.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(sixtyHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            text50HzGainTextView!!.text = "+" + (seekBarLevel - 16) + " dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(sixtyHertzBand,
                                    ((seekBarLevel - 16) * 100).toShort())
                        }
                    }
                }
                fiftyHertzLevel = seekBarLevel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {
            Log.d("ActivityEqualizer", "onStartTrackingTouch : ")
        }

        override fun onStopTrackingTouch(arg0: SeekBar) {
            Log.d("ActivityEqualizer", "onStopTrackingTouch : ")
        }
    }

    /**
     * 130 Hz equalizer seekbar listener.
     */
    private val equalizer130HzListener: SeekBar.OnSeekBarChangeListener = object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, seekBarLevel: Int, changedByUser: Boolean) {
            try {
                //Get the appropriate equalizer band.
                val twoThirtyHertzBand = ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.getBand(130000)

                if(twoThirtyHertzBand!=null) {
                    //Set the gain level text based on the slider position.
                    when {
                        seekBarLevel == 16 -> {
                            text130HzGainTextView!!.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(twoThirtyHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            if (seekBarLevel == 0) {
                                text130HzGainTextView!!.text = "-" + "15 dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(twoThirtyHertzBand, (-1500).toShort())
                            } else {
                                text130HzGainTextView!!.text = "-" + (16 - seekBarLevel) + " dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(
                                        twoThirtyHertzBand,
                                        (-((16 - seekBarLevel) * 100)).toShort())
                            }
                        }
                        seekBarLevel > 16 -> {
                            text130HzGainTextView!!.text = "+" + (seekBarLevel - 16) + " dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(
                                twoThirtyHertzBand,
                                ((seekBarLevel - 16) * 100).toShort())
                        }
                    }
                }
                oneThirtyHertzLevel = seekBarLevel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * 320 Hz equalizer seekbar listener.
     */
    private val equalizer320HzListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, seekBarLevel: Int, changedByUser: Boolean) {
            try {
                //Get the appropriate equalizer band.
                val nineTenHertzBand = ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.getBand(320000)

                //Set the gain level text based on the slider position.
                if(nineTenHertzBand!=null) {
                    when {
                        seekBarLevel == 16 -> {
                            text320HzGainTextView!!.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(nineTenHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            if (seekBarLevel == 0) {
                                text320HzGainTextView!!.text = "-" + "15 dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(nineTenHertzBand, (-1500).toShort())
                            } else {
                                text320HzGainTextView!!.text = "-" + (16 - seekBarLevel) + " dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(nineTenHertzBand,
                                        (-((16 - seekBarLevel) * 100)).toShort())
                            }
                        }
                        seekBarLevel > 16 -> {
                            text320HzGainTextView!!.text = "+" + (seekBarLevel - 16) + " dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(nineTenHertzBand,
                                    ((seekBarLevel - 16) * 100).toShort())
                        }
                    }
                }
                threeTwentyHertzLevel = seekBarLevel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * 800 Hz equalizer seekbar listener.
     */
    private val equalizer800HzListener = object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, seekBarLevel: Int, changedByUser: Boolean) {
            try {
                //Get the appropriate equalizer band.
                val threeKiloHertzBand = ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.getBand(800000)

                //Set the gain level text based on the slider position.
                if(threeKiloHertzBand!=null) {
                    when {
                        seekBarLevel == 16 -> {
                            text800HzGainTextView!!.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(threeKiloHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            if (seekBarLevel == 0) {
                                text800HzGainTextView!!.text = "-" + "15 dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(threeKiloHertzBand, (-1500).toShort())
                            } else {
                                text800HzGainTextView!!.text = "-" + (16 - seekBarLevel) + " dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(
                                        threeKiloHertzBand,
                                        (-((16 - seekBarLevel) * 100)).toShort())
                            }
                        }
                        seekBarLevel > 16 -> {
                            text800HzGainTextView!!.text = "+" + (seekBarLevel - 16) + " dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(threeKiloHertzBand,
                                    ((seekBarLevel - 16) * 100).toShort())
                        }
                    }
                }
                eightHundredHertzLevel = seekBarLevel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * 2 kHz equalizer seekbar listener.
     */
    private val equalizer2kHzListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, seekBarLevel: Int, changedByUser: Boolean) {
            try {
                //Get the appropriate equalizer band.
                val fourteenKiloHertzBand = ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.getBand(2000000)

                //Set the gain level text based on the slider position.
                if(fourteenKiloHertzBand!=null) {
                    when {
                        seekBarLevel == 16 -> {
                            text2kHzGainTextView!!.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fourteenKiloHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    text2kHzGainTextView!!.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fourteenKiloHertzBand, (-1500).toShort())
                                }
                                else -> {
                                    text2kHzGainTextView!!.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(
                                            fourteenKiloHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            text2kHzGainTextView!!.text = "+" + (seekBarLevel - 16) + " dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fourteenKiloHertzBand,
                                    ((seekBarLevel - 16) * 100).toShort())
                        }
                    }
                }
                twoKilohertzLevel = seekBarLevel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * 5 kHz equalizer seekbar listener.
     */
    private val equalizer5kHzListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, seekBarLevel: Int, changedByUser: Boolean) {
            try {
                //Get the appropriate equalizer band.
                val fiveKiloHertzBand = ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.getBand(5000000)

                if(fiveKiloHertzBand!=null) {
                    //Set the gain level text based on the slider position.
                    when {
                        seekBarLevel == 16 -> {
                            text5kHzGainTextView!!.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fiveKiloHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    text5kHzGainTextView!!.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fiveKiloHertzBand, (-1500).toShort())
                                }
                                else -> {
                                    text5kHzGainTextView!!.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(
                                            fiveKiloHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            text5kHzGainTextView!!.text = "+" + (seekBarLevel - 16) + " dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fiveKiloHertzBand,
                                    ((seekBarLevel - 16) * 100).toShort())
                        }
                    }
                }
                fiveKilohertzLevel = seekBarLevel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStartTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(arg0: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * 12.5 kHz equalizer seekbar listener.
     */
    private val equalizer12_5kHzListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(arg0: SeekBar, seekBarLevel: Int, changedByUser: Boolean) {
                try {
                    //Get the appropriate equalizer band.
                    val twelvePointFiveKiloHertzBand = ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.getBand(9000000)

                    if(twelvePointFiveKiloHertzBand!=null) {
                        //Set the gain level text based on the slider position.
                        when {
                            seekBarLevel == 16 -> {
                                text12_5kHzGainTextView!!.text = "0 dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(twelvePointFiveKiloHertzBand, 0.toShort())
                            }
                            seekBarLevel < 16 -> {
                                if (seekBarLevel == 0) {
                                    text12_5kHzGainTextView!!.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(twelvePointFiveKiloHertzBand,
                                            (-1500).toShort())
                                } else {
                                    text12_5kHzGainTextView!!.text =
                                        "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(
                                            twelvePointFiveKiloHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                            seekBarLevel > 16 -> {
                                text12_5kHzGainTextView!!.text = "+" + (seekBarLevel - 16) + " dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(
                                        twelvePointFiveKiloHertzBand,
                                        ((seekBarLevel - 16) * 100).toShort())
                            }
                        }
                    }
                    twelvePointFiveKilohertzLevel = seekBarLevel
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onStartTrackingTouch(arg0: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onStopTrackingTouch(arg0: SeekBar) {
                // TODO Auto-generated method stub
            }
        }

    /**
     * Spinner listener for reverb effects.
     */
    private val reverbListener: AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View, index: Int, arg3: Long) {
                reverbSetting = when (index) {
                    0 -> {
                        ApplicationClass.getService()?.getEqualizerHelper()?.getPresetReverb()?.preset =
                            PresetReverb.PRESET_NONE
                        0
                    }
                    1 -> {
                        ApplicationClass.getService()?.getEqualizerHelper()?.getPresetReverb()?.preset =
                            PresetReverb.PRESET_LARGEHALL
                        1
                    }
                    2 -> {
                        ApplicationClass.getService()?.getEqualizerHelper()?.getPresetReverb()?.preset =
                            PresetReverb.PRESET_LARGEROOM
                        2
                    }
                    3 -> {
                        ApplicationClass.getService()?.getEqualizerHelper()?.getPresetReverb()?.preset =
                            PresetReverb.PRESET_MEDIUMHALL
                        3
                    }
                    4 -> {
                        ApplicationClass.getService()?.getEqualizerHelper()?.getPresetReverb()?.preset =
                            PresetReverb.PRESET_MEDIUMROOM
                        4
                    }
                    5 -> {
                        ApplicationClass.getService()?.getEqualizerHelper()?.getPresetReverb()?.preset =
                            PresetReverb.PRESET_SMALLROOM
                        5
                    }
                    6 -> {
                        ApplicationClass.getService()?.getEqualizerHelper()?.getPresetReverb()?.preset =
                            PresetReverb.PRESET_PLATE
                        6
                    }
                    else -> 0
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                // TODO Auto-generated method stub
            }
        }

    /**
     * Bass boost listener.
     */
    private val bassBoostListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, arg1: Int, arg2: Boolean) {
            ApplicationClass.getService()?.getEqualizerHelper()?.getBassBoost()?.setStrength(arg1.toShort())
            bassBoostLevel = arg1
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Enhance listener.
     */
    private val enhanceListener = object : SeekBar.OnSeekBarChangeListener {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        override fun onProgressChanged(arg0: SeekBar, arg1: Int, arg2: Boolean) {
            ApplicationClass.getService()?.getEqualizerHelper()?.getEnhancer()?.setTargetGain(arg1)
            enhancementLevel = arg1
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Virtualizer listener.
     */
    private val virtualizerListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(arg0: SeekBar, arg1: Int, arg2: Boolean) {
            ApplicationClass.getService()?.getEqualizerHelper()?.getVirtualizer()?.setStrength(arg1.toShort())
            virtualizerLevel = arg1
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            // TODO Auto-generated method stub
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Builds the "Save Preset" dialog. Does not call the show() method, so you
     * should do this manually when calling this method.
     *
     * @return A fully built AlertDialog reference.
     */
    private fun showSavePresetDialog() {
        MaterialDialog(this)
            .title(R.string.title_save_preset)
            .input(hintRes = R.string.hint_save_preset, inputType = InputType.TYPE_CLASS_TEXT,
                callback =  object : InputCallback {
                override fun invoke(p1: MaterialDialog, input: CharSequence) {
                    if (input == "") {
                        Toast.makeText(applicationContext,
                            R.string.error_valid_preset_name_toast,
                            Toast.LENGTH_SHORT).show()
                        return
                    }
                    ApplicationClass.getService()?.getEqualizerHelper()?.insertPreset(input.toString(), currentEquSetting)
                    Toast.makeText(applicationContext,
                        R.string.preset_saved_toast,
                        Toast.LENGTH_SHORT).show()

                }
            })
            .negativeButton(R.string.cancel){}
            .show()
    }

    /**
     * Builds the "Load Preset" dialog. Does not call the show() method, so this
     * should be done manually after calling this method.
     *
     * @return A fully built AlertDialog reference.
     */
    private fun showLoadPresetDialog() {

        //load data from db here
        val array = ApplicationClass.getService()?.getEqualizerHelper()?.getPresetList()
        if (array != null) {
            for (s in array) {
                Log.d("ActivityEqualizer", "showLoadPresetDialog: array $s")
            }
        }
        MaterialDialog(this)
            .title(R.string.title_load_preset)
            .listItems(items = array as List<CharSequence>?
            , selection = object: ItemListener{
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        AsyncInitSlidersTask().execute(ApplicationClass.getService()?.getEqualizerHelper()?.getPreset(text.toString()))
                    }

                })
            .show()
    }

    /**
     * Applies the current EQ settings to the service.
     */
    private fun applyCurrentEQSettings() {
        when {
            ApplicationClass.getService() != null -> return
            else -> {
                equalizer50HzListener.onProgressChanged(equalizer50HzSeekBar,
                    equalizer50HzSeekBar!!.progress,
                    true)
                equalizer130HzListener.onProgressChanged(equalizer130HzSeekBar,
                    equalizer130HzSeekBar!!.progress,
                    true)
                equalizer320HzListener.onProgressChanged(equalizer320HzSeekBar!!,
                    equalizer320HzSeekBar!!.progress,
                    true)
                equalizer800HzListener.onProgressChanged(equalizer800HzSeekBar!!,
                    equalizer800HzSeekBar!!.progress,
                    true)
                equalizer2kHzListener.onProgressChanged(equalizer2kHzSeekBar!!,
                    equalizer2kHzSeekBar!!.progress,
                    true)
                equalizer5kHzListener.onProgressChanged(equalizer5kHzSeekBar!!,
                    equalizer5kHzSeekBar!!.progress,
                    true)
                equalizer12_5kHzListener.onProgressChanged(equalizer12_5kHzSeekBar!!,
                    equalizer12_5kHzSeekBar!!.progress,
                    true)
                virtualizerListener.onProgressChanged(virtualizerSeekBar!!,
                    virtualizerSeekBar!!.progress,
                    true)
                bassBoostListener.onProgressChanged(bassBoostSeekBar!!,
                    bassBoostSeekBar!!.progress,
                    true)
                enhanceListener.onProgressChanged(enhanceSeekBar!!, enhanceSeekBar!!.progress, true)
                reverbListener.onItemSelected(reverbSpinner,
                    null,
                    reverbSpinner!!.selectedItemPosition,
                    0L)
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class AsyncInitSlidersTask : AsyncTask<EqualizerSetting?, String?, Boolean?>() {
        var equalizerSetting: EqualizerSetting? = null
        override fun doInBackground(vararg p0: EqualizerSetting?): Boolean? {
            equalizerSetting = p0[0]
            return null
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            if (equalizerSetting == null) return
            Log.d("ActivityEqualizer",
                "onResume: found equ setting : " + equalizerSetting.toString())
            fiftyHertzLevel = equalizerSetting!!.getFiftyHertz()
            oneThirtyHertzLevel = equalizerSetting!!.getOneThirtyHertz()
            threeTwentyHertzLevel = equalizerSetting!!.getThreeTwentyHertz()
            eightHundredHertzLevel = equalizerSetting!!.getEightHundredHertz()
            twoKilohertzLevel = equalizerSetting!!.getTwoKilohertz()
            fiveKilohertzLevel = equalizerSetting!!.getFiveKilohertz()
            twelvePointFiveKilohertzLevel = equalizerSetting!!.getTwelvePointFiveKilohertz()
            virtualizerLevel = equalizerSetting!!.getVirtualizer()
            bassBoostLevel = equalizerSetting!!.getBassBoost()
            enhancementLevel = equalizerSetting!!.getEnhancement()
            reverbSetting = equalizerSetting!!.getReverb()

            //Move the sliders to the equalizer settings.
            equalizer50HzSeekBar!!.setProgressAndThumb(fiftyHertzLevel)
            equalizer130HzSeekBar!!.setProgressAndThumb(oneThirtyHertzLevel)
            equalizer320HzSeekBar!!.setProgressAndThumb(threeTwentyHertzLevel)
            equalizer800HzSeekBar!!.setProgressAndThumb(eightHundredHertzLevel)
            equalizer2kHzSeekBar!!.setProgressAndThumb(twoKilohertzLevel)
            equalizer5kHzSeekBar!!.setProgressAndThumb(fiveKilohertzLevel)
            equalizer12_5kHzSeekBar!!.setProgressAndThumb(twelvePointFiveKilohertzLevel)
            virtualizerSeekBar!!.progress = virtualizerLevel
            bassBoostSeekBar!!.progress = bassBoostLevel
            enhanceSeekBar!!.progress = enhancementLevel
            if (reverbSetting < reverbSpinner!!.adapter.count) reverbSpinner!!.setSelection(
                reverbSetting,
                false)

            //50Hz Band.
            when {
                fiftyHertzLevel == 16 -> {
                    text50HzGainTextView!!.text = "0 dB"
                }
                fiftyHertzLevel < 16 -> {
                    if (fiftyHertzLevel == 0) {
                        text50HzGainTextView!!.text = "-" + "15 dB"
                    } else {
                        text50HzGainTextView!!.text = "-" + (16 - fiftyHertzLevel) + " dB"
                    }
                }
                fiftyHertzLevel > 16 -> {
                    text50HzGainTextView!!.text = "+" + (fiftyHertzLevel - 16) + " dB"
                }
            }

            //130Hz Band.
            when {
                oneThirtyHertzLevel == 16 -> {
                    text130HzGainTextView!!.text = "0 dB"
                }
                oneThirtyHertzLevel < 16 -> {
                    if (oneThirtyHertzLevel == 0) {
                        text130HzGainTextView!!.text = "-" + "15 dB"
                    } else {
                        text130HzGainTextView!!.text = "-" + (16 - oneThirtyHertzLevel) + " dB"
                    }
                }
                oneThirtyHertzLevel > 16 -> {
                    text130HzGainTextView!!.text = "+" + (oneThirtyHertzLevel - 16) + " dB"
                }
            }

            //320Hz Band.
            when {
                threeTwentyHertzLevel == 16 -> {
                    text320HzGainTextView!!.text = "0 dB"
                }
                threeTwentyHertzLevel < 16 -> {
                    if (threeTwentyHertzLevel == 0) {
                        text320HzGainTextView!!.text = "-" + "15 dB"
                    } else {
                        text320HzGainTextView!!.text = "-" + (16 - threeTwentyHertzLevel) + " dB"
                    }
                }
                threeTwentyHertzLevel > 16 -> {
                    text320HzGainTextView!!.text = "+" + (threeTwentyHertzLevel - 16) + " dB"
                }
            }

            //800Hz Band.
            when {
                eightHundredHertzLevel == 16 -> {
                    text800HzGainTextView!!.text = "0 dB"
                }
                eightHundredHertzLevel < 16 -> {
                    if (eightHundredHertzLevel == 0) {
                        text800HzGainTextView!!.text = "-" + "15 dB"
                    } else {
                        text800HzGainTextView!!.text = "-" + (16 - eightHundredHertzLevel) + " dB"
                    }
                }
                eightHundredHertzLevel > 16 -> {
                    text800HzGainTextView!!.text = "+" + (eightHundredHertzLevel - 16) + " dB"
                }
            }

            //2kHz Band.
            when {
                twoKilohertzLevel == 16 -> {
                    text2kHzGainTextView!!.text = "0 dB"
                }
                twoKilohertzLevel < 16 -> {
                    if (twoKilohertzLevel == 0) {
                        text2kHzGainTextView!!.text = "-" + "15 dB"
                    } else {
                        text2kHzGainTextView!!.text = "-" + (16 - twoKilohertzLevel) + " dB"
                    }
                }
                twoKilohertzLevel > 16 -> {
                    text2kHzGainTextView!!.text = "+" + (twoKilohertzLevel - 16) + " dB"
                }
            }

            //5kHz Band.
            when {
                fiveKilohertzLevel == 16 -> {
                    text5kHzGainTextView!!.text = "0 dB"
                }
                fiveKilohertzLevel < 16 -> {
                    if (fiveKilohertzLevel == 0) {
                        text5kHzGainTextView!!.text = "-" + "15 dB"
                    } else {
                        text5kHzGainTextView!!.text = "-" + (16 - fiveKilohertzLevel) + " dB"
                    }
                }
                fiveKilohertzLevel > 16 -> {
                    text5kHzGainTextView!!.text = "+" + (fiveKilohertzLevel - 16) + " dB"
                }
            }

            //12.5kHz Band.
            when {
                twelvePointFiveKilohertzLevel == 16 -> {
                    text12_5kHzGainTextView!!.text = "0 dB"
                }
                twelvePointFiveKilohertzLevel < 16 -> {
                    if (twelvePointFiveKilohertzLevel == 0) {
                        text12_5kHzGainTextView!!.text = "-" + "15 dB"
                    } else {
                        text12_5kHzGainTextView!!.text = "-" + (16 - twelvePointFiveKilohertzLevel) + " dB"
                    }
                }
                twelvePointFiveKilohertzLevel > 16 -> {
                    text12_5kHzGainTextView!!.text = "+" + (twelvePointFiveKilohertzLevel - 16) + " dB"
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }
}