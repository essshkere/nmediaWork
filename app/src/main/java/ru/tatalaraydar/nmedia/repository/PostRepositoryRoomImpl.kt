package ru.tatalaraydar.nmedia.repository


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.floor
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import ru.tatalaraydar.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit

class PostRepositoryRoomImpl : PostRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<Post>>() {}

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    init {
        _posts.value = getAll()
    }

    override fun getAll(): List<Post> {
        return try {
            val request: Request = Request.Builder()
                .url("${BASE_URL}/api/slow/posts")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw RuntimeException("body is null")
            gson.fromJson(responseBody, typeToken.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    try {
                        callback.onSuccess(gson.fromJson(body, typeToken.type))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun save(post: Post) {
        val request: Request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .execute()
            .close()

        val currentPosts = posts.value ?: emptyList()
        _posts.postValue(currentPosts + post)

    }

    override fun removeById(id: Long) {
        val request: Request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        client.newCall(request)
            .execute()
            .close()

        val currentPosts = _posts.value ?: emptyList()
        _posts.postValue(currentPosts.filter { it.id != id })
    }

    override fun likeById(post: Post): Post {
        val response = if (post.likedByMe) {
            val request: Request = Request.Builder()
                .delete()
                .url("${BASE_URL}/api/slow/posts/${post.id}/likes")
                .build()
            client.newCall(request).execute()
        } else {
            val request: Request = Request.Builder()
                .post("{}".toRequestBody(jsonType))
                .url("${BASE_URL}/api/slow/posts/${post.id}/likes")
                .build()
            client.newCall(request).execute()
        }

        val responseBody = response.body?.string() ?: throw RuntimeException("body is null")
        return gson.fromJson(responseBody, Post::class.java)
    }

    private fun getPostById(id: Long): Post {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()
        return client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, Post::class.java)
            }
    }

    override fun updateShareById(id: Long) {
        val request: Request = Request.Builder()
            .post("{}".toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts/$id/shares")
            .build()
        client.newCall(request)
            .execute()
            .close()

        val currentPosts = _posts.value ?: emptyList()
        _posts.postValue(currentPosts.map {
            if (it.id == id) it.copy(share = it.share + 1) else it
        })
    }

    companion object {
        private const val KEY = "id"
        private const val ID = "posts"
        private const val FILENAME = "posts.json"
        private val gson = Gson()
        private val type = TypeToken.getParameterized(List::class.java, Post::class.java).type

        private const val BASE_URL = "http://10.0.2.2:9999"
        private val jsonType = "application/json".toMediaType()

        fun formatCount(count: Int): String {
            return when {
                count >= 1_000_000 -> String.format("%.1fM", floor(count / 1_000_000.0 * 10) / 10)
                    .replace(",", ".")

                count >= 1_000 -> String.format("%.1fK", floor(count / 1_000.0 * 10) / 10)
                    .replace(",", ".")

                else -> count.toString()
            }
        }
    }
}