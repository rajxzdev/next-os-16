package com.nextos.launcher.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.nextos.launcher.R
import com.nextos.launcher.databinding.ActivityPermissionsBinding
import com.nextos.launcher.util.PermissionHelper

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_back)
    }

    override fun onResume() {
        super.onResume()
        bind(
            binding.rowAccess.root, R.string.permission_access,
            PermissionHelper.hasAccessibility(this)
        ) { PermissionHelper.openAccessibility(this) }
        bind(
            binding.rowNotif.root, R.string.permission_notification,
            PermissionHelper.hasNotificationAccess(this)
        ) { PermissionHelper.openNotificationAccess(this) }
        bind(
            binding.rowUsage.root, R.string.permission_usage,
            PermissionHelper.hasUsageStats(this)
        ) { PermissionHelper.openUsageStats(this) }
        bind(
            binding.rowOverlay.root, R.string.permission_overlay,
            PermissionHelper.canDrawOverlays(this)
        ) { PermissionHelper.openOverlay(this) }
        bind(
            binding.rowHome.root, R.string.default_home,
            PermissionHelper.isDefaultHome(this)
        ) { PermissionHelper.requestDefaultHome(this) }
    }

    private fun bind(row: View, title: Int, ok: Boolean, action: () -> Unit) {
        row.findViewById<TextView>(R.id.permTitle).setText(title)
        row.findViewById<TextView>(R.id.permStatus).setText(if (ok) R.string.granted else R.string.not_granted)
        val btn = row.findViewById<MaterialButton>(R.id.permAction)
        btn.setText(if (ok) R.string.granted else R.string.grant)
        btn.isEnabled = !ok
        btn.setOnClickListener { action() }
    }
}
