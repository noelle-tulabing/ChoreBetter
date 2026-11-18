package com.example.ui_practice

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.ui_practice.data.ChoreState
import com.example.ui_practice.ui.theme.UI_practiceTheme
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE)
        )
        val prefs = getSharedPreferences("chorebetter_demo", MODE_PRIVATE)
        val gson = Gson()
        val saved = prefs.getString("state", null)
        val initial = if (saved == null) ChoreState.demo() else runCatching { gson.fromJson(saved, ChoreState::class.java) }.getOrElse { ChoreState.demo() }
        setContent {
            var state by remember { mutableStateOf(initial) }
            UI_practiceTheme {
                ChoreApp(state) { next ->
                    check(prefs.edit().putString("state", gson.toJson(next)).commit()) { "Couldn't save changes. Please try again." }
                    state = next
                }
            }
        }
    }
}
