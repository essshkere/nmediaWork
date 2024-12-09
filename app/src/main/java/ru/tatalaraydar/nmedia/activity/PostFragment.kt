package ru.tatalaraydar.nmedia.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.tatalaraydar.nmedia.R
import ru.tatalaraydar.nmedia.activity.NewPostFragment.Companion.textArg
import ru.tatalaraydar.nmedia.adapter.OnInteractionListener
import ru.tatalaraydar.nmedia.adapter.PostsAdapter
import ru.tatalaraydar.nmedia.databinding.FragmentPostBinding
import ru.tatalaraydar.nmedia.dto.Post
import ru.tatalaraydar.nmedia.repository.PostRepositoryInMemory.Companion.formatCount
import ru.tatalaraydar.nmedia.viewmodel.PostViewModel

class PostFragment : Fragment() {
    private var postId: Long = 0L
    private val TAG = "adapter"


    val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPostBinding.inflate(inflater, container, false)

        arguments?.let {
            postId = it.getLong("postId", -1)
            if (postId == -1L) {
                Log.e(TAG, "Post ID not found")
            } else {
                Log.i(TAG, "Received post ID: $postId")
            }
        }

        viewModel.findPostIdById(postId).observe(viewLifecycleOwner) { post ->
            binding.author.text = post?.author ?: "Автор не указан"
            binding.content.text = post?.content ?: "Содержимое отсутствует"
            binding.published.text = post?.published ?: "Дата публикации не указана"
            binding.viewsPost.text = post?.views_post?.let { formatCount(it) } ?: "0 просмотров"
            binding.buttonLikes.text = post?.likes?.let { formatCount(it) } ?: "0 лайков"
            binding.buttonShare.text = post?.share?.let { formatCount(it) } ?: "0 поделились"
            binding.buttonLikes.isChecked = post?.likedByMe == true
            binding.buttonLikes.setOnClickListener { viewModel.like(post?.id ?: 0) }
            binding.buttonShare.setOnClickListener {
                if (post != null) {
                    sharePost(post)
                }
            }
            binding.menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                viewModel.remove(post?.id ?: 0)
                                findNavController().navigateUp()
                                true
                            }

                            R.id.edit -> {
                                viewModel.edited.observe(viewLifecycleOwner) { post ->
                                    if (post.id != 0L) {
                                        findNavController().navigate(
                                            R.id.action_postFragment_to_editPostFragment,
                                            Bundle().apply { textArg = post.content })
                                    }
                                }
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }
        }

        val adapter = PostsAdapter(object : OnInteractionListener {

            override fun onRemove(post: Post) {
                viewModel.remove(post.id)
            }

            override fun onLike(post: Post) {
                viewModel.like(post.id)
            }

            override fun onEdit(post: Post) {
                viewModel.startEditing(post)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onVideolink(post: Post) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.videoURL))
                startActivity(intent)
            }
        })

        viewModel.edited.observe(viewLifecycleOwner) { post ->
            if (post.id != 0L) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_editPostFragment,
                    Bundle().apply { textArg = post.content })
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val updatedContent =
                    data.getStringExtra("updated_content") ?: return@registerForActivityResult
                val postId = data.getLongExtra("post_id", 0L)
                viewModel.updatePost(postId, updatedContent)
            }
        }
        return binding.root
    }

    private fun sharePost(post: Post) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, post.content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
        startActivity(shareIntent)
    }
}