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

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_input_email, container, false)
        tilEmail = view.findViewById(R.id.tilEmail)
        etEmail = view.findViewById(R.id.etEmail)
        return view
    }

    fun obterTexto(): String = etEmail.text.toString().trim()

    fun definirTexto(texto: String) { etEmail.setText(texto) }

    fun definirErro(mensagem: String?) { tilEmail.error = mensagem }

    fun limparErro() { tilEmail.error = null }
}
