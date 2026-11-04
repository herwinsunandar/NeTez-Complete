package com.herwin.netez.ui.components.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.herwin.netez.utils.TerminalUtils

class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var emulator = TerminalEmulator()
        private set

    var onSendBytes: ((ByteArray) -> Unit)? = null
    var onResize: ((cols: Int, rows: Int, widthPx: Int, heightPx: Int) -> Unit)? = null
    var onScrollStateChanged: ((isBottom: Boolean) -> Unit)? = null

    private val density = context.resources.displayMetrics.density
    private var padLeftPx = (12f * density).toInt()
    private var padTopPx = (8f * density).toInt()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = TerminalUtils.getMonospaceTypeface(context)
        textSize = 10f * density
    }
    private val bgPaint = Paint()
    private val cursorPaint = Paint().apply {
        color = 0xFF00FF00.toInt() // TermGreen
        style = Paint.Style.FILL
    }

    var fontCharWidth = 10f
        private set
    var fontLineHeight = 18f
        private set

    private var fontSizeSp = 10f
    private var scrollOffset = 0 // Rows scrolled up from bottom

    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            val newSize = (fontSizeSp * factor).coerceIn(8f, 16f)
            if (Math.abs(newSize - fontSizeSp) > 0.5f) {
                setFontSize(newSize)
            }
            return true
        }
    })

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        calculateFontMetrics()
    }

    fun setFontSize(sp: Float) {
        fontSizeSp = sp.coerceIn(8f, 16f)
        textPaint.textSize = fontSizeSp * density
        calculateFontMetrics()
        recalculateTerminalSize()
        postInvalidate()
    }

    fun getFontSizeSp(): Float = fontSizeSp

    private fun calculateFontMetrics() {
        val fm = textPaint.fontMetrics
        fontLineHeight = Math.ceil((fm.bottom - fm.top).toDouble()).toFloat()
        val bounds = Rect()
        textPaint.getTextBounds("X", 0, 1, bounds)
        fontCharWidth = textPaint.measureText("M")
        if (fontCharWidth <= 0) fontCharWidth = 10f
    }

    private fun recalculateTerminalSize() {
        val availWidth = width - (padLeftPx * 2)
        val availHeight = height - (padTopPx * 2)
        if (availWidth > 0 && availHeight > 0) {
            val cols = Math.max(4, (availWidth / fontCharWidth).toInt())
            val rows = Math.max(4, (availHeight / fontLineHeight).toInt())
            emulator.resize(cols, rows)
            scrollOffset = scrollOffset.coerceIn(0, emulator.getScrollbackSize())
            onResize?.invoke(cols, rows, availWidth, availHeight)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateTerminalSize()
    }

    fun appendData(data: ByteArray, length: Int) {
        emulator.processBytes(data, length)
        if (scrollOffset == 0) {
            onScrollStateChanged?.invoke(true)
        }
        postInvalidate()
    }

    fun jumpToBottom() {
        scrollOffset = 0
        onScrollStateChanged?.invoke(true)
        postInvalidate()
    }

    fun isScrolledToBottom(): Boolean = scrollOffset == 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFF0A0C14.toInt()) // Terminal Dark BG

        synchronized(emulator) {
            val cols = emulator.cols
            val rows = emulator.rows
            val fm = textPaint.fontMetrics
            val baseBaseline = -fm.top

            val historySize = emulator.getScrollbackSize()

            // Draw Visible Rows with Padding & Scrollback Support
            for (r in 0 until rows) {
                val y = padTopPx + (r * fontLineHeight)
                val baseline = y + baseBaseline

                val virtualRow = historySize - scrollOffset + r

                val cellRow = if (virtualRow in 0 until historySize) {
                    emulator.getScrollbackRow(virtualRow)
                } else null

                for (c in 0 until cols) {
                    val cell = if (cellRow != null) {
                        if (c < cellRow.size) cellRow[c] else TerminalCell.EMPTY
                    } else {
                        val realScreenRow = virtualRow - historySize
                        emulator.getCell(realScreenRow, c)
                    }

                    val x = padLeftPx + (c * fontCharWidth)
                    val fg = if (cell.inverse) cell.bgColor else cell.fgColor
                    val bg = if (cell.inverse) cell.fgColor else cell.bgColor

                    // Draw cell background if non-default
                    if (bg != 0xFF0A0C14.toInt()) {
                        bgPaint.color = bg
                        canvas.drawRect(x, y, x + fontCharWidth, y + fontLineHeight, bgPaint)
                    }

                    // Draw Text Glyph
                    if (cell.char != ' ' && cell.char != '\u0000') {
                        textPaint.color = fg
                        textPaint.isFakeBoldText = cell.bold
                        textPaint.isUnderlineText = cell.underline
                        canvas.drawText(cell.char.toString(), x, baseline, textPaint)
                    }
                }
            }

            // Draw Cursor
            if (emulator.cursorVisible && scrollOffset == 0 && emulator.cursorRow in 0 until rows && emulator.cursorCol in 0 until cols) {
                val cx = padLeftPx + (emulator.cursorCol * fontCharWidth)
                val cy = padTopPx + (emulator.cursorRow * fontLineHeight)
                canvas.drawRect(cx, cy, cx + fontCharWidth, cy + fontLineHeight, cursorPaint)

                // Redraw character under cursor with inverted color
                val cell = emulator.getCell(emulator.cursorRow, emulator.cursorCol)
                if (cell.char != ' ' && cell.char != '\u0000') {
                    textPaint.color = 0xFF000000.toInt()
                    textPaint.isFakeBoldText = cell.bold
                    val baseline = cy + baseBaseline
                    canvas.drawText(cell.char.toString(), cx, baseline, textPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val deltaY = event.y - lastTouchY
                    if (Math.abs(deltaY) > 8f) {
                        isDragging = true
                        val scrollRowDelta = (deltaY / fontLineHeight).toInt()
                        if (scrollRowDelta != 0) {
                            val maxScroll = emulator.getScrollbackSize()
                            val newScrollOffset = (scrollOffset + scrollRowDelta).coerceIn(0, maxScroll)
                            if (newScrollOffset != scrollOffset) {
                                scrollOffset = newScrollOffset
                                onScrollStateChanged?.invoke(scrollOffset == 0)
                                postInvalidate()
                            }
                            lastTouchY = event.y
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isDragging) {
                    requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
                isDragging = false
            }
        }
        return true
    }

    // Disable keyboard autocorrect and word suggestions for terminal input
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NONE

        return object : BaseInputConnection(this, false) {
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    onKeyDown(event.keyCode, event)
                }
                return true
            }

            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.let {
                    onSendBytes?.invoke(it.toString().toByteArray())
                }
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                onSendBytes?.invoke(byteArrayOf(127)) // Backspace / DEL
                return true
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val bytes = when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> "\r".toByteArray()
            KeyEvent.KEYCODE_DEL -> byteArrayOf(127)
            KeyEvent.KEYCODE_TAB -> byteArrayOf(9)
            KeyEvent.KEYCODE_ESCAPE -> byteArrayOf(27)
            KeyEvent.KEYCODE_DPAD_UP -> byteArrayOf(27, 91, 65)
            KeyEvent.KEYCODE_DPAD_DOWN -> byteArrayOf(27, 91, 66)
            KeyEvent.KEYCODE_DPAD_LEFT -> byteArrayOf(27, 91, 68)
            KeyEvent.KEYCODE_DPAD_RIGHT -> byteArrayOf(27, 91, 67)
            else -> {
                val unicodeChar = event.getUnicodeChar(event.metaState)
                if (unicodeChar > 0) {
                    unicodeChar.toChar().toString().toByteArray()
                } else null
            }
        }

        bytes?.let {
            onSendBytes?.invoke(it)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
