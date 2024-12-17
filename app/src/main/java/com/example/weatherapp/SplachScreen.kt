package com.example.weatherapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.View.MainActivity
import com.example.weatherapp.databinding.ActivitySplachScreenBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SplachScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplachScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val locale = Locale("en")
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        enableEdgeToEdge()
        binding = ActivitySplachScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initial state of text views
        binding.textView.apply {

            alpha = 0f
            rotation = 0f // Initial rotation for textView
        }
        binding.textView2.apply {
            alpha = 0f
            rotation = 0f // Initial rotation for textView2
        }
        binding.z.apply {
            alpha = 0f
            rotation = 0f // Initial rotation for textView2

        }
        val scaleXUp = ObjectAnimator.ofFloat(binding.imageView, "scaleX", 1f,1.2f)
        val scaleYUp = ObjectAnimator.ofFloat(binding.imageView, "scaleY", 1f,1.2f)
        val scaleXDown = ObjectAnimator.ofFloat(binding.imageView, "scaleX", 1.2f,1f)
        val scaleYDown = ObjectAnimator.ofFloat(binding.imageView, "scaleY", 1.2f,1f)
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val screenWidth = binding.imageView.width.toFloat()

        val moveDown = ObjectAnimator.ofFloat(binding.imageView, "translationY",
             (screenHeight / 2).toFloat())
        val moveRight = ObjectAnimator.ofFloat(binding.imageView, "translationX", (screenWidth)-300)
        val animationSet: AnimatorSet = AnimatorSet()

        animationSet.playSequentially(

            AnimatorSet().apply {
                playTogether(scaleXUp, scaleYUp, moveDown, moveRight) // Scale up together
            },
            AnimatorSet().apply {
                playTogether(scaleXDown, scaleYDown) // Scale down together
            }
        )
        animationSet.duration = 500 // Duration for each scale phase
        animationSet.interpolator = LinearInterpolator() // Repeat once (2 total runs)
        animationSet.start()

        lifecycleScope.launch {
            // Animate the "z" text view
            val zJob = launch {
                delay(1550) // Delay before starting animation
                binding.z.alpha = 1f
                val screenHeight = resources.displayMetrics.heightPixels.toFloat()

                val animatorZ = ObjectAnimator.ofFloat(
                    binding.z,
                    "translationY",
                    (binding.z.height + (screenHeight/2))+240

                )
                val widthOfScreen = resources.displayMetrics.widthPixels.toFloat()
                val animatorZX = ObjectAnimator.ofFloat(
                    binding.z,
                    "translationX",
                    widthOfScreen -1090f
                )
                val scaleY = ObjectAnimator.ofFloat(
                    binding.z,
                    "scaleY",
                     1f,
                )
                val scaleX = ObjectAnimator.ofFloat(
                    binding.z,
                    "scaleX",
                     1f,

                )
                scaleX.duration = 1000
                scaleX.interpolator = DecelerateInterpolator()
                scaleX.start()
                scaleY.duration = 1000
                scaleY.interpolator = DecelerateInterpolator()
                scaleY.start()
                animatorZ.duration = 500
                animatorZ.interpolator = AccelerateDecelerateInterpolator()
                animatorZ.start()
                animatorZX.duration = 500
                animatorZX.interpolator = AccelerateDecelerateInterpolator()
                animatorZX .start()

            }
            zJob.join() // Wait for z animation to finish

            // Animate textView and textView2 rotations
            val rotationJob = launch {
                delay(600)
                val animatorTextViewWidth = ObjectAnimator.ofFloat(
                    binding.textView,
                    "translationX",
                    -10f,
                )
                animatorTextViewWidth.interpolator = LinearInterpolator()
                animatorTextViewWidth.duration = 500
                animatorTextViewWidth.start()
                // Animate textView
                val textsJob = launch {
                    binding.textView.alpha = 1f
                    ObjectAnimator.ofFloat(binding.textView, "rotation", 0f, -15f).apply {
                        duration = 100
                        interpolator = LinearInterpolator()
                        start()
                    }
                    binding.textView2.alpha = 1f
                    ObjectAnimator.ofFloat(binding.textView2, "rotation", 0f, 10f).apply {
                        duration = 100
                        interpolator = LinearInterpolator()
                        start()
                    }
                }
                textsJob.join()

                val Resetting2 =launch {
                    delay(1000)
                    ObjectAnimator.ofFloat(binding.textView2, "rotation", 10f, 0f).apply {
                        duration = 100
                        interpolator = LinearInterpolator()
                        start()
                        binding.z.textSize = 45f
                    }
                    ObjectAnimator.ofFloat(binding.textView, "rotation", -15f, 0f).apply {
                        duration = 100
                        interpolator = LinearInterpolator()
                        start()
                    }
                }
                Resetting2.join()
                lifecycleScope.launch {
                    delay(2000)
                    startActivity(Intent(this@SplachScreen, MainActivity::class.java))
                }
            }
            rotationJob.join() // Ensure everything completes sequentially if needed

        }


        lifecycleScope.launch {
            // Animate textView
            delay(800)
            binding.textView.alpha = 1f
            val animatorTextView = ObjectAnimator.ofFloat(
                binding.textView,
                "translationY",
                binding.textView2.height.toFloat() - 800,
            )

            animatorTextView.duration = 100
            animatorTextView.interpolator = AccelerateDecelerateInterpolator()
            animatorTextView.start()

        }

        lifecycleScope.launch {
            // Animate textView2
            delay(800)
            binding.textView2.alpha = 1f
            val animatorTextView2 = ObjectAnimator.ofFloat(
                binding.textView2,
                "translationY",
               binding.textView2.height.toFloat() -800
            )
            animatorTextView2.duration = 100
            animatorTextView2.interpolator = AccelerateDecelerateInterpolator()
            animatorTextView2.start()
        }


    }

}
