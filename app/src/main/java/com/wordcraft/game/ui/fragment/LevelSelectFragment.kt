package com.wordcraft.game.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.wordcraft.game.R
import com.wordcraft.game.databinding.FragmentLevelSelectBinding
import com.wordcraft.game.ui.LevelAdapter
import com.wordcraft.game.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class LevelSelectFragment : Fragment() {

    private val viewModel: GameViewModel by activityViewModels()
    private var _binding: FragmentLevelSelectBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LevelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackLevelSelect.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = LevelAdapter(
            levels = emptyList(),
            highestUnlocked = 1,
            onLevelClick = { levelId ->
                viewModel.selectLevel(levelId)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.fragment_container, GameFragment())
                    .addToBackStack("game")
                    .commit()
            }
        )

        binding.rvLevels.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvLevels.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.updateData(state.allLevels, state.highestUnlockedLevel)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
