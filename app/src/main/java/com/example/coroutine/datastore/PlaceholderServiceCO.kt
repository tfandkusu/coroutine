package com.example.coroutine.datastore

import com.example.coroutine.entity.Comment
import com.example.coroutine.entity.Post
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Path

interface PlaceholderServiceCO {
    /**
     * 記事一覧を取得する
     */
    @GET("/posts")
    fun listPosts(): Deferred<List<Post>>

    /**
     * コメント一覧を取得する
     */
    @GET("/posts/{postId}/comments")
    fun listComments(@Path("postId")  postId : Int) : Deferred<List<Comment>>
}