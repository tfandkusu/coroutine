package com.example.coroutine.entity

/**
 * https://jsonplaceholder.typicode.com/posts/1/comments
 *
 * ブログのコメント
 */
data class Comment(val postId : Int, val id : Int,val name : String, val email : String,val body : String)
