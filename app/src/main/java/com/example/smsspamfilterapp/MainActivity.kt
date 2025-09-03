package com.example.smsspamfilterapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.smsspamfilterapp.data.AppDatabase
import com.example.smsspamfilterapp.data.BayesianRepository
import com.example.smsspamfilterapp.data.MessageRepository
import com.example.smsspamfilterapp.databinding.ActivityMainBinding
import com.example.smsspamfilterapp.ui.InboxFragment
import com.example.smsspamfilterapp.ui.SpamFragment
import com.example.smsspamfilterapp.ui.MessageViewModel
import com.example.smsspamfilterapp.ml.SpamDetector
import com.google.android.material.navigation.NavigationView
import androidx.activity.viewModels
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val database by lazy { AppDatabase.getDatabase(application) }
    private val messageRepository by lazy { MessageRepository(database.messageDao()) }
    private val bayesianRepository by lazy { BayesianRepository(database.wordFrequencyDao()) }
    private val viewModel: MessageViewModel by viewModels {
        MessageViewModel.Factory(messageRepository, bayesianRepository)
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            checkDefaultSmsApp()
        } else {
            Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_LONG).show()
            updateStatus(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDrawer()
        setupNavigation()
        checkPermissions()
        setupFab()
        
        // Start with inbox fragment
        if (savedInstanceState == null) {
            navigateToInbox()
        }
    }

    private fun setupDrawer() {
        setSupportActionBar(binding.toolbar)
        
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Setup header views
        val headerView = binding.navigationView.getHeaderView(0)
        val statusText = headerView.findViewById<TextView>(R.id.navHeaderStatusText)
        val setDefaultButton = headerView.findViewById<Button>(R.id.navHeaderSetDefaultButton)
        
        setDefaultButton.setOnClickListener {
            requestDefaultSmsApp()
        }
    }

    private fun setupNavigation() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inbox -> {
                    navigateToInbox()
                    true
                }
                R.id.nav_spam -> {
                    navigateToSpam()
                    true
                }
                R.id.nav_settings -> {
                    // TODO: Navigate to settings
                    Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToInbox() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, InboxFragment())
            .commit()
        binding.toolbar.title = "Inbox"
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun navigateToSpam() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SpamFragment())
            .commit()
        binding.toolbar.title = "Spam"
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun checkPermissions() {
        val hasAllPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAllPermissions) {
            checkDefaultSmsApp()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun checkDefaultSmsApp() {
        val isDefault = Telephony.Sms.getDefaultSmsPackage(this) == packageName
        updateStatus(isDefault)
    }

    private fun requestDefaultSmsApp() {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        startActivity(intent)
    }

    private fun updateStatus(isDefault: Boolean) {
        val headerView = binding.navigationView.getHeaderView(0)
        val statusText = headerView.findViewById<TextView>(R.id.navHeaderStatusText)
        val setDefaultButton = headerView.findViewById<Button>(R.id.navHeaderSetDefaultButton)
        
        statusText.text = if (isDefault) {
            "Status: App is default SMS handler"
        } else {
            "Status: App is not default SMS handler"
        }
        setDefaultButton.isEnabled = !isDefault
    }

    override fun onResume() {
        super.onResume()
        checkDefaultSmsApp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            // TODO: Implement new message composition
            android.widget.Toast.makeText(this, "New message feature coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
