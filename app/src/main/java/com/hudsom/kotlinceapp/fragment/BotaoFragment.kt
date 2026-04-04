package com.hudsom.kotlinceapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.hudsom.kotlinceapp.R

class BotaoFragment : Fragment() {

    private var botao: MaterialButton? = null
    private var textoPendente: String? = null
    private var listener: (() -> Unit)? = null

    companion object {
        private const val ARG_TEXTO = "texto"

        fun novaInstancia(texto: String): BotaoFragment {
            val fragment = BotaoFragment()
            fragment.arguments = Bundle().apply { putString(ARG_TEXTO, texto) }
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_botao, container, false)
        botao = view.findViewById(R.id.btnFragmento)
        botao?.text = textoPendente ?: arguments?.getString(ARG_TEXTO) ?: ""
        listener?.let { acao -> botao?.setOnClickListener { acao() } }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        botao = null
    }

    fun definirTexto(texto: String) {
        if (botao != null) botao?.text = texto else textoPendente = texto
    }

    fun definirClique(acao: () -> Unit) {
        listener = acao
        botao?.setOnClickListener { acao() }
    }
}
