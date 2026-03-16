package com.hudsom.kotlinceapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.hudsom.kotlinceapp.dados.BancoLocal
import com.hudsom.kotlinceapp.databinding.TelaComunidadeBinding
import com.hudsom.kotlinceapp.model.Comunidade

class TelaComunidadeActivity : AppCompatActivity() {

    private lateinit var binding: TelaComunidadeBinding
    private lateinit var bancoDados: DatabaseReference
    private lateinit var dao: com.hudsom.kotlinceapp.dados.ComunidadeDao
    private var comunidadeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = TelaComunidadeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.principal) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        bancoDados = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).reference.child("comunidades")
        dao = BancoLocal.obterInstancia(this).comunidadeDao()
        comunidadeId = intent.getStringExtra("comunidade_id")

        binding.barraFerramentas.setNavigationOnClickListener { finish() }

        if (comunidadeId != null) {
            binding.barraFerramentas.title = getString(R.string.comunidade_titulo_editar)
            binding.btnExcluir.visibility = View.VISIBLE
            carregarComunidade()
        }

        binding.btnSalvar.setOnClickListener { salvarComunidade() }
        binding.btnExcluir.setOnClickListener { confirmarExclusao() }
    }

    private fun carregarComunidade() {
        Thread {
            val local = dao.buscarPorId(comunidadeId!!)
            if (local != null) {
                runOnUiThread {
                    binding.etNomeComunidade.setText(local.nome)
                    binding.etLiderComunidade.setText(local.lider)
                    binding.etDescricaoComunidade.setText(local.descricao)
                }
            }
        }.start()

        bancoDados.child(comunidadeId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comunidade = snapshot.getValue(Comunidade::class.java) ?: return
                binding.etNomeComunidade.setText(comunidade.nome)
                binding.etLiderComunidade.setText(comunidade.lider)
                binding.etDescricaoComunidade.setText(comunidade.descricao)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun salvarComunidade() {
        val nome = binding.etNomeComunidade.text.toString().trim()
        val lider = binding.etLiderComunidade.text.toString().trim()
        val descricao = binding.etDescricaoComunidade.text.toString().trim()

        binding.tilNomeComunidade.error = null
        if (nome.isEmpty()) {
            binding.tilNomeComunidade.error = getString(R.string.erro_campos_vazios)
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val id = comunidadeId ?: bancoDados.push().key!!
        val comunidade = Comunidade(id = id, nome = nome, lider = lider, descricao = descricao, ownerId = uid)

        // Salva localmente primeiro
        Thread { dao.salvar(comunidade) }.start()

        // Sincroniza com Firebase
        bancoDados.child(id).setValue(comunidade)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.sucesso_comunidade_salva), Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { erro ->
                Log.e("TelaComunidade", "Erro ao salvar no Firebase", erro)
                Toast.makeText(this, getString(R.string.sucesso_comunidade_salva), Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun confirmarExclusao() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialogo_excluir_titulo))
            .setMessage(getString(R.string.dialogo_excluir_mensagem))
            .setNegativeButton(getString(R.string.dialogo_cancelar), null)
            .setPositiveButton(getString(R.string.dialogo_confirmar)) { _, _ ->
                Thread { dao.excluir(comunidadeId!!) }.start()
                bancoDados.child(comunidadeId!!).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.sucesso_comunidade_excluida), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, getString(R.string.sucesso_comunidade_excluida), Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .show()
    }
}
