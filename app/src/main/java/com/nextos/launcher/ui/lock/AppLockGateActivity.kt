package com.nextos.launcher.ui.lock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextos.launcher.R
import com.nextos.launcher.data.AppLockStore
import com.nextos.launcher.databinding.ActivityAppLockGateBinding
import com.nextos.launcher.util.AppLauncher

class AppLockGateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAppLockGateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val store = AppLockStore(this)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }
        val launch = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_LAUNCH, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_LAUNCH)
        }
        binding.gateTitle.text = getString(R.string.enter_pin)
        binding.btnUnlock.setOnClickListener {
            val pin = binding.gatePin.text?.toString().orEmpty()
            if (store.verifyPin(pin)) {
                store.unlockSession(pkg)
                if (launch != null) {
                    AppLauncher.launch(this, launch, pkg, null)
                } else {
                    val i = packageManager.getLaunchIntentForPackage(pkg)
                    if (i != null) AppLauncher.launch(this, i, pkg, null)
                }
                finish()
            } else {
                Toast.makeText(this, R.string.wrong_pin, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "pkg"
        const val EXTRA_LAUNCH = "launch"
    }
}
