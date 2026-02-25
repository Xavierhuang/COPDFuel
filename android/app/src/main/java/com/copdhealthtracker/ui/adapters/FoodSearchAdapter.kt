package com.copdhealthtracker.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.copdhealthtracker.R
import com.copdhealthtracker.data.model.FoodSearchResult

class FoodSearchAdapter(
    private val onItemClick: (FoodSearchResult) -> Unit
) : RecyclerView.Adapter<FoodSearchAdapter.ViewHolder>() {

    private var items: List<FoodSearchResult> = emptyList()

    fun submitList(newItems: List<FoodSearchResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodName: TextView = itemView.findViewById(R.id.food_name)
        private val foodBrand: TextView = itemView.findViewById(R.id.food_brand)
        private val foodNutrients: TextView = itemView.findViewById(R.id.food_nutrients)

        fun bind(item: FoodSearchResult) {
            foodName.text = item.description
            
            if (!item.brandOwner.isNullOrBlank()) {
                foodBrand.visibility = View.VISIBLE
                foodBrand.text = item.brandOwner
            } else {
                foodBrand.visibility = View.GONE
            }

            val nutrientText = buildString {
                append("${item.calories.toInt()} cal")
                append(" | P: ${String.format("%.1f", item.protein)}g")
                append(" | C: ${String.format("%.1f", item.carbs)}g")
                append(" | F: ${String.format("%.1f", item.fat)}g")
            }
            foodNutrients.text = nutrientText

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
