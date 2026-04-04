package com.hudsom.kotlinceapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hudsom.kotlinceapp.R

class InputEmailFragment : Fragment() {

    private var tilEmail: TextInputLayout? = null
    private var etEmail: TextInputEditText? = null
    private var textoPendente: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_input_email, container, false)
        tilEmail = view.findViewById(R.id.tilEmail)
        etEmail = view.findViewById(R.id.etEmail)
        textoPendente?.let { etEmail?.setText(it); textoPendente = null }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tilEmail = null
        etEmail = null
    }

    fun obterTexto(): String = etEmail?.text.toString().trim()

    fun definirTexto(texto: String) {
        if (etEmail != null) etEmail?.setText(texto) else textoPendente = texto
    }

    fun definirErro(mensagem: String?) { tilEmail?.error = mensagem }

    fun limparErro() { tilEmail?.error = null }
}
