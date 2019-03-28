package com.example.coroutine

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.coroutine.datastore.NoPostsException
import com.example.coroutine.datastore.PlaceholderService
import com.example.coroutine.datastore.PlaceholderServiceCO
import com.example.coroutine.datastore.PlaceholderServiceRX
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import com.example.coroutine.entity.Comment
import com.example.coroutine.entity.Post
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import retrofit2.*
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException


/**
 * 最初で唯一の画面
 */
class MainActivity : AppCompatActivity() {

    lateinit var service: PlaceholderService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // APIクライアント実装を作成する
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(PlaceholderService::class.java)

        //APIを非同期で呼ぶ
        //APIで取得したい項目は最初の記事にコメントした人すべて

        // 案1. コールバック地獄版
        // requestCallbackHell()

        // 案2. 1コールバック1メソッド
        // requestPostsAsync()

        // 案3. RXJava
        // requestRxJava()

        // 案4. Kotlin-coroutine
        requestKotlinCoroutines()
    }

    /**
     * APIを呼び出す。コールバック地獄版。
     */
    private fun requestCallbackHell() {
        service.listPosts().enqueue(object : Callback<List<Post>> {
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                showNetworkError()
            }

            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                // 通信はワーカスレッドで行われるが
                // コールバックであるここはメインスレッドとなる
                if (response.isSuccessful) {
                    response.body()?.let { posts ->
                        if (posts.isNotEmpty()) {
                            //最初の記事のコメント一覧を取得
                            service.listComments(posts[0].id).enqueue(object : Callback<List<Comment>> {
                                override fun onFailure(call: Call<List<Comment>>, t: Throwable) {
                                    showNetworkError()
                                }

                                override fun onResponse(call: Call<List<Comment>>, response: Response<List<Comment>>) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { comments ->
                                            val sb = StringBuilder()
                                            for (comment in comments) {
                                                sb.append(comment.name)
                                                sb.append("\n")
                                            }
                                            result.text = sb.toString()
                                        }
                                    } else {
                                        showServerError()
                                    }
                                }
                            })
                        }
                    }
                } else {
                    showServerError()
                }
            }
        })
    }


    /**
     * 記事一覧を非同期に読み込む
     */
    private fun requestPostsAsync() {
        service.listPosts().enqueue(object : Callback<List<Post>> {
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                showNetworkError()
            }

            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    response.body()?.let { posts ->
                        if (posts.isNotEmpty()) {
                            requestCommentsAsync(posts[0])

                            // 別案 メッセージを送る
                        }
                    }
                } else {
                    showServerError()
                }
            }
        })
    }


    /**
     * 記事イベントを受け取った
     */
    fun requestCommentsAsync(post: Post) {

        //最初の記事のコメント一覧を取得
        service.listComments(post.id).enqueue(object : Callback<List<Comment>> {
            override fun onFailure(call: Call<List<Comment>>, t: Throwable) {
                showNetworkError()
            }

            override fun onResponse(call: Call<List<Comment>>, response: Response<List<Comment>>) {
                if (response.isSuccessful) {
                    response.body()?.let { comments ->
                        val sb = StringBuilder()
                        for (comment in comments) {
                            sb.append(comment.name)
                            sb.append("\n")
                        }
                        result.text = sb.toString()
                    }
                } else {
                    showServerError()
                }
            }
        })

        // よみづらい

        // 非同期関数呼び出しでは無くメッセージを伝える方法もある

        // 別案　メッセージを受け取る
    }

    /**
     * JXJava版
     */
    @SuppressLint("CheckResult")
    private fun requestRxJava() {
        // APIクライアント実装を作成する
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
        val service = retrofit.create(PlaceholderServiceRX::class.java)

        // リアクティブプログラム
        // 流れてくるデータに反応して処理するプログラムを書ける
        // スレッドの切り替えもできる

        service.listPosts()
            .map { if (it.isNotEmpty()) it[0].id else throw NoPostsException() }
            .flatMap { service.listComments(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()) // 下で実行するスレッド
            .subscribe({
                printThread("subscribe");
                it?.let { comments ->
                    val sb = StringBuilder()
                    for (comment in comments) {
                        sb.append(comment.name)
                        sb.append("\n")
                    }
                    result.text = sb.toString()
                }
            }, {
                if (it is HttpException) {
                    showServerError()
                } else if (it is IOException) {
                    showNetworkError()
                } else if (it is NoPostsException) {
                    result.text = getString(R.string.error_no_posts)
                }
            })
    }

    private fun requestKotlinCoroutines() = GlobalScope.launch(Dispatchers.Main) {
        // APIクライアント実装を作成する
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
        val service = retrofit.create(PlaceholderServiceCO::class.java)


        try {
            printThread("1")
            val posts = service.listPosts().await()
            val comments = service.listComments(posts[0].id).await()
            printThread("3")

            val sb = StringBuilder()
            for (comment in comments) {
                sb.append(comment.name)
                sb.append("\n")
            }
            result.text = sb.toString()
        }catch (e:HttpException){
            showServerError()
        }catch (e:IOException){
            showNetworkError()
        }catch (e: NoPostsException){
            result.text = getString(R.string.error_no_posts)
        }
    }


    /**
     * ネットワークエラーを表示する
     */
    private fun showNetworkError() {
        Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show()
    }

    /**
     * さーばーエラーを表示する
     */
    private fun showServerError() {
        Toast.makeText(this, R.string.error_server, Toast.LENGTH_SHORT).show()
    }

    private fun printThread(name: String) {
        if (Thread.currentThread().id == mainLooper.thread.id)
            Log.d("Takada", "$name is on main thread.")
        else
            Log.d("Takada", "$name is on worker thread.")
    }
}
