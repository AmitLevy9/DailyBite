package com.example.dailybite.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.dailybite.data.post.PostItem
import com.example.dailybite.databinding.ItemPostBinding
import com.google.firebase.storage.FirebaseStorage


class PostAdapter(
    private val storage: FirebaseStorage,
    private val onLike: (postId: String) -> Unit = {},
    private val onLongPress: ((postId: String, imagePath: String) -> Unit)? = null,
    private val onComments: (postId: String) -> Unit = {}
) : ListAdapter<PostItem, PostAdapter.VH>(DIFF) {

    inner class VH(val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.b.tvMeal.text = item.mealType
        holder.b.tvDesc.text = item.description
        holder.b.tvLikes.text = item.likesCount.toString()

        holder.b.btnLike.setOnClickListener { onLike(item.id) }
        holder.b.btnComments.setOnClickListener { onComments(item.id) }

        holder.itemView.setOnLongClickListener {
            onLongPress?.invoke(item.id, item.imageStoragePath)
            true
        }

        if (item.imageStoragePath.isNotEmpty()) {
            storage.reference.child(item.imageStoragePath)
                .downloadUrl
                .addOnSuccessListener { uri -> holder.b.imgPost.load(uri) }
        } else {
            holder.b.imgPost.setImageDrawable(null)
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PostItem>() {
            override fun areItemsTheSame(oldItem: PostItem, newItem: PostItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: PostItem, newItem: PostItem) =
                oldItem == newItem
        }
    }
}