package ru.tatalaraydar.nmedia

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.tatalaraydar.nmedia.databinding.ActivityMainBinding
import ru.tatalaraydar.nmedia.dto.Post
import ru.tatalaraydar.nmedia.viewmodel.PostViewModel
import androidx.activity.viewModels
import ru.tatalaraydar.nmedia.repository.PostRepository
import ru.tatalaraydar.nmedia.repository.PostRepositoryInMemory
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    private val PostRepositoryInMemory = PostRepositoryInMemory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val viewModel by viewModels<PostViewModel>()
        viewModel.post.observe(this) { post ->
            with(binding) {
                binding.content.text = post.content
                binding.published.text = post.published
                binding.author.text = post.author
                binding.likes.text = PostRepositoryInMemory.formatCount(post.likes)
                binding.viewsPost.text = PostRepositoryInMemory.formatCount(post.views_post)
                binding.share.text = PostRepositoryInMemory.formatCount(post.share)
                binding.root.setOnClickListener {
                }

                binding.buttonShare.setOnClickListener {
                    viewModel.share()
                }

                binding.buttonLikes.setOnClickListener {
                    viewModel.like()
                }
            }
        }
    }
}







