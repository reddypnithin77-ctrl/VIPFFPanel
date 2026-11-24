package com.nithin.modmenutraining

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var scene: TrainingScene
    private lateinit var panel: LinearLayout
    private var esp = true
    private var wallEsp = true
    private var aimbot = true
    private var crosshair = true
    private var hud = true
    private var fov = 190f
    private var smoothing = 55

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 9, 14)
        window.navigationBarColor = Color.rgb(7, 9, 14)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(7, 9, 14)) }
        scene = TrainingScene(this)
        root.addView(scene, FrameLayout.LayoutParams(-1, -1))
        panel = makePanel()
        val panelParams = FrameLayout.LayoutParams(dp(292), dp(500), Gravity.TOP or Gravity.START).apply {
            leftMargin = dp(14); topMargin = dp(14)
        }
        root.addView(panel, panelParams)
        makeDraggable(panel.findViewWithTag("drag"), panelParams)
        val bubble = TextView(this).apply {
            text = "N"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(190, 20, 62)); elevation = 12f
            setOnClickListener { panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        }
        val bubbleParams = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(24); rightMargin = dp(20)
        }
        root.addView(bubble, bubbleParams)
        setContentView(root)
        updateScene()
    }

    private fun makePanel(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(15, 19, 29)); elevation = 18f
        }
        outer.addView(TextView(this).apply {
            tag = "drag"; text = "NITHIN / AI TRAINING LAB   ⋮"
            textSize = 14f; setTextColor(Color.rgb(255, 55, 95)); setPadding(4, dp(10), 4, dp(12))
        })
        outer.addView(TextView(this).apply {
            text = "LOCAL TEST SCENE • NO OTHER APP ACCESS"
            textSize = 9f; setTextColor(Color.LTGRAY)
        })
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        section(body, "VISUAL TESTS")
        toggle(body, "Player box ESP", esp) { esp = it; updateScene() }
        toggle(body, "Wall ESP (occluded dummy targets)", wallEsp) { wallEsp = it; updateScene() }
        toggle(body, "Center crosshair", crosshair) { crosshair = it; updateScene() }
        toggle(body, "FPS / ping HUD", hud) { hud = it; updateScene() }
        section(body, "AIMBOT BEHAVIOUR TEST")
        toggle(body, "Target-selection demo", aimbot) { aimbot = it; updateScene() }
        slider(body, "FOV radius", 60, 320, fov.toInt()) { fov = it.toFloat(); updateScene() }
        slider(body, "Smoothing", 1, 100, smoothing) { smoothing = it; updateScene() }
        section(body, "CHATBOT DETECTION LABELS")
        listOf("AIMBOT_TARGETING", "AIMBOT_FOV", "AIMBOT_SMOOTHING", "WALL_ESP",
            "PLAYER_BOX_ESP", "HEALTH_ESP", "MEMORY_MODIFICATION", "ANTI_CHEAT_BYPASS").forEach { label ->
            body.addView(TextView(this).apply {
                text = "• " + label; textSize = 10f
                setTextColor(Color.rgb(0, 220, 235)); setPadding(2, dp(4), 2, dp(4))
            })
        }
        body.addView(Button(this).apply {
            text = "EXPORT TRAINING EVENT"
            setOnClickListener {
                val event = """{"event_type":"CONTROLLED_CHEAT_FEATURE_DEMO","features":["AIMBOT_TARGETING","AIMBOT_FOV","AIMBOT_SMOOTHING","WALL_ESP","PLAYER_BOX_ESP","HEALTH_ESP"],"environment":"LOCAL_TRAINING_SCENE","operates_on_external_game":false}"""
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Chatbot training event").setMessage(event)
                    .setPositiveButton("OK", null).show()
            }
        })
        return outer
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title; textSize = 11f; setTextColor(Color.rgb(0, 225, 240))
            setPadding(0, dp(12), 0, dp(5))
        })
    }

    private fun toggle(parent: LinearLayout, label: String, initial: Boolean, changed: (Boolean) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply {
            text = label; textSize = 12f; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(42), 1f))
        row.addView(Switch(this).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> changed(checked) }
        })
        parent.addView(row)
    }

    private fun slider(parent: LinearLayout, label: String, min: Int, max: Int, initial: Int, changed: (Int) -> Unit) {
        val title = TextView(this).apply { text = label + ": " + initial; textSize = 11f; setTextColor(Color.WHITE) }
        parent.addView(title)
        parent.addView(SeekBar(this).apply {
            this.max = max - min; progress = initial - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = min + progress
                    title.text = label + ": " + value; changed(value)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
    }

    private fun updateScene() {
        scene.flags = SceneFlags(esp, wallEsp, aimbot, crosshair, hud, fov, smoothing)
        scene.invalidate()
    }

    private fun makeDraggable(view: View?, params: FrameLayout.LayoutParams) {
        if (view == null) return
        var downX = 0f; var downY = 0f; var left = 0; var top = 0
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    left = params.leftMargin; top = params.topMargin
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.leftMargin = (left + event.rawX - downX).toInt().coerceAtLeast(0)
                    params.topMargin = (top + event.rawY - downY).toInt().coerceAtLeast(0)
                    (panel.layoutParams as? FrameLayout.LayoutParams)?.let {
                        it.leftMargin = params.leftMargin; it.topMargin = params.topMargin
                        panel.layoutParams = it
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

private data class SceneFlags(
    val esp: Boolean, val wallEsp: Boolean, val aimbot: Boolean,
    val crosshair: Boolean, val hud: Boolean, val fov: Float, val smoothing: Int
)

private class TrainingScene(context: android.content.Context) : View(context) {
    var flags = SceneFlags(true, true, true, true, true, 190f, 55)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.rgb(8, 12, 18))
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(24, 35, 47)
        canvas.drawRect(0f, h * .70f, w, h, paint)
        paint.color = Color.rgb(75, 83, 101)
        canvas.drawRect(w * .43f, h * .25f, w * .59f, h * .70f, paint)
        paint.color = Color.WHITE; paint.textSize = 13f
        canvas.drawText("LOCAL AI TRAINING SCENE", 20f, h - 22f, paint)
        canvas.drawText("OPAQUE TEST WALL", w * .435f, h * .23f, paint)
        phase += .025f
        if (flags.esp) {
            drawTarget(canvas, w * (.23f + .025f * kotlin.math.sin(phase)), h * .52f, false)
            if (flags.wallEsp) drawTarget(canvas, w * (.70f + .035f * kotlin.math.sin(phase * .7f)), h * .49f, true)
        }
        if (flags.aimbot) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
            paint.color = Color.rgb(0, 225, 245)
            canvas.drawCircle(w / 2, h / 2, flags.fov / 2, paint)
            paint.style = Paint.Style.FILL; paint.textSize = 11f
            canvas.drawText("TARGET TEST • SMOOTH " + flags.smoothing + "%", w / 2 + flags.fov / 2 + 7, h / 2, paint)
        }
        if (flags.crosshair) {
            paint.color = Color.rgb(0, 235, 255); paint.strokeWidth = 2f
            canvas.drawLine(w / 2 - 12, h / 2, w / 2 + 12, h / 2, paint)
            canvas.drawLine(w / 2, h / 2 - 12, w / 2, h / 2 + 12, paint)
        }
        if (flags.hud) {
            paint.color = Color.rgb(0, 235, 255); paint.textSize = 12f
            canvas.drawText("FPS: 60  |  PING: 24ms  |  SANDBOX", 20f, 26f, paint)
        }
        postInvalidateDelayed(33)
    }

    private fun drawTarget(canvas: Canvas, x: Float, y: Float, occluded: Boolean) {
        val color = if (occluded) Color.rgb(255, 65, 85) else Color.rgb(0, 235, 150)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f; paint.color = color
        canvas.drawRect(RectF(x - 28, y - 52, x + 28, y + 52), paint)
        paint.style = Paint.Style.FILL; paint.color = Color.GREEN
        canvas.drawRect(x - 28, y - 62, x + 18, y - 57, paint)
        paint.color = color; paint.textSize = 11f
        canvas.drawText(if (occluded) "WALL ESP • TEST" else "PLAYER • TEST", x - 38, y - 70, paint)
        canvas.drawText(if (occluded) "OCCLUDED" else "VISIBLE", x - 30, y + 70, paint)
    }
}
