package com.example.dailybite.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailybite.databinding.FragmentPostCommentsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostCommentsFragment : Fragment() {

    private var _binding: FragmentPostCommentsBinding? = null
    private val binding get() = _binding!!
    private val vm: PostCommentsViewModel by viewModels()
    private lateinit var state: kotlinx.coroutines.flow.StateFlow<CommentsUiState>
    private val adapter = CommentsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = requireArguments().getString("postId") ?: return

        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = adapter

        state = vm.stream(postId)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            state.collectLatest { s ->
                adapter.submit(s.items)
            }
        }

        binding.btnSend.isEnabled = false
        binding.etComment.doAfterTextChanged {
            binding.btnSend.isEnabled = !it.isNullOrBlank()
        }

        // ui/comments/PostCommentsFragment.kt
        binding.btnSend.setOnClickListener {
            val text = binding.etComment.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener
            binding.btnSend.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val res = vm.send(postId, text)
                binding.btnSend.isEnabled = true
                if (res.isSuccess) {
                    binding.etComment.setText("")
                    binding.rvComments.scrollToPosition(maxOf(0, adapter.itemCount - 1))
                } else {
                    android.widget.Toast.makeText(requireContext(), "שליחת תגובה נכשלה", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}