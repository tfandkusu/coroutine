package com.example.coroutine.datastore

import io.reactivex.Observable
import com.example.coroutine.entity.Comment
import com.example.coroutine.entity.Post
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * JSONPlaceholder APIのインターフェースを定義する。
 * https://jsonplaceholder.typicode.com/
 */
interface PlaceholderServiceRX {
    /**
     * 記事一覧を取得する
     */
    @GET("/posts")
    fun listPosts(): Observable<List<Post>>

    /**
     * コメント一覧を取得する
     */
    @GET("/posts/{postId}/comments")
    fun listComments(@Path("postId")  postId : Int) : Observable<List<Comment>>
}
