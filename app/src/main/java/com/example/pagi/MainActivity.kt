package com.example.pagi

import android.content.Context
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil



import com.example.pagi.Post
import android.nfc.tech.MifareUltralight.PAGE_SIZE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.recyclerview.widget.RecyclerView
import com.example.pagi.databinding.ActivityMainBinding
import com.example.pagi.databinding.ItemLayoutBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

//


import androidx.recyclerview.widget.LinearLayoutManager


class MainActivity : AppCompatActivity() ,LoadingCallback{
    override fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    private  val PAGE_SIZE = 4
    private lateinit var binding: ActivityMainBinding
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postAdapter = PostAdapter()

        // Set LinearLayoutManager
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.recyclerView.adapter = postAdapter

        fetchData()
    }

    private fun fetchData() {
        //binding.progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "fetchdat1", Toast.LENGTH_SHORT).show()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this, "fetchdat2", Toast.LENGTH_SHORT).show()
        val apiService = retrofit.create(ApiService::class.java)

        val postPagingSource = { PostPagingSource(apiService,this) }

        lifecycleScope.launch {
            try {
                Pager(
                    config = PagingConfig(pageSize = 2),
                    pagingSourceFactory = postPagingSource

                ).flow
                    .cachedIn(lifecycleScope)
                    .collectLatest { pagingData ->
                        postAdapter.submitData(pagingData)
                    }
            } catch (e: Exception) {
                // Handle exceptions if needed
                e.printStackTrace()
            } finally {
            }
        }
    }


}

private const val INITIAL_PAGE = 0
class PostPagingSource(
    private val apiService: ApiService,
    private val loadingCallback: LoadingCallback
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        loadingCallback.showLoading(true) // Show progress bar

        val position = params.key ?: INITIAL_PAGE

        return try {
            val response = apiService.getPosts(position * PAGE_SIZE, PAGE_SIZE)
            val posts = response.body() ?: emptyList()

            loadingCallback.showLoading(false) // Hide progress bar

            LoadResult.Page(
                data = posts,
                prevKey = if (position == INITIAL_PAGE) null else position - 1,
                nextKey = if (posts.isEmpty()) null else position + 1
            )
        } catch (e: IOException) {
            loadingCallback.showLoading(false) // Hide progress bar
            LoadResult.Error(e)
        } catch (e: HttpException) {
            loadingCallback.showLoading(false) // Hide progress bar
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition
    }
}

interface LoadingCallback {
    fun showLoading(isLoading: Boolean)
}


interface ApiService {

    @GET("posts")
    suspend fun getPosts(
        @Query("_start") start: Int,
        @Query("_limit") limit: Int
    ): Response<List<Post>>
}

data class Post(
    val id: Long,
    val title: String,
    val body: String
)


class PostAdapter : PagingDataAdapter<Post, PostAdapter.PostViewHolder>(POST_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        post?.let {
            holder.bind(it)
        }
    }

    class PostViewHolder(private val binding: ItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.titleTextView.text = post.id.toString()
            binding.bodyTextView.text = post.body
        }
    }

    companion object {
        private val POST_COMPARATOR = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem == newItem
            }
        }
    }
}