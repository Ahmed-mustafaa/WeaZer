package com.example.weatherapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R

class SuggestionAdapter(
    private var suggestions: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

  inner  class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val suggestionTextView: TextView = itemView.findViewById(R.id.suggestion_text)

            fun bind(suggestion: String) {
                suggestionTextView.text = suggestion
                itemView.setOnClickListener {
                    onClick(suggestion)
                    Log.d("SuggestionAdapter", "Suggestion clicked: $suggestion")
                }
            }
        }
    fun updateData(newSuggestions: List<String>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
      holder.bind(suggestions[position])

    }

    override fun getItemCount(): Int  = suggestions.size


}