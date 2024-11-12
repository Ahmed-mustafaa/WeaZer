package com.example.weatherapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R

class SuggestionAdapter(
    private var suggestions: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val suggestionText: TextView = itemView.findViewById(R.id.suggestion_text)
    }
    fun updateData(newSuggestions: List<String>) {
        this.suggestions = newSuggestions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.suggestionText.text = suggestion
        holder.itemView.setOnClickListener { onItemClick(suggestion) }
    }

    override fun getItemCount() = suggestions.size
}