package com.aemerse.muserse.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.databinding.ActivityRequestNotificationAccessBinding
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.service.NotificationListenerService
import com.aemerse.muserse.uiElementHelper.ColorHelper
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class ActivityRequestNotificationAccess : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityRequestNotificationAccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorHelper.setStatusBarGradiant(this)
        when (ApplicationClass.getPref().getInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.LIGHT)) {
            Constants.PRIMARY_COLOR.DARK -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.GLOSSY -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.LIGHT -> setTheme(R.style.AppThemeLight)
        }
        binding = ActivityRequestNotificationAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //findViewById(R.id.root_view_request_notification_access).setBackgroundDrawable(ColorHelper.GetGradientDrawableDark());
        findViewById<View>(R.id.request_button).setOnClickListener(this)
        binding.textSkip.setOnClickListener(this)
        binding.textNeverAsk.setOnClickListener(this)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onResume() {
        super.onResume()
        if (NotificationListenerService.isListeningAuthorized(this)) {
            launchMainActivity()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.request_button -> {
                val intent: Intent =
                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(intent)
                Toast.makeText(this, "Click on Muserse to enable!", Toast.LENGTH_LONG).show()
            }
            R.id.text_skip -> {
                launchMainActivity()
                binding.textSkip.visibility = View.GONE
                binding.textNeverAsk.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            }
            R.id.text_never_ask -> {
                binding.textNeverAsk.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                ApplicationClass.getPref().edit()
                    .putBoolean(getString(R.string.pref_never_ask_notification_permission), true)
                    .apply()
                launchMainActivity()
            }
        }
    }

    private fun launchMainActivity() {
        val mainActIntent = Intent(this, ActivityMain::class.java)
        startActivity(mainActIntent)
        finish()
    }
}