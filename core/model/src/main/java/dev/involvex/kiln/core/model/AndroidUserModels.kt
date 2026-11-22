package dev.involvex.kiln.core.model

/** One Android user from `pm list users` (read-only census). */
data class AndroidUserInfo(val id: Int, val name: String, val running: Boolean)

/** Pure parser for `pm list users` stdout (unit-tested). */
object AndroidUserListParser {
    private val LINE = Regex(
        """UserInfo\{(\d+):([^:}]*):[^}]*\}(\s+running)?""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(stdout: String): List<AndroidUserInfo> {
        val seen = LinkedHashMap<Int, AndroidUserInfo>()
        stdout.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parseLine(it) }
            .forEach { user -> seen[user.id] = user }
        return seen.values.toList()
    }

    private fun parseLine(line: String): AndroidUserInfo? {
        val match = LINE.find(line) ?: return null
        val id = match.groupValues[1].toIntOrNull() ?: return null
        val name = match.groupValues[2].ifBlank { "User $id" }
        val running = match.groupValues[3].isNotBlank()
        return AndroidUserInfo(id = id, name = name, running = running)
    }
}
