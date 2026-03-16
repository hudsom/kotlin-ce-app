package com.hudsom.kotlinceapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.hudsom.kotlinceapp.adapter.ComunidadeAdapter
import com.hudsom.kotlinceapp.dados.BancoLocal
import com.hudsom.kotlinceapp.databinding.TelaInicialBinding
import com.hudsom.kotlinceapp.model.Comunidade

class TelaInicialActivity : AppCompatActivity() {

    private lateinit var binding: TelaInicialBinding
    private lateinit var autenticacao: FirebaseAuth
    private lateinit var bancoDados: DatabaseReference
    private lateinit var adaptador: ComunidadeAdapter
    private lateinit var dao: com.hudsom.kotlinceapp.dados.ComunidadeDao

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
        dao = BancoLocal.obterInstancia(this).comunidadeDao()

        configurarDrawer()
        configurarListaComunidades()
        carregarLocal()
        sincronizarComFirebase()

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

        binding.fabAdicionar.setOnClickListener {
            startActivity(Intent(this, TelaComunidadeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        carregarLocal()
        sincronizarComFirebase()
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

    private fun configurarListaComunidades() {
        adaptador = ComunidadeAdapter(
            aoClicar = { comunidade ->
                val intent = Intent(this, TelaComunidadeActivity::class.java)
                intent.putExtra("comunidade_id", comunidade.id)
                startActivity(intent)
            }
        )
        binding.rvComunidades.layoutManager = LinearLayoutManager(this)
        binding.rvComunidades.adapter = adaptador
    }

    private fun carregarLocal() {
        Thread {
            val lista = dao.listarTodas()
            runOnUiThread { atualizarUI(lista) }
        }.start()
    }

    private fun sincronizarComFirebase() {
        bancoDados.child("comunidades")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lista = snapshot.children.mapNotNull { it.getValue(Comunidade::class.java) }
                    Thread {
                        dao.limparTodas()
                        dao.salvarTodas(lista)
                        runOnUiThread { atualizarUI(lista) }
                    }.start()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun atualizarUI(lista: List<Comunidade>) {
        adaptador.atualizarLista(lista)
        binding.tvVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
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
