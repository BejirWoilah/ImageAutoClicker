package com.example.imageautoclicker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

// panel melayang di atas aplikasi lain (tampilan mengikuti desain IM Clicker)
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var root: FrameLayout? = null
    private var running = false

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (root == null) {
            val planName = intent?.getStringExtra("plan_name") ?: "Plan"
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            showOverlay(planName)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        root?.let { windowManager.removeView(it) }
        root = null
        super.onDestroy()
    }

    private fun showOverlay(planName: String) {
        val screenWidth = resources.displayMetrics.widthPixels
        val panelWidth = (screenWidth * 0.92f).toInt()

        params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = (screenWidth - panelWidth) / 2
        params.y = dp(120)

        // ---- panel utama ----
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(16), dp(12), dp(16), dp(16))
        panel.background = rounded(Color.parseColor("#333333"), dp(24))

        // judul plan di tengah, tombol ciutkan di kanan (judul bisa dipakai untuk menggeser panel)
        val titleRow = FrameLayout(this)

        val title = TextView(this)
        title.text = planName
        title.textSize = 16f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.parseColor("#D0D0D0"))
        title.gravity = Gravity.CENTER
        titleRow.addView(title, FrameLayout.LayoutParams(match, dp(36)))

        val collapse = TextView(this)
        collapse.text = "—"
        collapse.textSize = 18f
        collapse.setTextColor(Color.parseColor("#D0D0D0"))
        collapse.gravity = Gravity.CENTER
        titleRow.addView(
            collapse,
            FrameLayout.LayoutParams(dp(40), dp(36), Gravity.END or Gravity.CENTER_VERTICAL)
        )

        // baris Start + status
        val startRow = LinearLayout(this)
        startRow.orientation = LinearLayout.HORIZONTAL
        startRow.gravity = Gravity.CENTER_VERTICAL

        val startButton = TextView(this)
        startButton.text = "Start"
        startButton.textSize = 14f
        startButton.setTextColor(Color.parseColor("#F5DDD0"))
        startButton.gravity = Gravity.CENTER
        startButton.background = circle(Color.parseColor("#5A4033"))
        startRow.addView(startButton, LinearLayout.LayoutParams(dp(60), dp(60)))

        val status = TextView(this)
        status.text = "Idle"
        status.textSize = 12f
        status.setTextColor(Color.parseColor("#BBBBBB"))
        status.gravity = Gravity.CENTER_VERTICAL
        status.setPadding(dp(12), 0, dp(12), 0)
        status.background = rounded(Color.parseColor("#5A5A5A"), dp(6))
        val statusParams = LinearLayout.LayoutParams(0, dp(36), 1f)
        statusParams.leftMargin = dp(12)
        startRow.addView(status, statusParams)

        // baris tombol menu: Click, Image Click, Exit
        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL
        buttonRow.setPadding(0, dp(16), 0, 0)

        val clickItem = menuItem("Click", "👆")
        val imageItem = menuItem("Image Click", "🖼")
        val exitItem = menuItem("Exit", "🚪")
        buttonRow.addView(clickItem, LinearLayout.LayoutParams(dp(88), wrap))
        buttonRow.addView(imageItem, LinearLayout.LayoutParams(dp(88), wrap))
        buttonRow.addView(exitItem, LinearLayout.LayoutParams(dp(88), wrap))

        panel.addView(titleRow, LinearLayout.LayoutParams(match, wrap))
        val startRowParams = LinearLayout.LayoutParams(match, wrap)
        startRowParams.topMargin = dp(8)
        panel.addView(startRow, startRowParams)
        panel.addView(buttonRow, LinearLayout.LayoutParams(match, wrap))

        // ---- bentuk ciut: bulatan kecil ----
        val bubble = TextView(this)
        bubble.text = "▶"
        bubble.textSize = 20f
        bubble.setTextColor(Color.WHITE)
        bubble.gravity = Gravity.CENTER
        bubble.background = circle(Color.parseColor("#703810"))
        bubble.visibility = View.GONE

        val container = FrameLayout(this)
        container.addView(panel, FrameLayout.LayoutParams(match, wrap))
        container.addView(bubble, FrameLayout.LayoutParams(dp(56), dp(56)))

        // ---- aksi tombol ----
        startButton.setOnClickListener {
            running = !running
            startButton.text = if (running) "Stop" else "Start"
            status.text = if (running) "Running" else "Idle"
        }

        clickItem.setOnClickListener {
            Toast.makeText(this, "Fitur Click: segera dibuat", Toast.LENGTH_SHORT).show()
        }

        imageItem.setOnClickListener {
            Toast.makeText(this, "Fitur Image Click: segera dibuat", Toast.LENGTH_SHORT).show()
        }

        exitItem.setOnClickListener {
            stopSelf()
        }

        collapse.setOnClickListener {
            panel.visibility = View.GONE
            bubble.visibility = View.VISIBLE
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(container, params)
        }

        // geser panel lewat judul, geser bulatan lewat bulatannya; ketuk bulatan untuk membuka lagi
        root = container
        makeDraggable(titleRow, null)
        makeDraggable(bubble) {
            bubble.visibility = View.GONE
            panel.visibility = View.VISIBLE
            params.width = panelWidth
            params.x = (screenWidth - panelWidth) / 2
            windowManager.updateViewLayout(container, params)
        }

        windowManager.addView(container, params)
    }

    // satu item menu: lingkaran cokelat + tulisan di bawahnya
    private fun menuItem(label: String, symbol: String): LinearLayout {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER_HORIZONTAL

        val circleView = TextView(this)
        circleView.text = symbol
        circleView.textSize = 24f
        circleView.gravity = Gravity.CENTER
        circleView.background = circle(Color.parseColor("#703810"))
        item.addView(circleView, LinearLayout.LayoutParams(dp(56), dp(56)))

        val labelView = TextView(this)
        labelView.text = label
        labelView.textSize = 12f
        labelView.setTextColor(Color.parseColor("#CCCCCC"))
        labelView.gravity = Gravity.CENTER
        val labelParams = LinearLayout.LayoutParams(wrap, wrap)
        labelParams.topMargin = dp(4)
        item.addView(labelView, labelParams)

        return item
    }

    // membuat view bisa digeser; onTap dipanggil kalau cuma diketuk (tidak bergeser)
    private fun makeDraggable(handle: View, onTap: (() -> Unit)?) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    root?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - touchX) > dp(10) || abs(event.rawY - touchY) > dp(10)
                    if (!moved) {
                        onTap?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = radius.toFloat()
        return drawable
    }

    private fun circle(color: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(color)
        return drawable
    }
}
