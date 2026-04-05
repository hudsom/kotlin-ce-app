package com.hudsom.kotlinceapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import com.hudsom.kotlinceapp.databinding.TelaLoginBinding
import com.hudsom.kotlinceapp.fragment.BotaoFragment
import com.hudsom.kotlinceapp.fragment.InputEmailFragment
import com.hudsom.kotlinceapp.fragment.InputSenhaFragment
import com.hudsom.kotlinceapp.model.PerfilUsuario
import kotlinx.coroutines.launch

class TelaLoginActivity : AppCompatActivity() {

    private lateinit var binding: TelaLoginBinding
    private lateinit var autenticacao: FirebaseAuth
    private lateinit var analytics: FirebaseAnalytics
    private val prefs by lazy { getSharedPreferences("login_prefs", MODE_PRIVATE) }

    private lateinit var fragmentEmail: InputEmailFragment
    private lateinit var fragmentSenha: InputSenhaFragment
    private lateinit var fragmentBtnEntrar: BotaoFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = TelaLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.principal) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        autenticacao = FirebaseAuth.getInstance()
        analytics = FirebaseAnalytics.getInstance(this)

        fragmentEmail = supportFragmentManager.findFragmentById(R.id.fragmentEmail) as InputEmailFragment
        fragmentSenha = supportFragmentManager.findFragmentById(R.id.fragmentSenha) as InputSenhaFragment
        fragmentBtnEntrar = supportFragmentManager.findFragmentById(R.id.fragmentBtnEntrar) as BotaoFragment

        fragmentBtnEntrar.definirTexto(getString(R.string.login_botao))
        fragmentBtnEntrar.definirClique { realizarLogin() }

        carregarCredenciais()

        binding.btnGoogle.setOnClickListener { loginComGoogle() }
        binding.btnEsqueciSenha.setOnClickListener { mostrarDialogoEsqueciSenha() }
        binding.btnIrCadastro.setOnClickListener {
            startActivity(Intent(this, TelaCadastroActivity::class.java))
        }
    }

    private fun carregarCredenciais() {
        if (prefs.getBoolean("lembrar", false)) {
            fragmentEmail.definirTexto(prefs.getString("email", "") ?: "")
            fragmentSenha.definirTexto(prefs.getString("senha", "") ?: "")
            binding.cbLembrar.isChecked = true
        }
    }

    private fun salvarCredenciais(email: String, senha: String) {
        prefs.edit()
            .putBoolean("lembrar", true)
            .putString("email", email)
            .putString("senha", senha)
            .apply()
    }

    private fun limparCredenciais() {
        prefs.edit().clear().apply()
    }

    private fun limparErros() {
        fragmentEmail.limparErro()
        fragmentSenha.limparErro()
    }

    private fun validarCampos(email: String, senha: String): Boolean {
        limparErros()
        var valido = true

        if (email.isEmpty()) {
            fragmentEmail.definirErro(getString(R.string.erro_email_obrigatorio))
            valido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            fragmentEmail.definirErro(getString(R.string.erro_email_invalido))
            valido = false
        }

        if (senha.isEmpty()) {
            fragmentSenha.definirErro(getString(R.string.erro_senha_obrigatoria))
            valido = false
        } else if (senha.length < 6) {
            fragmentSenha.definirErro(getString(R.string.erro_senha_curta))
            valido = false
        }

        return valido
    }

    private fun realizarLogin() {
        val email = fragmentEmail.obterTexto()
        val senha = fragmentSenha.obterTexto()

        if (!validarCampos(email, senha)) return

        autenticacao.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                analytics.logEvent("login_email", null)
                if (binding.cbLembrar.isChecked) {
                    salvarCredenciais(email, senha)
                } else {
                    limparCredenciais()
                }
                irParaInicial()
            }
            .addOnFailureListener { erro ->
                limparErros()
                when (erro) {
                    is FirebaseAuthInvalidUserException ->
                        fragmentEmail.definirErro(getString(R.string.erro_login_falhou))
                    is FirebaseAuthInvalidCredentialsException ->
                        fragmentSenha.definirErro(getString(R.string.erro_login_falhou))
                    else ->
                        fragmentEmail.definirErro(getString(R.string.erro_conexao))
                }
            }
    }

    private fun loginComGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(this)

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@TelaLoginActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleToken = GoogleIdTokenCredential.createFrom(credential.data)
                    autenticarComFirebase(googleToken.idToken)
                }
            } catch (e: Exception) {
                Log.e("TelaLogin", "Erro Google Sign-In", e)
                Toast.makeText(this@TelaLoginActivity, getString(R.string.erro_conexao), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun autenticarComFirebase(idToken: String) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        autenticacao.signInWithCredential(credencial)
            .addOnSuccessListener { resultado ->
                val user = resultado.user ?: return@addOnSuccessListener
                val perfil = PerfilUsuario(uid = user.uid, nome = user.displayName ?: "", email = user.email ?: "")
                FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).reference
                    .child("usuarios").child(user.uid).setValue(perfil)
                analytics.logEvent("login_google", null)
                irParaInicial()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.erro_conexao), Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoEsqueciSenha() {
        val layout = layoutInflater.inflate(R.layout.fragment_input_email, null)
        val tilEmail = layout.findViewById<TextInputLayout>(R.id.tilEmail)
        val etEmail = layout.findViewById<TextInputEditText>(R.id.etEmail)

        val emailAtual = fragmentEmail.obterTexto()
        if (emailAtual.isNotEmpty()) etEmail.setText(emailAtual)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.esqueci_senha_titulo))
            .setMessage(getString(R.string.esqueci_senha_mensagem))
            .setView(layout)
            .setNegativeButton(getString(R.string.dialogo_cancelar), null)
            .setPositiveButton(getString(R.string.esqueci_senha_enviar)) { _, _ ->
                val email = etEmail.text.toString().trim()
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, getString(R.string.erro_email_invalido), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                autenticacao.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.sucesso_email_enviado), Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, getString(R.string.erro_conexao), Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
    }

    private fun irParaInicial() {
        startActivity(Intent(this, TelaInicialActivity::class.java))
        finish()
    }
}
