package com.example.projeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Comment(val text: String, val rating: Float)

class CommentAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commentText: TextView = itemView.findViewById(R.id.comment_text)
        val commentRating: RatingBar = itemView.findViewById(R.id.comment_rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.commentText.text = comment.text
        holder.commentRating.rating = comment.rating
    }

    override fun getItemCount() = comments.size
}