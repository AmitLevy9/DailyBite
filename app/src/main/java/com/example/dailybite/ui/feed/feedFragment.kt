package com.example.dailybite.ui.feed

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailybite.R
import com.example.dailybite.data.auth.AuthRepository
import com.example.dailybite.data.post.PostRepository
import com.example.dailybite.databinding.FragmentFeedBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // הפעלת תמיכה בתפריט
        setHasOptionsMenu(true)
    }

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

        // הגדרת האדפטר לפוסטים
        adapter = PostAdapter(
            storage = storage,
            onLike = { postId ->
                val uid = authRepo.currentUidOrNull() ?: run {
                    Toast.makeText(requireContext(), "צריך להתחבר כדי לעשות לייק", Toast.LENGTH_SHORT).show()
                    return@PostAdapter
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val res = postsRepo.like(postId, uid)
                    if (res.isFailure) {
                        Toast.makeText(requireContext(), "הלייק נכשל", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onLongPress = { _, _ -> /* מחיקה בפיד הכללי לא חובה */ },
            onComments = { postId ->
                findNavController().navigate(
                    R.id.action_feed_to_postComments,
                    Bundle().apply { putString("postId", postId) }
                )
            }
        )

        // חיבור האדפטר ל־RecyclerView
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        // האזנה לשינויים ב־ViewModel
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { s ->
                binding.tvEmpty.visibility = if (s.items.isEmpty()) View.VISIBLE else View.GONE
                binding.tvEmpty.text = "הפיד ריק כרגע. לחצי על ה־FAB כדי לפרסם פוסט ראשון."
                adapter.submitList(s.items)
            }
        }

        // כפתור יצירת פוסט חדש
        binding.fabNewPost.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_newPost)
        }

        // כפתור "הפוסטים שלי"
        binding.btnMyPosts.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_myPosts)
        }

        // כפתור מעבר לפרופיל
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_profile)
        }
    }

    // טעינת התפריט העליון
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_feed, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // טיפול בלחיצה על אייטמים בתפריט
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                authRepo.signOut()
                findNavController().navigate(R.id.action_feed_to_login)
                true
            }
            R.id.profileFragment -> {
                findNavController().navigate(R.id.action_feed_to_profile)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}