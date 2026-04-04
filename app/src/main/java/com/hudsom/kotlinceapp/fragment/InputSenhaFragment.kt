package com.hudsom.kotlinceapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hudsom.kotlinceapp.R

class InputSenhaFragment : Fragment() {

    private var tilSenha: TextInputLayout? = null
    private var etSenha: TextInputEditText? = null
    private var textoPendente: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_input_senha, container, false)
        tilSenha = view.findViewById(R.id.tilSenha)
        etSenha = view.findViewById(R.id.etSenha)
        textoPendente?.let { etSenha?.setText(it); textoPendente = null }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tilSenha = null
        etSenha = null
    }

    fun obterTexto(): String = etSenha?.text.toString().trim()

    fun definirTexto(texto: String) {
        if (etSenha != null) etSenha?.setText(texto) else textoPendente = texto
    }

    fun definirErro(mensagem: String?) { tilSenha?.error = mensagem }

    fun limparErro() { tilSenha?.error = null }
}
