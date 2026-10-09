package com.rehan.verbum

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rehan.verbum.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener { saveConfig() }
        binding.btnSyncNow.setOnClickListener { syncNow() }

        refresh()
        checkSyncStatus()
    }

    override fun onResume() {
        super.onResume()
        checkSyncStatus()
    }

    private fun checkSyncStatus() {
        if (!Prefs.isConfigured(this)) return
        val today = java.time.LocalDate.now().toString()
        if (Prefs.lastSyncDate(this) != today) {
            Toast.makeText(this, "Auto-sync for today is pending. Tap Sync now to refresh manually.", Toast.LENGTH_LONG).show()
        }
    }

    private fun refresh() {
        val configured = Prefs.isConfigured(this)
        binding.setupCard.visibility = if (configured) View.GONE else View.VISIBLE
        binding.statusCard.visibility = if (configured) View.VISIBLE else View.GONE

        if (configured) {
            val lastSync = Prefs.lastSyncDate(this).ifBlank { "never" }
            binding.tvLastSync.text = "Last sync: $lastSync"
            binding.tvRepo.text = "${Prefs.owner(this)}/${Prefs.repo(this)}"
        }
    }

    private fun saveConfig() {
        val owner = binding.etOwner.text.toString().trim()
        val repo = binding.etRepo.text.toString().trim()
        val token = binding.etToken.text.toString().trim()

        if (owner.isBlank() || repo.isBlank() || token.isBlank()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
            return
        }

        Prefs.save(this, owner, repo, token)
        SyncWorker.schedule(this)
        refresh()
        Toast.makeText(this, "Saved! Daily sync scheduled.", Toast.LENGTH_SHORT).show()
    }

    private fun syncNow() {
        binding.btnSyncNow.isEnabled = false
        binding.tvLastSync.text = "Syncing…"
        SyncWorker.forceSync(this)
        lifecycleScope.launch {
            delay(1500)
            binding.btnSyncNow.isEnabled = true
            refresh()
        }
    }
}
