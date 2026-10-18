package com.jabir.brot.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*

/**
 * Direct WindowManager overlay controller managed seamlessly by ReelCountAccessibilityService.
 *
 * Key advantages:
 * - NO FOREGROUND SERVICE: Eliminates the persistent, annoying notification completely!
 * - Steady Visibility: 1200ms exit debounce prevents any flickering while watching reels.
 * - Smooth Dragging: Clamped to screen bounds with persistent position memory.
 * - Clean circular number badge with warm amber styling.
 */
class FloatingOverlayController(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var tvCountOnly: TextView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var hideJob: Job? = null

    private var currentTotal = 0
    private var currentTarget = 50

    companion object {
        private var savedX: Int = 36
        private var savedY: Int = 260
    }

    fun show() {
        hideJob?.cancel()
        hideJob = null

        if (overlayView == null) {
            initOverlayView()
        }
        overlayView?.visibility = View.VISIBLE
    }

    fun hide(debounced: Boolean = true) {
        if (!debounced) {
            hideJob?.cancel()
            hideJob = null
            overlayView?.visibility = View.GONE
            return
        }

        // Debounced hide (1200ms): If another reel event arrives, hide is cancelled!
        hideJob?.cancel()
        hideJob = controllerScope.launch {
            delay(1200)
            overlayView?.visibility = View.GONE
        }
    }

    fun updateCount(total: Int, target: Int) {
        currentTotal = total
        currentTarget = target
        tvCountOnly?.text = "$total"

        val isExceeded = target in 1..total
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (isExceeded) {
                setColor(0xEEF43F5E.toInt())
                setStroke(4, 0xFFFF3B30.toInt())
            } else {
                setColor(0xF0161311.toInt())
                setStroke(3, 0xFFF59E0B.toInt())
            }
        }
        overlayView?.background = bg
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlayView() {
        if (!Settings.canDrawOverlays(context)) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        val sizePx = (54 * density).toInt()

        val wmLayoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX.coerceIn(8, (screenWidth - sizePx - 8).coerceAtLeast(8))
            y = savedY.coerceIn(24, (screenHeight - sizePx - 24).coerceAtLeast(24))
        }
        windowLayoutParams = wmLayoutParams

        // Circular root container
        val root = FrameLayout(context).apply {
            elevation = 18f
            visibility = View.GONE

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xF0161311.toInt())
                setStroke(3, 0xFFF59E0B.toInt())
            }
            background = bg
        }

        // Centered clean number only
        tvCountOnly = TextView(context).apply {
            text = "$currentTotal"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(
            tvCountOnly,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )

        // Smooth clamp-bounded dragging & click feedback
        root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val lp = windowLayoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = lp.x
                        initialY = lp.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) {
                            isClick = false
                        }
                        val newX = (initialX + dx).coerceIn(8, (screenWidth - sizePx - 8).coerceAtLeast(8))
                        val newY = (initialY + dy).coerceIn(24, (screenHeight - sizePx - 24).coerceAtLeast(24))
                        lp.x = newX
                        lp.y = newY
                        savedX = newX
                        savedY = newY
                        windowManager?.updateViewLayout(root, lp)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            Toast.makeText(
                                context,
                                "Reels Today: $currentTotal | Daily Limit: $currentTarget",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(root, wmLayoutParams)
            overlayView = root
            updateCount(currentTotal, currentTarget)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        hideJob?.cancel()
        controllerScope.cancel()
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Ignore if already removed
            }
            overlayView = null
        }
    }
}
