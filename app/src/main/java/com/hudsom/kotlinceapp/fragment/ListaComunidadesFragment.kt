package com.hudsom.kotlinceapp.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.hudsom.kotlinceapp.BuildConfig
import com.hudsom.kotlinceapp.R
import com.hudsom.kotlinceapp.TelaComunidadeActivity
import com.hudsom.kotlinceapp.adapter.ComunidadeAdapter
import com.hudsom.kotlinceapp.dados.BancoLocal
import com.hudsom.kotlinceapp.dados.ComunidadeDao
import com.hudsom.kotlinceapp.model.Comunidade

class ListaComunidadesFragment : Fragment() {

    private lateinit var rvComunidades: RecyclerView
    private lateinit var tvVazio: TextView
    private lateinit var fabAdicionar: FloatingActionButton
    private lateinit var adaptador: ComunidadeAdapter
    private lateinit var bancoDados: DatabaseReference
    private lateinit var dao: ComunidadeDao

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_lista_comunidades, container, false)
        rvComunidades = view.findViewById(R.id.rvComunidades)
        tvVazio = view.findViewById(R.id.tvVazio)
        fabAdicionar = view.findViewById(R.id.fabAdicionar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bancoDados = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).reference
        dao = BancoLocal.obterInstancia(requireContext()).comunidadeDao()

        configurarLista()
        carregarLocal()
        sincronizarComFirebase()

        fabAdicionar.setOnClickListener {
            startActivity(Intent(requireContext(), TelaComunidadeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        carregarLocal()
        sincronizarComFirebase()
    }

    private fun configurarLista() {
        adaptador = ComunidadeAdapter(
            aoClicar = { comunidade ->
                val intent = Intent(requireContext(), TelaComunidadeActivity::class.java)
                intent.putExtra("comunidade_id", comunidade.id)
                startActivity(intent)
            }
        )
        rvComunidades.layoutManager = LinearLayoutManager(requireContext())
        rvComunidades.adapter = adaptador
    }

    private fun carregarLocal() {
        Thread {
            val lista = dao.listarTodas()
            activity?.runOnUiThread { atualizarUI(lista) }
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
                        activity?.runOnUiThread { atualizarUI(lista) }
                    }.start()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun atualizarUI(lista: List<Comunidade>) {
        adaptador.atualizarLista(lista)
        tvVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }
}
