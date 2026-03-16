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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.hudsom.kotlinceapp.databinding.TelaLoginBinding
import com.hudsom.kotlinceapp.model.PerfilUsuario
import kotlinx.coroutines.launch

class TelaLoginActivity : AppCompatActivity() {

    private lateinit var binding: TelaLoginBinding
    private lateinit var autenticacao: FirebaseAuth
    private val prefs by lazy { getSharedPreferences("login_prefs", MODE_PRIVATE) }

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

        carregarCredenciais()

        binding.btnEntrar.setOnClickListener { realizarLogin() }
        binding.btnGoogle.setOnClickListener { loginComGoogle() }
        binding.btnIrCadastro.setOnClickListener {
            startActivity(Intent(this, TelaCadastroActivity::class.java))
        }
    }

    private fun carregarCredenciais() {
        if (prefs.getBoolean("lembrar", false)) {
            binding.etEmail.setText(prefs.getString("email", ""))
            binding.etSenha.setText(prefs.getString("senha", ""))
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
        binding.tilEmail.error = null
        binding.tilSenha.error = null
    }

    private fun validarCampos(email: String, senha: String): Boolean {
        limparErros()
        var valido = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.erro_email_obrigatorio)
            valido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.erro_email_invalido)
            valido = false
        }

        if (senha.isEmpty()) {
            binding.tilSenha.error = getString(R.string.erro_senha_obrigatoria)
            valido = false
        } else if (senha.length < 6) {
            binding.tilSenha.error = getString(R.string.erro_senha_curta)
            valido = false
        }

        return valido
    }

    private fun realizarLogin() {
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etSenha.text.toString().trim()

        if (!validarCampos(email, senha)) return

        autenticacao.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
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
                        binding.tilEmail.error = getString(R.string.erro_login_falhou)
                    is FirebaseAuthInvalidCredentialsException ->
                        binding.tilSenha.error = getString(R.string.erro_login_falhou)
                    else ->
                        binding.tilEmail.error = getString(R.string.erro_conexao)
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
                irParaInicial()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.erro_conexao), Toast.LENGTH_SHORT).show()
            }
    }

    private fun irParaInicial() {
        startActivity(Intent(this, TelaInicialActivity::class.java))
        finish()
    }
}
