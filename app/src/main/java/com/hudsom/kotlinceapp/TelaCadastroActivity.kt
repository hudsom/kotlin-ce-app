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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.hudsom.kotlinceapp.databinding.TelaCadastroBinding
import com.hudsom.kotlinceapp.model.PerfilUsuario
import kotlinx.coroutines.launch

class TelaCadastroActivity : AppCompatActivity() {

    private lateinit var binding: TelaCadastroBinding
    private lateinit var autenticacao: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = TelaCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.principal) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        autenticacao = FirebaseAuth.getInstance()

        binding.btnCadastrar.setOnClickListener { realizarCadastro() }
        binding.btnGoogle.setOnClickListener { cadastrarComGoogle() }
        binding.btnIrLogin.setOnClickListener {
            startActivity(Intent(this, TelaLoginActivity::class.java))
            finish()
        }
    }

    private fun limparErros() {
        binding.tilNome.error = null
        binding.tilEmail.error = null
        binding.tilSenha.error = null
    }

    private fun validarCampos(nome: String, email: String, senha: String): Boolean {
        limparErros()
        var valido = true

        if (nome.isEmpty()) {
            binding.tilNome.error = getString(R.string.erro_nome_obrigatorio)
            valido = false
        } else if (nome.length < 3) {
            binding.tilNome.error = getString(R.string.erro_nome_curto)
            valido = false
        }

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

    private fun realizarCadastro() {
        val nome = binding.etNome.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etSenha.text.toString().trim()

        if (!validarCampos(nome, email, senha)) return

        autenticacao.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user!!.uid
                val perfil = PerfilUsuario(uid = uid, nome = nome, email = email)
                FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).reference
                    .child("usuarios").child(uid)
                    .setValue(perfil)

                autenticacao.signOut()
                Toast.makeText(this, getString(R.string.sucesso_cadastro), Toast.LENGTH_LONG).show()
                startActivity(Intent(this, TelaLoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { erro ->
                limparErros()
                when (erro) {
                    is FirebaseAuthUserCollisionException ->
                        binding.tilEmail.error = getString(R.string.erro_email_ja_cadastrado)
                    is FirebaseAuthWeakPasswordException ->
                        binding.tilSenha.error = getString(R.string.erro_senha_curta)
                    is FirebaseAuthInvalidCredentialsException ->
                        binding.tilEmail.error = getString(R.string.erro_email_invalido)
                    else ->
                        binding.tilEmail.error = getString(R.string.erro_conexao)
                }
            }
    }

    private fun cadastrarComGoogle() {
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
                val result = credentialManager.getCredential(this@TelaCadastroActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleToken = GoogleIdTokenCredential.createFrom(credential.data)
                    autenticarComFirebase(googleToken.idToken)
                }
            } catch (e: Exception) {
                Log.e("TelaCadastro", "Erro Google Sign-In", e)
                Toast.makeText(this@TelaCadastroActivity, getString(R.string.erro_conexao), Toast.LENGTH_SHORT).show()
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

                startActivity(Intent(this, TelaInicialActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.erro_conexao), Toast.LENGTH_SHORT).show()
            }
    }
}
