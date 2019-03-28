package com.example.coroutine.entity

/**
 * https://jsonplaceholder.typicode.com/posts
 *
 * ブログの記事
 */
data class Post(val userId: Int, val id: Int, val title: String, val body: String)
