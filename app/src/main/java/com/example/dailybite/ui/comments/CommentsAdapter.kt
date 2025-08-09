package com.example.dailybite.ui.comments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailybite.data.post.CommentItem
import com.example.dailybite.databinding.ItemCommentBinding

class CommentsAdapter : RecyclerView.Adapter<CommentsAdapter.VH>() {
    private val items = mutableListOf<CommentItem>()

    class VH(val b: ItemCommentBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.b.tvText.text = c.text
        holder.b.tvMeta.text = "by ${c.authorUid} • ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(c.createdAt))}"
    }

    override fun getItemCount() = items.size

    fun submit(list: List<CommentItem>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }
}