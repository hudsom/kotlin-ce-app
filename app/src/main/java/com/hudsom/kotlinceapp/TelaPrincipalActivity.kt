package com.hudsom.kotlinceapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TelaPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, TelaLoginActivity::class.java))
        finish()
    }
}
