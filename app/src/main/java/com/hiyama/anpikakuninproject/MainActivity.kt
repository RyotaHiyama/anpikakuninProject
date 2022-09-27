package com.hiyama.anpikakuninproject

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.addCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.hiyama.anpikakuninproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

//        onBackPressedDispatcher.addCallback{
//            val builder = AlertDialog.Builder(activity)
//            builder.setTitle("ログアウトしますか？")
//                .setPositiveButton("はい") { _, _ ->
//                    finish()
//                }
//                .setNegativeButton("いいえ") { _, _ -> }
//                .show()
//        }
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("ログアウトしますか？")
//            .setPositiveButton("はい") { _, _ ->
//                finish()
//            }
//            .setNegativeButton("いいえ") { _, _ -> }
//            .show()
//    }
}