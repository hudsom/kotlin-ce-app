package com.hudsom.kotlinceapp.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hudsom.kotlinceapp.databinding.ItemComunidadeBinding
import com.hudsom.kotlinceapp.model.Comunidade

class ComunidadeAdapter(
    private val itens: MutableList<Comunidade> = mutableListOf(),
    private val aoClicar: (Comunidade) -> Unit
) : RecyclerView.Adapter<ComunidadeAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(val binding: ItemComunidadeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemComunidadeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val comunidade = itens[position]
        holder.binding.tvNomeComunidade.text = comunidade.nome
        holder.binding.tvLiderComunidade.text = comunidade.lider
        holder.binding.tvDescricaoComunidade.text = comunidade.descricao
        holder.binding.cardComunidade.setOnClickListener { aoClicar(comunidade) }

        if (comunidade.imagem.isNotEmpty()) {
            val bytes = Base64.decode(comunidade.imagem, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.binding.ivImagemComunidade.setImageBitmap(bitmap)
            holder.binding.ivImagemComunidade.visibility = View.VISIBLE
        } else {
            holder.binding.ivImagemComunidade.visibility = View.GONE
        }

        if (comunidade.latitude != 0.0 || comunidade.longitude != 0.0) {
            val texto = if (comunidade.endereco.isNotEmpty()) {
                comunidade.endereco
            } else {
                String.format("%.4f, %.4f", comunidade.latitude, comunidade.longitude)
            }
            holder.binding.tvLocalizacaoComunidade.text = "📍 $texto"
            holder.binding.tvLocalizacaoComunidade.visibility = View.VISIBLE
        } else {
            holder.binding.tvLocalizacaoComunidade.visibility = View.GONE
        }
    }

    override fun getItemCount() = itens.size

    fun atualizarLista(novosItens: List<Comunidade>) {
        itens.clear()
        itens.addAll(novosItens)
        notifyDataSetChanged()
    }
}
