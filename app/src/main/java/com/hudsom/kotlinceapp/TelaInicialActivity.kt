package com.hudsom.kotlinceapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.hudsom.kotlinceapp.databinding.TelaInicialBinding

class TelaInicialActivity : AppCompatActivity() {

    private lateinit var binding: TelaInicialBinding
    private lateinit var autenticacao: FirebaseAuth
    private lateinit var bancoDados: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = TelaInicialBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.principal) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        autenticacao = FirebaseAuth.getInstance()
        bancoDados = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).reference

        configurarDrawer()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun configurarDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.barraFerramentas,
            R.string.nome_app, R.string.nome_app
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = binding.navigationView.getHeaderView(0)
        val tvNome = headerView.findViewById<TextView>(R.id.tvNomeDrawer)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvEmailDrawer)

        val usuario = autenticacao.currentUser
        tvEmail.text = usuario?.email ?: ""

        usuario?.uid?.let { uid ->
            bancoDados.child("usuarios").child(uid).child("nome")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        tvNome.text = snapshot.getValue(String::class.java) ?: ""
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.navigationView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_nova_comunidade -> {
                    startActivity(Intent(this, TelaComunidadeActivity::class.java))
                    true
                }
                R.id.nav_sair -> {
                    confirmarSaida()
                    true
                }
                else -> false
            }
        }
    }

    private fun confirmarSaida() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialogo_sair_titulo))
            .setMessage(getString(R.string.dialogo_sair_mensagem))
            .setNegativeButton(getString(R.string.dialogo_cancelar), null)
            .setPositiveButton(getString(R.string.dialogo_confirmar)) { _, _ ->
                autenticacao.signOut()
                startActivity(Intent(this, TelaLoginActivity::class.java))
                finish()
            }
            .show()
    }
}
