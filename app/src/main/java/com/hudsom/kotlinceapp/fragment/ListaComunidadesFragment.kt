package com.hudsom.kotlinceapp.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hudsom.kotlinceapp.R
import com.hudsom.kotlinceapp.TelaComunidadeActivity
import com.hudsom.kotlinceapp.adapter.ComunidadeAdapter
import com.hudsom.kotlinceapp.api.RetrofitClient
import com.hudsom.kotlinceapp.dados.BancoLocal
import com.hudsom.kotlinceapp.dados.ComunidadeDao
import com.hudsom.kotlinceapp.model.Comunidade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListaComunidadesFragment : Fragment() {

    private lateinit var rvComunidades: RecyclerView
    private lateinit var tvVazio: TextView
    private lateinit var fabAdicionar: FloatingActionButton
    private lateinit var adaptador: ComunidadeAdapter
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
        dao = BancoLocal.obterInstancia(requireContext()).comunidadeDao()

        configurarLista()
        carregarLocal()
        carregarViaApi()

        fabAdicionar.setOnClickListener {
            startActivity(Intent(requireContext(), TelaComunidadeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        carregarLocal()
        carregarViaApi()
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

    private fun carregarViaApi() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instancia.listarComunidades()
                }
                if (response.isSuccessful) {
                    val lista = response.body()?.values?.toList() ?: emptyList()
                    withContext(Dispatchers.IO) {
                        dao.limparTodas()
                        dao.salvarTodas(lista)
                    }
                    atualizarUI(lista)
                }
            } catch (e: Exception) {
                Log.e("ListaComunidades", "Erro ao carregar via API REST", e)
            }
        }
    }

    private fun atualizarUI(lista: List<Comunidade>) {
        adaptador.atualizarLista(lista)
        tvVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }
}
