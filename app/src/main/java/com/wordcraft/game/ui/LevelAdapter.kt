package com.wordcraft.game.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wordcraft.game.data.model.Level
import com.wordcraft.game.databinding.ItemLevelCardBinding

class LevelAdapter(
    private var levels: List<Level>,
    private var highestUnlocked: Int,
    private val onLevelClick: (Int) -> Unit
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    fun updateData(newLevels: List<Level>, newHighestUnlocked: Int) {
        levels = newLevels
        highestUnlocked = newHighestUnlocked
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val binding = ItemLevelCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LevelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        holder.bind(levels[position])
    }

    override fun getItemCount(): Int = levels.size

    inner class LevelViewHolder(private val binding: ItemLevelCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(level: Level) {
            val isUnlocked = level.id <= highestUnlocked
            val isCompleted = level.id < highestUnlocked

            if (isUnlocked) {
                binding.tvCardLevelNum.visibility = View.VISIBLE
                binding.tvCardLevelTitle.visibility = View.VISIBLE
                binding.layoutStars.visibility = View.VISIBLE
                binding.ivLock.visibility = View.GONE
                binding.tvLockedText.visibility = View.GONE

                binding.tvCardLevelNum.text = level.id.toString()
                binding.tvCardLevelTitle.text = level.title

                val starColor = if (isCompleted) Color.parseColor("#FFD166") else Color.parseColor("#44FFFFFF")
                binding.ivStar1.setColorFilter(starColor)
                binding.ivStar2.setColorFilter(starColor)
                binding.ivStar3.setColorFilter(starColor)

                binding.cardLevel.setCardBackgroundColor(
                    if (isCompleted) Color.parseColor("#1E3A2F") else Color.parseColor("#1B263B")
                )
                binding.cardLevel.strokeColor =
                    if (isCompleted) Color.parseColor("#FFB703") else Color.parseColor("#44FFFFFF")

                binding.root.setOnClickListener {
                    onLevelClick(level.id)
                }
            } else {
                binding.tvCardLevelNum.visibility = View.GONE
                binding.tvCardLevelTitle.visibility = View.GONE
                binding.layoutStars.visibility = View.GONE
                binding.ivLock.visibility = View.VISIBLE
                binding.tvLockedText.visibility = View.VISIBLE

                binding.cardLevel.setCardBackgroundColor(Color.parseColor("#151D28"))
                binding.cardLevel.strokeColor = Color.parseColor("#22FFFFFF")
                binding.root.setOnClickListener(null)
            }
        }
    }
}
