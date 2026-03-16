package com.hudsom.kotlinceapp.adapter

import android.view.LayoutInflater
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
    }

    override fun getItemCount() = itens.size

    fun atualizarLista(novosItens: List<Comunidade>) {
        itens.clear()
        itens.addAll(novosItens)
        notifyDataSetChanged()
    }
}
