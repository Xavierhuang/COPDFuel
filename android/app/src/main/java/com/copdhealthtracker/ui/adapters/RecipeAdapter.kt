package com.copdhealthtracker.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.copdhealthtracker.R
import com.copdhealthtracker.ui.models.Recipe

class RecipeAdapter(private val recipes: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }
    
    override fun getItemCount() = recipes.size
    
    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImage: ImageView = itemView.findViewById(R.id.recipe_icon)
        private val nameText: TextView = itemView.findViewById(R.id.recipe_name)
        private val descriptionText: TextView = itemView.findViewById(R.id.recipe_description)
        private val tagsContainer: ViewGroup = itemView.findViewById(R.id.recipe_tags_container)
        
        fun bind(recipe: Recipe) {
            val drawable = ContextCompat.getDrawable(itemView.context, recipe.iconRes)
            if (drawable != null) {
                val wrappedDrawable = DrawableCompat.wrap(drawable.mutate())
                DrawableCompat.setTint(wrappedDrawable, recipe.iconTint)
                iconImage.setImageDrawable(wrappedDrawable)
            }
            nameText.text = recipe.name
            descriptionText.text = recipe.description
            
            tagsContainer.removeAllViews()
            recipe.tags.forEach { tag ->
                val tagView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_recipe_tag, tagsContainer, false)
                val tagText = tagView.findViewById<TextView>(R.id.tag_text)
                tagText.text = tag
                tagsContainer.addView(tagView)
            }
        }
    }
}
