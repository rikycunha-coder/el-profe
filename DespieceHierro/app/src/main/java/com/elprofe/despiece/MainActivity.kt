package com.elprofe.despiece

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.elprofe.despiece.ui.App
import com.elprofe.despiece.ui.theme.TemaDespiece

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vm = ViewModelProvider(this)[AppViewModel::class.java]
        setContent {
            TemaDespiece {
                App(vm)
            }
        }
    }
}
