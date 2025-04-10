package com.vishal2376.miniplayerexperiment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.vishal2376.miniplayerexperiment.databinding.ActivityMainBinding
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isMiniMode = false
    private var player: ExoPlayer? = null

    private var snapThresholdDp = 80
    private val snapThresholdPx: Int
        get() = dpToPx(snapThresholdDp)

    private val demoUrl =
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializePlayer()

        binding.changeMode.setOnClickListener {
            animateChangeMode()
        }

        binding.thresholdSeekBar.progress = snapThresholdDp
        binding.thresholdValue.text = "Snap Threshold: ${snapThresholdDp}dp"

        binding.thresholdSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                snapThresholdDp = progress
                binding.thresholdValue.text = "Snap Threshold: ${snapThresholdDp}dp"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.playerView.setOnTouchListener { view, event ->
            if (!isMiniMode) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val dX = view.x - event.rawX
                    val dY = view.y - event.rawY
                    val dragStartX = event.rawX
                    val dragStartY = event.rawY

                    view.setTag(R.id.tag_dx, dX)
                    view.setTag(R.id.tag_dy, dY)
                    view.setTag(R.id.tag_start_x, dragStartX)
                    view.setTag(R.id.tag_start_y, dragStartY)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dX = view.getTag(R.id.tag_dx) as? Float ?: 0f
                    val dY = view.getTag(R.id.tag_dy) as? Float ?: 0f
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                }

                MotionEvent.ACTION_UP -> {
                    val dragStartX = view.getTag(R.id.tag_start_x) as? Float ?: 0f
                    val dragStartY = view.getTag(R.id.tag_start_y) as? Float ?: 0f
                    val dist = distance(dragStartX, dragStartY, event.rawX, event.rawY)
                    if (dist > snapThresholdPx) {
                        snapToCornerBasedOnDirection(
                            view,
                            dragStartX,
                            dragStartY,
                            event.rawX,
                            event.rawY
                        )
                    } else {
                        snapToNearestCorner(view)
                    }

                    // Clear tags after use
                    view.setTag(R.id.tag_dx, null)
                    view.setTag(R.id.tag_dy, null)
                    view.setTag(R.id.tag_start_x, null)
                    view.setTag(R.id.tag_start_y, null)
                }
            }
            true
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer

            val mediaItem = MediaItem.fromUri(Uri.parse(demoUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun animateChangeMode() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)

        val transition = ChangeBounds().apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }
        TransitionManager.beginDelayedTransition(binding.root, transition)

        if (!isMiniMode) {
            // Mini mode
            val height = dpToPx(140)
            val width = dpToPx(200)

            binding.playerView.layoutParams = binding.playerView.layoutParams.apply {
                this.height = height
                this.width = width
            }

            constraintSet.clear(binding.playerView.id, ConstraintSet.TOP)
            constraintSet.clear(binding.playerView.id, ConstraintSet.START)
            constraintSet.clear(binding.playerView.id, ConstraintSet.END)

            constraintSet.connect(
                binding.playerView.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                dpToPx(20)
            )
            constraintSet.connect(
                binding.playerView.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                dpToPx(20)
            )

            constraintSet.constrainWidth(binding.playerView.id, width)
            constraintSet.constrainHeight(binding.playerView.id, height)

            isMiniMode = true
        } else {
            // Full mode
            val height = dpToPx(300)

            binding.playerView.layoutParams = binding.playerView.layoutParams.apply {
                this.height = height
                this.width = ConstraintSet.MATCH_CONSTRAINT
            }

            constraintSet.clear(binding.playerView.id, ConstraintSet.BOTTOM)
            constraintSet.clear(binding.playerView.id, ConstraintSet.END)

            constraintSet.connect(
                binding.playerView.id,
                ConstraintSet.TOP,
                binding.debugView.id,
                ConstraintSet.BOTTOM,
                0
            )
            constraintSet.connect(
                binding.playerView.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                0
            )
            constraintSet.connect(
                binding.playerView.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                0
            )

            constraintSet.constrainWidth(binding.playerView.id, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.constrainHeight(binding.playerView.id, height)

            // Reset translation
            binding.playerView.translationX = 0f
            binding.playerView.translationY = 0f

            isMiniMode = false
        }

        constraintSet.applyTo(binding.root)
    }

    private fun snapToCornerBasedOnDirection(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        val deltaX = endX - startX
        val deltaY = endY - startY

        val horizontal = if (deltaX > 0) "right" else "left"
        val vertical = if (deltaY > 0) "bottom" else "top"

        val parentWidth = binding.root.width
        val parentHeight = binding.root.height
        val viewWidth = view.width
        val viewHeight = view.height

        val margin = dpToPx(20)
        val leftX = margin.toFloat()
        val rightX = (parentWidth - viewWidth - margin).toFloat()
        val topY = margin.toFloat()
        val bottomY = (parentHeight - viewHeight - margin).toFloat()

        val targetX = if (horizontal == "right") rightX else leftX
        val targetY = if (vertical == "bottom") bottomY else topY

        view.animate()
            .x(targetX)
            .y(targetY)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun snapToNearestCorner(view: View) {
        val parentWidth = binding.root.width
        val parentHeight = binding.root.height
        val viewWidth = view.width
        val viewHeight = view.height

        val margin = dpToPx(20)

        val possibleX = listOf(margin.toFloat(), (parentWidth - viewWidth - margin).toFloat())
        val possibleY = listOf(margin.toFloat(), (parentHeight - viewHeight - margin).toFloat())

        var nearestX = possibleX[0]
        var nearestY = possibleY[0]
        var minDistance = Float.MAX_VALUE

        for (x in possibleX) {
            for (y in possibleY) {
                val dist = distance(view.x, view.y, x, y)
                if (dist < minDistance) {
                    minDistance = dist
                    nearestX = x
                    nearestY = y
                }
            }
        }

        view.animate()
            .x(nearestX)
            .y(nearestY)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }
}
