package com.hudsom.kotlinceapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import com.hudsom.kotlinceapp.databinding.TelaCadastroBinding
import com.hudsom.kotlinceapp.fragment.BotaoFragment
import com.hudsom.kotlinceapp.fragment.InputEmailFragment
import com.hudsom.kotlinceapp.fragment.InputSenhaFragment
import com.hudsom.kotlinceapp.model.PerfilUsuario
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class TelaCadastroActivity : AppCompatActivity() {

    private lateinit var binding: TelaCadastroBinding
    private lateinit var autenticacao: FirebaseAuth
    private lateinit var analytics: FirebaseAnalytics

    private lateinit var fragmentEmail: InputEmailFragment
    private lateinit var fragmentSenha: InputSenhaFragment
    private lateinit var fragmentBtnCadastrar: BotaoFragment

    private var fotoUri: Uri? = null
    private var cameraUri: Uri? = null

    private val selecionarGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoUri = uri
            binding.ivFotoCapa.setImageURI(uri)
            binding.ivFotoCapa.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private val tirarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        if (sucesso && cameraUri != null) {
            fotoUri = cameraUri
            binding.ivFotoCapa.setImageURI(cameraUri)
            binding.ivFotoCapa.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

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
        analytics = FirebaseAnalytics.getInstance(this)

        fragmentEmail = supportFragmentManager.findFragmentById(R.id.fragmentEmail) as InputEmailFragment
        fragmentSenha = supportFragmentManager.findFragmentById(R.id.fragmentSenha) as InputSenhaFragment
        fragmentBtnCadastrar = supportFragmentManager.findFragmentById(R.id.fragmentBtnCadastrar) as BotaoFragment

        fragmentBtnCadastrar.definirTexto(getString(R.string.cadastro_botao))
        fragmentBtnCadastrar.definirClique { realizarCadastro() }

        binding.btnSelecionarFoto.setOnClickListener { mostrarDialogoFoto() }
        binding.ivFotoCapa.setOnClickListener { mostrarDialogoFoto() }

        binding.btnGoogle.setOnClickListener { cadastrarComGoogle() }
        binding.btnIrLogin.setOnClickListener {
            startActivity(Intent(this, TelaLoginActivity::class.java))
            finish()
        }
    }

    private fun mostrarDialogoFoto() {
        val opcoes = arrayOf(
            getString(R.string.cadastro_foto_camera),
            getString(R.string.cadastro_foto_galeria)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cadastro_foto_titulo))
            .setItems(opcoes) { _, qual ->
                when (qual) {
                    0 -> abrirCamera()
                    1 -> selecionarGaleria.launch("image/*")
                }
            }
            .show()
    }

    private fun abrirCamera() {
        val dir = File(cacheDir, "fotos").apply { mkdirs() }
        val arquivo = File(dir, "foto_${System.currentTimeMillis()}.jpg")
        cameraUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", arquivo)
        tirarFoto.launch(cameraUri!!)
    }

    private fun converterImagemParaBase64(uri: Uri): String {
        val source = ImageDecoder.createSource(contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val redimensionado = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
        val stream = ByteArrayOutputStream()
        redimensionado.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun limparErros() {
        binding.tilNome.error = null
        fragmentEmail.limparErro()
        fragmentSenha.limparErro()
    }

    private fun validarCampos(nome: String, email: String, senha: String): Boolean {
        limparErros()
        var valido = true

        if (fotoUri == null) {
            Toast.makeText(this, getString(R.string.erro_foto_obrigatoria), Toast.LENGTH_SHORT).show()
            valido = false
        }

        if (nome.isEmpty()) {
            binding.tilNome.error = getString(R.string.erro_nome_obrigatorio)
            valido = false
        } else if (nome.length < 3) {
            binding.tilNome.error = getString(R.string.erro_nome_curto)
            valido = false
        }

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

    private fun realizarCadastro() {
        val nome = binding.etNome.text.toString().trim()
        val email = fragmentEmail.obterTexto()
        val senha = fragmentSenha.obterTexto()

        if (!validarCampos(nome, email, senha)) return

        val fotoBase64 = try {
            converterImagemParaBase64(fotoUri!!)
        } catch (e: Exception) {
            Log.e("TelaCadastro", "Erro ao converter imagem", e)
            ""
        }

        autenticacao.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user!!.uid
                val perfil = PerfilUsuario(uid = uid, nome = nome, email = email, fotoCapa = fotoBase64)
                FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).reference
                    .child("usuarios").child(uid).setValue(perfil)

                analytics.logEvent("cadastro_email", null)
                autenticacao.signOut()
                Toast.makeText(this, getString(R.string.sucesso_cadastro), Toast.LENGTH_LONG).show()
                startActivity(Intent(this, TelaLoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { erro ->
                limparErros()
                when (erro) {
                    is FirebaseAuthUserCollisionException ->
                        fragmentEmail.definirErro(getString(R.string.erro_email_ja_cadastrado))
                    is FirebaseAuthWeakPasswordException ->
                        fragmentSenha.definirErro(getString(R.string.erro_senha_curta))
                    is FirebaseAuthInvalidCredentialsException ->
                        fragmentEmail.definirErro(getString(R.string.erro_email_invalido))
                    else ->
                        fragmentEmail.definirErro(getString(R.string.erro_conexao))
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
