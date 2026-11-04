package com.herwin.netez.ui.components.terminal

import android.graphics.Color
import java.util.ArrayDeque

data class TerminalCell(
    var char: Char = ' ',
    var fgColor: Int = DEFAULT_FG,
    var bgColor: Int = DEFAULT_BG,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var inverse: Boolean = false
) {
    fun reset() {
        char = ' '
        fgColor = DEFAULT_FG
        bgColor = DEFAULT_BG
        bold = false
        italic = false
        underline = false
        inverse = false
    }

    fun copyFrom(other: TerminalCell) {
        char = other.char
        fgColor = other.fgColor
        bgColor = other.bgColor
        bold = other.bold
        italic = other.italic
        underline = other.underline
        inverse = other.inverse
    }

    companion object {
        const val DEFAULT_FG = 0xFFE5E7EB.toInt() // Light Gray / Off-white
        const val DEFAULT_BG = 0xFF0A0C14.toInt() // Dark Background
        val EMPTY = TerminalCell()
    }
}

class TerminalEmulator(
    var cols: Int = 80,
    var rows: Int = 24,
    val maxScrollback: Int = 10000
) {
    // Buffers
    private var mainBuffer = Array(rows) { Array(cols) { TerminalCell() } }
    private var altBuffer = Array(rows) { Array(cols) { TerminalCell() } }
    val scrollbackHistory = ArrayDeque<Array<TerminalCell>>(maxScrollback)

    private var currentBuffer = mainBuffer
    var isAltScreen = false
        private set

    // Cursor position
    var cursorRow = 0
    var cursorCol = 0
    var savedCursorRow = 0
    var savedCursorCol = 0
    var cursorVisible = true

    // Current Style Attributes
    private var currentFg = TerminalCell.DEFAULT_FG
    private var currentBg = TerminalCell.DEFAULT_BG
    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isInverse = false

    // ANSI ESC State Machine
    private var escapeState = 0 // 0: Normal, 1: ESC, 2: CSI (\e[), 3: OSC (\e])
    private val csiParams = StringBuilder()

    // 256 Color Palette cache
    private val palette256 = IntArray(256)

    init {
        initPalette()
    }

    private fun initPalette() {
        // Standard 16 ANSI colors
        val standard = intArrayOf(
            0xFF0A0C14.toInt(), 0xFFEF4444.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(),
            0xFF3B82F6.toInt(), 0xFFEC4899.toInt(), 0xFF06B6D4.toInt(), 0xFFE5E7EB.toInt(),
            0xFF6B7280.toInt(), 0xFFF87171.toInt(), 0xFF34D399.toInt(), 0xFFFBBF24.toInt(),
            0xFF60A5FA.toInt(), 0xFFF472B6.toInt(), 0xFF38BDF8.toInt(), 0xFFFFFFFF.toInt()
        )
        for (i in 0..15) {
            palette256[i] = standard[i]
        }
        // 216 Color Cube (16..231)
        val steps = intArrayOf(0, 95, 135, 175, 215, 255)
        var index = 16
        for (r in steps) {
            for (g in steps) {
                for (b in steps) {
                    palette256[index++] = Color.rgb(r, g, b)
                }
            }
        }
        // 24 Grayscale steps (232..255)
        for (i in 0..23) {
            val v = 8 + i * 10
            palette256[index++] = Color.rgb(v, v, v)
        }
    }

    @Synchronized
    fun resize(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return
        val validCols = Math.max(4, newCols)
        val validRows = Math.max(4, newRows)

        val newMain = Array(validRows) { r ->
            Array(validCols) { c ->
                if (r < mainBuffer.size && c < mainBuffer[r].size) mainBuffer[r][c] else TerminalCell()
            }
        }
        val newAlt = Array(validRows) { r ->
            Array(validCols) { c ->
                if (r < altBuffer.size && c < altBuffer[r].size) altBuffer[r][c] else TerminalCell()
            }
        }

        cols = validCols
        rows = validRows
        mainBuffer = newMain
        altBuffer = newAlt
        currentBuffer = if (isAltScreen) altBuffer else mainBuffer

        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
    }

    @Synchronized
    fun processBytes(data: ByteArray, length: Int) {
        for (i in 0 until length) {
            val b = data[i].toInt() and 0xFF
            val ch = b.toChar()

            when (escapeState) {
                0 -> { // Normal Text Mode
                    when (ch) {
                        '\u001B' -> escapeState = 1
                        '\r' -> cursorCol = 0
                        '\n' -> lineFeed()
                        '\b' -> if (cursorCol > 0) cursorCol--
                        '\t' -> {
                            cursorCol = (cursorCol + 8) and 7.inv()
                            if (cursorCol >= cols) cursorCol = cols - 1
                        }
                        '\u0007' -> { /* Bell - ignore */ }
                        else -> {
                            if (b >= 32) {
                                putChar(ch)
                            }
                        }
                    }
                }
                1 -> { // ESC Received
                    when (ch) {
                        '[' -> {
                            escapeState = 2
                            csiParams.clear()
                        }
                        ']' -> {
                            escapeState = 3
                            csiParams.clear()
                        }
                        '7' -> { // Save Cursor (DECSC)
                            savedCursorRow = cursorRow
                            savedCursorCol = cursorCol
                            escapeState = 0
                        }
                        '8' -> { // Restore Cursor (DECRC)
                            cursorRow = savedCursorRow.coerceIn(0, rows - 1)
                            cursorCol = savedCursorCol.coerceIn(0, cols - 1)
                            escapeState = 0
                        }
                        'E' -> { // Next Line
                            cursorCol = 0
                            lineFeed()
                            escapeState = 0
                        }
                        'M' -> { // Reverse Line Feed
                            if (cursorRow > 0) cursorRow--
                            escapeState = 0
                        }
                        'c' -> { // Reset Terminal
                            resetTerminal()
                            escapeState = 0
                        }
                        else -> escapeState = 0
                    }
                }
                2 -> { // CSI (\e[) Parameters Mode
                    if (ch in '0'..'9' || ch == ';' || ch == '?' || ch == ':') {
                        csiParams.append(ch)
                    } else {
                        parseCsiCommand(ch, csiParams.toString())
                        escapeState = 0
                    }
                }
                3 -> { // OSC (\e]) Mode
                    if (ch == '\u0007' || (ch == '\\' && csiParams.endsWith("\u001B"))) {
                        escapeState = 0
                    } else {
                        csiParams.append(ch)
                    }
                }
            }
        }
    }

    private fun putChar(ch: Char) {
        if (cursorCol >= cols) {
            cursorCol = 0
            lineFeed()
        }
        val cell = currentBuffer[cursorRow][cursorCol]
        cell.char = ch
        cell.fgColor = currentFg
        cell.bgColor = currentBg
        cell.bold = isBold
        cell.italic = isItalic
        cell.underline = isUnderline
        cell.inverse = isInverse
        cursorCol++
    }

    private fun lineFeed() {
        if (cursorRow < rows - 1) {
            cursorRow++
        } else {
            // Scroll Up
            if (!isAltScreen) {
                if (scrollbackHistory.size >= maxScrollback) {
                    scrollbackHistory.removeFirst()
                }
                val topRow = Array(cols) { TerminalCell().apply { copyFrom(currentBuffer[0][it]) } }
                scrollbackHistory.addLast(topRow)
            }
            for (r in 0 until rows - 1) {
                for (c in 0 until cols) {
                    currentBuffer[r][c].copyFrom(currentBuffer[r + 1][c])
                }
            }
            for (c in 0 until cols) {
                currentBuffer[rows - 1][c].reset()
            }
        }
    }

    private fun parseCsiCommand(commandChar: Char, paramStr: String) {
        val isPrivate = paramStr.startsWith("?")
        val cleanParamStr = if (isPrivate) paramStr.substring(1) else paramStr
        val args = if (cleanParamStr.isEmpty()) emptyList() else cleanParamStr.split(";").mapNotNull { it.toIntOrNull() }

        when (commandChar) {
            'm' -> parseSgr(args)
            'H', 'f' -> {
                val r = (args.getOrNull(0) ?: 1) - 1
                val c = (args.getOrNull(1) ?: 1) - 1
                cursorRow = r.coerceIn(0, rows - 1)
                cursorCol = c.coerceIn(0, cols - 1)
            }
            'A' -> { // Up
                val n = args.getOrNull(0) ?: 1
                cursorRow = (cursorRow - n).coerceIn(0, rows - 1)
            }
            'B' -> { // Down
                val n = args.getOrNull(0) ?: 1
                cursorRow = (cursorRow + n).coerceIn(0, rows - 1)
            }
            'C' -> { // Right
                val n = args.getOrNull(0) ?: 1
                cursorCol = (cursorCol + n).coerceIn(0, cols - 1)
            }
            'D' -> { // Left
                val n = args.getOrNull(0) ?: 1
                cursorCol = (cursorCol - n).coerceIn(0, cols - 1)
            }
            'J' -> { // Erase Display
                val mode = args.getOrNull(0) ?: 0
                when (mode) {
                    0 -> { // From cursor to end of screen
                        for (c in cursorCol until cols) currentBuffer[cursorRow][c].reset()
                        for (r in cursorRow + 1 until rows) {
                            for (c in 0 until cols) currentBuffer[r][c].reset()
                        }
                    }
                    1 -> { // From start of screen to cursor
                        for (r in 0 until cursorRow) {
                            for (c in 0 until cols) currentBuffer[r][c].reset()
                        }
                        for (c in 0..cursorCol) currentBuffer[cursorRow][c].reset()
                    }
                    2, 3 -> { // Entire Screen
                        for (r in 0 until rows) {
                            for (c in 0 until cols) currentBuffer[r][c].reset()
                        }
                    }
                }
            }
            'K' -> { // Erase Line
                val mode = args.getOrNull(0) ?: 0
                when (mode) {
                    0 -> for (c in cursorCol until cols) currentBuffer[cursorRow][c].reset()
                    1 -> for (c in 0..cursorCol) currentBuffer[cursorRow][c].reset()
                    2 -> for (c in 0 until cols) currentBuffer[cursorRow][c].reset()
                }
            }
            'h' -> { // Set Mode
                if (isPrivate) {
                    when (args.getOrNull(0)) {
                        25 -> cursorVisible = true
                        47, 1049 -> {
                            isAltScreen = true
                            currentBuffer = altBuffer
                        }
                    }
                }
            }
            'l' -> { // Reset Mode
                if (isPrivate) {
                    when (args.getOrNull(0)) {
                        25 -> cursorVisible = false
                        47, 1049 -> {
                            isAltScreen = false
                            currentBuffer = mainBuffer
                        }
                    }
                }
            }
        }
    }

    private fun parseSgr(args: List<Int>) {
        if (args.isEmpty() || (args.size == 1 && args[0] == 0)) {
            currentFg = TerminalCell.DEFAULT_FG
            currentBg = TerminalCell.DEFAULT_BG
            isBold = false
            isItalic = false
            isUnderline = false
            isInverse = false
            return
        }

        var idx = 0
        while (idx < args.size) {
            when (val code = args[idx]) {
                0 -> {
                    currentFg = TerminalCell.DEFAULT_FG
                    currentBg = TerminalCell.DEFAULT_BG
                    isBold = false
                    isItalic = false
                    isUnderline = false
                    isInverse = false
                }
                1 -> isBold = true
                2 -> isBold = false // Dim
                3 -> isItalic = true
                4 -> isUnderline = true
                7 -> isInverse = true
                22 -> isBold = false
                23 -> isItalic = false
                24 -> isUnderline = false
                27 -> isInverse = false

                // 16 ANSI Standard Colors (30..37 FG, 40..47 BG, 90..97 FG Bright, 100..107 BG Bright)
                in 30..37 -> currentFg = palette256[code - 30]
                in 40..47 -> currentBg = palette256[code - 40]
                in 90..97 -> currentFg = palette256[code - 90 + 8]
                in 100..107 -> currentBg = palette256[code - 100 + 8]

                39 -> currentFg = TerminalCell.DEFAULT_FG
                49 -> currentBg = TerminalCell.DEFAULT_BG

                // 256 / TrueColor Extended SGR (38 / 48)
                38, 48 -> {
                    val isFg = (code == 38)
                    if (idx + 1 < args.size) {
                        val mode = args[idx + 1]
                        if (mode == 5 && idx + 2 < args.size) { // 256 Colors
                            val colorIdx = args[idx + 2].coerceIn(0, 255)
                            val col = palette256[colorIdx]
                            if (isFg) currentFg = col else currentBg = col
                            idx += 2
                        } else if (mode == 2 && idx + 4 < args.size) { // TrueColor RGB
                            val r = args[idx + 2].coerceIn(0, 255)
                            val g = args[idx + 3].coerceIn(0, 255)
                            val b = args[idx + 4].coerceIn(0, 255)
                            val col = Color.rgb(r, g, b)
                            if (isFg) currentFg = col else currentBg = col
                            idx += 4
                        }
                    }
                }
            }
            idx++
        }
    }

    @Synchronized
    fun getScrollbackSize(): Int {
        return scrollbackHistory.size
    }

    @Synchronized
    fun getScrollbackRow(index: Int): Array<TerminalCell>? {
        if (index in 0 until scrollbackHistory.size) {
            return scrollbackHistory.elementAt(index)
        }
        return null
    }

    @Synchronized
    fun getCell(row: Int, col: Int): TerminalCell {
        if (row in 0 until rows) {
            val r = currentBuffer.getOrNull(row)
            if (r != null && col in 0 until r.size) {
                return r[col]
            }
        }
        return TerminalCell()
    }

    @Synchronized
    fun resetTerminal() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                mainBuffer[r][c].reset()
                altBuffer[r][c].reset()
            }
        }
        cursorRow = 0
        cursorCol = 0
        isAltScreen = false
        currentBuffer = mainBuffer
        scrollbackHistory.clear()
    }
}
