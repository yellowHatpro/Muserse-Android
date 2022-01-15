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
import com.aemerse.muserse.databinding.ActivityEqualizerBinding
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

    private lateinit var binding: ActivityEqualizerBinding

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
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.reverbSpinner.adapter = dataAdapter

        //Set the max values for the seekbars.
        binding.virtualizerSeekbar.max = 1000
        binding.bassBoostSeekbar.max = 1000
        binding.enhancerSeekbar.max = 1000
        binding.resetAllButton.setOnClickListener { //Reset all sliders to 0.
            binding.equalizer50Hz.setProgressAndThumb(16)
            binding.equalizer130Hz.setProgressAndThumb(16)
            binding.equalizer320Hz.setProgressAndThumb(16)
            binding.equalizer800Hz.setProgressAndThumb(16)
            binding.equalizer2kHz.setProgressAndThumb(16)
            binding.equalizer5kHz.setProgressAndThumb(16)
            binding.equalizer125kHz.setProgressAndThumb(16)
            binding.virtualizerSeekbar.progress = 0
            binding.bassBoostSeekbar.progress = 0
            binding.enhancerSeekbar.progress = 0
            binding.reverbSpinner.setSelection(0, false)

            //Apply the new setings to the service.
            applyCurrentEQSettings()

            //Show a confirmation toast.
            Toast.makeText(applicationContext, R.string.equ_reset_toast, Toast.LENGTH_SHORT)
                .show()
        }
        binding.loadPresetButton.setOnClickListener { showLoadPresetDialog() }
        binding.saveAsPresetButton.setOnClickListener { showSavePresetDialog() }
        binding.equalizer50Hz.setOnSeekBarChangeListener(equalizer50HzListener)
        binding.equalizer130Hz.setOnSeekBarChangeListener(equalizer130HzListener)
        binding.equalizer320Hz.setOnSeekBarChangeListener(equalizer320HzListener)
        binding.equalizer800Hz.setOnSeekBarChangeListener(equalizer800HzListener)
        binding.equalizer2kHz.setOnSeekBarChangeListener(equalizer2kHzListener)
        binding.equalizer5kHz.setOnSeekBarChangeListener(equalizer5kHzListener)
        binding.equalizer125kHz.setOnSeekBarChangeListener(equalizer12_5kHzListener)
        binding.virtualizerSeekbar.setOnSeekBarChangeListener(virtualizerListener)
        binding.bassBoostSeekbar.setOnSeekBarChangeListener(bassBoostListener)
        binding.reverbSpinner.onItemSelectedListener = reverbListener
        binding.enhancerSeekbar.setOnSeekBarChangeListener(enhanceListener)
        AsyncInitSlidersTask().execute(ApplicationClass.getService()?.getEqualizerHelper()?.getLastEquSetting())
        binding.equalizer50Hz.setOnTouchListener(listener)
        binding.equalizer130Hz.setOnTouchListener(listener)
        binding.equalizer320Hz.setOnTouchListener(listener)
        binding.equalizer800Hz.setOnTouchListener(listener)
        binding.equalizer2kHz.setOnTouchListener(listener)
        binding.equalizer5kHz.setOnTouchListener(listener)
        binding.equalizer125kHz.setOnTouchListener(listener)
    }

    var listener: View.OnTouchListener = View.OnTouchListener { v, event ->
        Log.d("ActivityEqualizer", "onTouch: $event")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                binding.equalizerScrollView.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                binding.equalizerScrollView.requestDisallowInterceptTouchEvent(false)
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
                            binding.text50HzGain.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(sixtyHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    binding.text50HzGain.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(sixtyHertzBand, (-1500).toShort())
                                }
                                else -> {
                                    binding.text50HzGain.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(sixtyHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            binding.text50HzGain.text = "+" + (seekBarLevel - 16) + " dB"
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
                            binding.text130HzGain.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(twoThirtyHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    binding.text130HzGain.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(twoThirtyHertzBand, (-1500).toShort())
                                }
                                else -> {
                                    binding.text130HzGain.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(
                                            twoThirtyHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            binding.text130HzGain.text = "+" + (seekBarLevel - 16) + " dB"
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
                            binding.text320HzGain.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(nineTenHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    binding.text320HzGain.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(nineTenHertzBand, (-1500).toShort())
                                }
                                else -> {
                                    binding.text320HzGain.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(nineTenHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            binding.text320HzGain.text = "+" + (seekBarLevel - 16) + " dB"
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
                            binding.text800HzGain.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                ?.setBandLevel(threeKiloHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            if (seekBarLevel == 0) {
                                binding.text800HzGain.text = "-" + "15 dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(threeKiloHertzBand, (-1500).toShort())
                            } else {
                                binding.text800HzGain.text = "-" + (16 - seekBarLevel) + " dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                    ?.setBandLevel(
                                        threeKiloHertzBand,
                                        (-((16 - seekBarLevel) * 100)).toShort())
                            }
                        }
                        seekBarLevel > 16 -> {
                            binding.text800HzGain.text = "+" + (seekBarLevel - 16) + " dB"
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
                            binding.text2kHzGain.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fourteenKiloHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    binding.text2kHzGain.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fourteenKiloHertzBand, (-1500).toShort())
                                }
                                else -> {
                                    binding.text2kHzGain.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(
                                            fourteenKiloHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            binding.text2kHzGain.text = "+" + (seekBarLevel - 16) + " dB"
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
                            binding.text5kHzGain.text = "0 dB"
                            ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fiveKiloHertzBand, 0.toShort())
                        }
                        seekBarLevel < 16 -> {
                            when (seekBarLevel) {
                                0 -> {
                                    binding.text5kHzGain.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(fiveKiloHertzBand, (-1500).toShort())
                                }
                                else -> {
                                   binding.text5kHzGain.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(
                                            fiveKiloHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                        }
                        seekBarLevel > 16 -> {
                            binding.text5kHzGain.text = "+" + (seekBarLevel - 16) + " dB"
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
                                binding.text125kHzGain.text = "0 dB"
                                ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(twelvePointFiveKiloHertzBand, 0.toShort())
                            }
                            seekBarLevel < 16 -> {
                                if (seekBarLevel == 0) {
                                    binding.text125kHzGain.text = "-" + "15 dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()?.setBandLevel(twelvePointFiveKiloHertzBand,
                                            (-1500).toShort())
                                } else {
                                    binding.text125kHzGain.text = "-" + (16 - seekBarLevel) + " dB"
                                    ApplicationClass.getService()?.getEqualizerHelper()?.getEqualizer()
                                        ?.setBandLevel(
                                            twelvePointFiveKiloHertzBand,
                                            (-((16 - seekBarLevel) * 100)).toShort())
                                }
                            }
                            seekBarLevel > 16 -> {
                                binding.text125kHzGain.text = "+" + (seekBarLevel - 16) + " dB"
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
                equalizer50HzListener.onProgressChanged(binding.equalizer50Hz,
                    binding.equalizer50Hz.progress,
                    true)
                equalizer130HzListener.onProgressChanged(binding.equalizer130Hz,
                    binding.equalizer130Hz.progress,
                    true)
                equalizer320HzListener.onProgressChanged(binding.equalizer320Hz,
                    binding.equalizer320Hz.progress,
                    true)
                equalizer800HzListener.onProgressChanged(binding.equalizer800Hz,
                    binding.equalizer800Hz.progress,
                    true)
                equalizer2kHzListener.onProgressChanged(binding.equalizer2kHz,
                    binding.equalizer2kHz.progress,
                    true)
                equalizer5kHzListener.onProgressChanged(binding.equalizer5kHz,
                    binding.equalizer5kHz.progress,
                    true)
                equalizer12_5kHzListener.onProgressChanged(binding.equalizer125kHz,
                    binding.equalizer125kHz.progress,
                    true)
                virtualizerListener.onProgressChanged(binding.virtualizerSeekbar,
                    binding.virtualizerSeekbar.progress,
                    true)
                bassBoostListener.onProgressChanged(binding.bassBoostSeekbar,
                    binding.bassBoostSeekbar.progress,
                    true)
                enhanceListener.onProgressChanged(binding.enhancerSeekbar, binding.enhancerSeekbar.progress, true)
                reverbListener.onItemSelected(binding.reverbSpinner,
                    null,
                    binding.reverbSpinner.selectedItemPosition,
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
            binding.equalizer50Hz.setProgressAndThumb(fiftyHertzLevel)
            binding.equalizer130Hz.setProgressAndThumb(oneThirtyHertzLevel)
            binding.equalizer320Hz.setProgressAndThumb(threeTwentyHertzLevel)
            binding.equalizer800Hz.setProgressAndThumb(eightHundredHertzLevel)
            binding.equalizer2kHz.setProgressAndThumb(twoKilohertzLevel)
            binding.equalizer5kHz.setProgressAndThumb(fiveKilohertzLevel)
            binding.equalizer125kHz.setProgressAndThumb(twelvePointFiveKilohertzLevel)
            binding.virtualizerSeekbar.progress = virtualizerLevel
            binding.bassBoostSeekbar.progress = bassBoostLevel
            binding.enhancerSeekbar.progress = enhancementLevel
            if (reverbSetting < binding.reverbSpinner.adapter.count) binding.reverbSpinner.setSelection(
                reverbSetting,
                false)

            //50Hz Band.
            when {
                fiftyHertzLevel == 16 -> {
                    binding.text50HzGain.text = "0 dB"
                }
                fiftyHertzLevel < 16 -> {
                    if (fiftyHertzLevel == 0) {
                        binding.text50HzGain.text = "-" + "15 dB"
                    } else {
                        binding.text50HzGain.text = "-" + (16 - fiftyHertzLevel) + " dB"
                    }
                }
                fiftyHertzLevel > 16 -> {
                    binding.text50HzGain.text = "+" + (fiftyHertzLevel - 16) + " dB"
                }
            }

            //130Hz Band.
            when {
                oneThirtyHertzLevel == 16 -> {
                    binding.text130HzGain.text = "0 dB"
                }
                oneThirtyHertzLevel < 16 -> {
                    if (oneThirtyHertzLevel == 0) {
                        binding.text130HzGain.text = "-" + "15 dB"
                    } else {
                        binding.text130HzGain.text = "-" + (16 - oneThirtyHertzLevel) + " dB"
                    }
                }
                oneThirtyHertzLevel > 16 -> {
                    binding.text130HzGain.text = "+" + (oneThirtyHertzLevel - 16) + " dB"
                }
            }

            //320Hz Band.
            when {
                threeTwentyHertzLevel == 16 -> {
                    binding.text320HzGain.text = "0 dB"
                }
                threeTwentyHertzLevel < 16 -> {
                    if (threeTwentyHertzLevel == 0) {
                        binding.text320HzGain.text = "-" + "15 dB"
                    } else {
                        binding.text320HzGain.text = "-" + (16 - threeTwentyHertzLevel) + " dB"
                    }
                }
                threeTwentyHertzLevel > 16 -> {
                    binding.text320HzGain.text = "+" + (threeTwentyHertzLevel - 16) + " dB"
                }
            }

            //800Hz Band.
            when {
                eightHundredHertzLevel == 16 -> {
                    binding.text800HzGain.text = "0 dB"
                }
                eightHundredHertzLevel < 16 -> {
                    if (eightHundredHertzLevel == 0) {
                        binding.text800HzGain.text = "-" + "15 dB"
                    } else {
                        binding.text800HzGain.text = "-" + (16 - eightHundredHertzLevel) + " dB"
                    }
                }
                eightHundredHertzLevel > 16 -> {
                    binding.text800HzGain.text = "+" + (eightHundredHertzLevel - 16) + " dB"
                }
            }

            //2kHz Band.
            when {
                twoKilohertzLevel == 16 -> {
                    binding.text2kHzGain.text = "0 dB"
                }
                twoKilohertzLevel < 16 -> {
                    if (twoKilohertzLevel == 0) {
                        binding.text2kHzGain.text = "-" + "15 dB"
                    } else {
                        binding.text2kHzGain.text = "-" + (16 - twoKilohertzLevel) + " dB"
                    }
                }
                twoKilohertzLevel > 16 -> {
                    binding.text2kHzGain.text = "+" + (twoKilohertzLevel - 16) + " dB"
                }
            }

            //5kHz Band.
            when {
                fiveKilohertzLevel == 16 -> {
                    binding.text5kHzGain.text = "0 dB"
                }
                fiveKilohertzLevel < 16 -> {
                    if (fiveKilohertzLevel == 0) {
                        binding.text5kHzGain.text = "-" + "15 dB"
                    } else {
                        binding.text5kHzGain.text = "-" + (16 - fiveKilohertzLevel) + " dB"
                    }
                }
                fiveKilohertzLevel > 16 -> {
                    binding.text5kHzGain.text = "+" + (fiveKilohertzLevel - 16) + " dB"
                }
            }

            //12.5kHz Band.
            when {
                twelvePointFiveKilohertzLevel == 16 -> {
                    binding.text125kHzGain.text = "0 dB"
                }
                twelvePointFiveKilohertzLevel < 16 -> {
                    if (twelvePointFiveKilohertzLevel == 0) {
                        binding.text125kHzGain.text = "-" + "15 dB"
                    } else {
                        binding.text125kHzGain.text = "-" + (16 - twelvePointFiveKilohertzLevel) + " dB"
                    }
                }
                twelvePointFiveKilohertzLevel > 16 -> {
                    binding.text125kHzGain.text = "+" + (twelvePointFiveKilohertzLevel - 16) + " dB"
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }
}