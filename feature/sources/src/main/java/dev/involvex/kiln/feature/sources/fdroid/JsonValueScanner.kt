package dev.involvex.kiln.feature.sources.fdroid

import java.io.Reader

/**
 * Minimal JSON scanner that can skip or capture the next value without building a tree.
 * Used to stream F-Droid index-v2.json (~70MB) without loading it into a String.
 */
internal class JsonValueScanner(private val reader: Reader) {
    private var peeked: Int = UNSET
    private val capture = StringBuilder()
    private var capturing = false

    fun peek(): Int {
        if (peeked == UNSET) {
            peeked = reader.read()
        }
        return peeked
    }

    fun nextChar(): Int {
        val c = if (peeked != UNSET) {
            val value = peeked
            peeked = UNSET
            value
        } else {
            reader.read()
        }
        if (capturing && c >= 0) {
            capture.append(c.toChar())
        }
        return c
    }

    fun skipWs() {
        while (true) {
            when (peek()) {
                ' '.code, '\n'.code, '\r'.code, '\t'.code -> nextChar()
                else -> return
            }
        }
    }

    fun expect(char: Char) {
        skipWs()
        val c = nextChar()
        require(c == char.code) { "Expected '$char' but was '${c.toCharOrEof()}'" }
    }

    fun tryConsume(char: Char): Boolean {
        skipWs()
        if (peek() == char.code) {
            nextChar()
            return true
        }
        return false
    }

    fun readString(): String {
        skipWs()
        expect('"')
        val out = StringBuilder()
        while (true) {
            when (val c = nextChar()) {
                -1 -> error("Unterminated string")
                '\\'.code -> out.append(readEscape())
                '"'.code -> return out.toString()
                else -> out.append(c.toChar())
            }
        }
    }

    fun skipValue() {
        skipWs()
        when (peek()) {
            '"'.code -> {
                readString()
            }

            '{'.code -> skipContainer('{', '}')

            '['.code -> skipContainer('[', ']')

            else -> skipPrimitive()
        }
    }

    fun readValueAsString(): String {
        skipWs()
        capture.setLength(0)
        capturing = true
        try {
            when (peek()) {
                '"'.code -> readString()
                '{'.code -> skipContainer('{', '}')
                '['.code -> skipContainer('[', ']')
                else -> skipPrimitive()
            }
            return capture.toString()
        } finally {
            capturing = false
        }
    }

    private fun skipContainer(open: Char, close: Char) {
        expect(open)
        var depth = 1
        var inString = false
        var escape = false
        while (depth > 0) {
            val c = nextChar()
            if (c < 0) error("Unterminated container")
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\'.code -> escape = true
                    c == '"'.code -> inString = false
                }
            } else {
                when (c) {
                    '"'.code -> inString = true
                    open.code -> depth++
                    close.code -> depth--
                }
            }
        }
    }

    private fun skipPrimitive() {
        while (true) {
            when (val c = peek()) {
                -1 -> return
                ','.code, '}'.code, ']'.code, ' '.code, '\n'.code, '\r'.code, '\t'.code -> return
                else -> nextChar()
            }
        }
    }

    private fun readEscape(): Char = when (val c = nextChar()) {
        '"'.code, '\\'.code, '/'.code -> c.toChar()

        'b'.code -> '\b'

        'f'.code -> '\u000c'

        'n'.code -> '\n'

        'r'.code -> '\r'

        't'.code -> '\t'

        'u'.code -> {
            var code = 0
            repeat(UNICODE_HEX_DIGITS) {
                val hex = nextChar()
                require(hex >= 0) { "Invalid unicode escape" }
                code = (code shl HEX_NIBBLE_BITS) + Character.digit(hex, HEX_RADIX)
            }
            code.toChar()
        }

        else -> error("Invalid escape")
    }

    private fun Int.toCharOrEof(): String = if (this < 0) "EOF" else this.toChar().toString()

    private companion object {
        const val UNSET = -2
        const val UNICODE_HEX_DIGITS = 4
        const val HEX_NIBBLE_BITS = 4
        const val HEX_RADIX = 16
    }
}
