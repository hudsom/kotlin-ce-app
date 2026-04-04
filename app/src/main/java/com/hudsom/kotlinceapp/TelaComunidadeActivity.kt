package com.hudsom.kotlinceapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.hudsom.kotlinceapp.dados.BancoLocal
import com.hudsom.kotlinceapp.databinding.TelaComunidadeBinding
import com.hudsom.kotlinceapp.model.Comunidade
import java.io.ByteArrayOutputStream
import java.io.File

class TelaComunidadeActivity : AppCompatActivity() {

    private lateinit var binding: TelaComunidadeBinding
    private lateinit var bancoDados: DatabaseReference
    private lateinit var dao: com.hudsom.kotlinceapp.dados.ComunidadeDao
    private var comunidadeId: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var imagemBase64: String = ""
    private var endereco: String = ""
    private var fotoUri: Uri? = null
    private var cameraUri: Uri? = null

    private val pedirPermissaoLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) obterLocalizacao()
        else Toast.makeText(this, getString(R.string.erro_localizacao_permissao), Toast.LENGTH_SHORT).show()
    }

    private val selecionarGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoUri = uri
            imagemBase64 = converterImagemParaBase64(uri)
            binding.ivImagemComunidade.setImageURI(uri)
            binding.ivImagemComunidade.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private val tirarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        if (sucesso && cameraUri != null) {
            fotoUri = cameraUri
            imagemBase64 = converterImagemParaBase64(cameraUri!!)
            binding.ivImagemComunidade.setImageURI(cameraUri)
            binding.ivImagemComunidade.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = TelaComunidadeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.principal) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        bancoDados = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).reference.child("comunidades")
        dao = BancoLocal.obterInstancia(this).comunidadeDao()
        comunidadeId = intent.getStringExtra("comunidade_id")

        binding.barraFerramentas.setNavigationOnClickListener { finish() }

        if (comunidadeId != null) {
            binding.barraFerramentas.title = getString(R.string.comunidade_titulo_editar)
            binding.btnExcluir.visibility = View.VISIBLE
            carregarComunidade()
        }

        binding.btnSalvar.setOnClickListener { salvarComunidade() }
        binding.btnExcluir.setOnClickListener { confirmarExclusao() }
        binding.btnLocalizacao.setOnClickListener { verificarPermissaoLocalizacao() }
        binding.btnSelecionarFoto.setOnClickListener { mostrarDialogoFoto() }
        binding.ivImagemComunidade.setOnClickListener { mostrarDialogoFoto() }
    }

    private fun mostrarDialogoFoto() {
        val opcoes = arrayOf(
            getString(R.string.comunidade_foto_camera),
            getString(R.string.comunidade_foto_galeria)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.comunidade_foto_titulo))
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
        val arquivo = File(dir, "comunidade_${System.currentTimeMillis()}.jpg")
        cameraUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", arquivo)
        tirarFoto.launch(cameraUri!!)
    }

    private fun converterImagemParaBase64(uri: Uri): String {
        val source = ImageDecoder.createSource(contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val redimensionado = Bitmap.createScaledBitmap(bitmap, 400, 300, true)
        val stream = ByteArrayOutputStream()
        redimensionado.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun verificarPermissaoLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacao()
        } else {
            pedirPermissaoLocalizacao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun obterLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    endereco = obterEndereco(latitude, longitude)
                    binding.tvLocalizacao.text = getString(R.string.comunidade_localizacao, endereco, latitude, longitude)
                } else {
                    Toast.makeText(this, getString(R.string.erro_localizacao_falhou), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.erro_localizacao_falhou), Toast.LENGTH_SHORT).show()
            }
    }

    private fun obterEndereco(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this)
            val resultados = geocoder.getFromLocation(lat, lng, 1)
            if (!resultados.isNullOrEmpty()) {
                val local = resultados[0]
                listOfNotNull(local.subAdminArea ?: local.locality, local.adminArea, local.countryName)
                    .joinToString(", ")
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun carregarComunidade() {
        Thread {
            val local = dao.buscarPorId(comunidadeId!!)
            if (local != null) {
                runOnUiThread { preencherCampos(local) }
            }
        }.start()

        bancoDados.child(comunidadeId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comunidade = snapshot.getValue(Comunidade::class.java) ?: return
                preencherCampos(comunidade)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun preencherCampos(comunidade: Comunidade) {
        binding.etNomeComunidade.setText(comunidade.nome)
        binding.etLiderComunidade.setText(comunidade.lider)
        binding.etDescricaoComunidade.setText(comunidade.descricao)
        latitude = comunidade.latitude
        longitude = comunidade.longitude
        if (latitude != 0.0 || longitude != 0.0) {
            if (comunidade.endereco.isNotEmpty()) {
                endereco = comunidade.endereco
            } else {
                endereco = obterEndereco(latitude, longitude)
            }
            binding.tvLocalizacao.text = getString(R.string.comunidade_localizacao, endereco, latitude, longitude)
        }
        if (comunidade.imagem.isNotEmpty()) {
            imagemBase64 = comunidade.imagem
            val bytes = Base64.decode(comunidade.imagem, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.ivImagemComunidade.setImageBitmap(bitmap)
            binding.ivImagemComunidade.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private fun salvarComunidade() {
        val nome = binding.etNomeComunidade.text.toString().trim()
        val lider = binding.etLiderComunidade.text.toString().trim()
        val descricao = binding.etDescricaoComunidade.text.toString().trim()

        binding.tilNomeComunidade.error = null
        if (nome.isEmpty()) {
            binding.tilNomeComunidade.error = getString(R.string.erro_campos_vazios)
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val id = comunidadeId ?: bancoDados.push().key!!
        val comunidade = Comunidade(
            id = id, nome = nome, lider = lider, descricao = descricao,
            ownerId = uid, latitude = latitude, longitude = longitude,
            endereco = endereco, imagem = imagemBase64
        )

        Thread { dao.salvar(comunidade) }.start()

        bancoDados.child(id).setValue(comunidade)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.sucesso_comunidade_salva), Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { erro ->
                Log.e("TelaComunidade", "Erro ao salvar no Firebase", erro)
                Toast.makeText(this, getString(R.string.sucesso_comunidade_salva), Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun confirmarExclusao() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialogo_excluir_titulo))
            .setMessage(getString(R.string.dialogo_excluir_mensagem))
            .setNegativeButton(getString(R.string.dialogo_cancelar), null)
            .setPositiveButton(getString(R.string.dialogo_confirmar)) { _, _ ->
                Thread { dao.excluir(comunidadeId!!) }.start()
                bancoDados.child(comunidadeId!!).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.sucesso_comunidade_excluida), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, getString(R.string.sucesso_comunidade_excluida), Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .show()
    }
}
