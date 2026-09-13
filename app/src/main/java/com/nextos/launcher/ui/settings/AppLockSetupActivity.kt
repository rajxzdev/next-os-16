package com.nextos.launcher.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextos.launcher.R
import com.nextos.launcher.data.AppCatalog
import com.nextos.launcher.data.AppLockStore
import com.nextos.launcher.databinding.ActivityAppLockSetupBinding

class AppLockSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAppLockSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_back)
        val store = AppLockStore(this)
        binding.btnSavePin.setOnClickListener {
            val pin = binding.pinInput.text?.toString().orEmpty()
            if (pin.length < 4) return@setOnClickListener
            store.setPin(pin)
            Toast.makeText(this, R.string.pin_set, Toast.LENGTH_SHORT).show()
        }
        val apps = AppCatalog(this).allLaunchable(includeHidden = true)
        binding.lockList.layoutManager = LinearLayoutManager(this)
        binding.lockList.adapter = AppToggleAdapter(
            apps,
            checked = { store.isLocked(it.packageName) },
            onToggle = { app, on ->
                if (!store.hasPin()) {
                    Toast.makeText(this, R.string.set_pin, Toast.LENGTH_SHORT).show()
                    return@AppToggleAdapter
                }
                store.setLocked(app.packageName, on)
            }
        )
    }
}
