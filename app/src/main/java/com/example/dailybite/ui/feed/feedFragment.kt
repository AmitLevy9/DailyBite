package com.example.dailybite.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailybite.R
import com.example.dailybite.databinding.FragmentFeedBinding
import com.example.dailybite.data.auth.AuthRepository
import com.example.dailybite.data.post.PostRepository
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val vm: FeedViewModel by viewModels()

    @Inject lateinit var storage: FirebaseStorage
    @Inject lateinit var postsRepo: PostRepository
    @Inject lateinit var authRepo: AuthRepository

    private lateinit var adapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // יוצר אדפטר שמעביר אירוע לייק חזרה לפרגמנט
        adapter = PostAdapter(storage) { postId ->
            val uid = authRepo.currentUidOrNull() ?: return@PostAdapter
            viewLifecycleOwner.lifecycleScope.launch {
                postsRepo.like(postId, uid)
                // אין צורך לרענן - feedFlow יעדכן את הרשימה אוטומטית
            }
        }

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        binding.fabNewPost.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_newPost)
        }

        binding.btnMyPosts.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_myPosts)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { s ->
                binding.tvEmpty.visibility = if (s.items.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(s.items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}