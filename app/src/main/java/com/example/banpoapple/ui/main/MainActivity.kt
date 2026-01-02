package com.example.banpoapple.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.banpoapple.R
import com.example.banpoapple.databinding.ActivityMainBinding
import com.example.banpoapple.BuildConfig
import com.example.banpoapple.ui.main.NotificationFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    openFragment(HomeFragment())
                    true
                }
                R.id.menu_groups -> {
                    openFragment(LearningFragment())
                    true
                }
                R.id.menu_admin -> {
                    openFragment(AdminFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.menu_home
        }

        // 데모: 루트/관리자만 메뉴 표시. 실제로는 로그인 정보로 역할 판별 필요.
        val isAdmin = true // TODO wire with actual role when backend exposes it
        if (!isAdmin) {
            binding.bottomNav.menu.removeItem(R.id.menu_admin)
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}
