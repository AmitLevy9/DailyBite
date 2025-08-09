package com.example.dailybite.ui.myposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailybite.databinding.FragmentFeedBinding
import com.example.dailybite.ui.feed.PostAdapter
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MyPostsFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var storage: FirebaseStorage
    private lateinit var adapter: PostAdapter

    private val vm: MyPostsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // במסך ״הפוסטים שלי״ אין צורך ב־FAB או בכפתור ניווט ״הפוסטים שלי״
        binding.fabNewPost.visibility = View.GONE
        val id = resources.getIdentifier("btnMyPosts", "id", requireContext().packageName)
        if (id != 0) binding.root.findViewById<View>(id)?.visibility = View.GONE

        adapter = PostAdapter(
            storage = storage,
            onLike = { /* לא קריטי במסך זה, אפשר להשאיר ריק */ },
            onLongPress = { postId, imagePath ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("מחיקת פוסט")
                    .setMessage("למחוק את הפוסט לצמיתות?")
                    .setPositiveButton("מחק") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                            val res = repo.deletePost(postId, imagePath)
                            // פידבק קל למשתמש
                            val msg = if (res.isSuccess) "הפוסט נמחק" else "מחיקה נכשלה"
                            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
                            // אין צורך לרענן - ה־Flow יעדכן את הרשימה אוטומטית
                        }
                    }
                    .setNegativeButton("בטל", null)
                    .show()
            }
        )
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

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

    @Inject lateinit var repo: com.example.dailybite.data.post.PostRepository
}