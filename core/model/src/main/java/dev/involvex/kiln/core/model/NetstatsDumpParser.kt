package dev.involvex.kiln.core.model

/**
 * Pure parser for `dumpsys netstats` / `--checkin` output.
 * Drops lines that may carry subscriberId / networkId / SSID.
 */
@Suppress("CyclomaticComplexMethod", "ComplexCondition", "LoopWithTooManyJumpStatements", "MagicNumber")
object NetstatsDumpParser {
    private const val DEFAULT_LIMIT = 8
    private const val APP_UID_MIN = 1000
    private val PRIVACY = Regex("""subscriberId|networkId|ssid|imsi""", RegexOption.IGNORE_CASE)

    fun parse(stdout: String, limit: Int = DEFAULT_LIMIT): NetstatsSnapshot? {
        if (stdout.isBlank() || stdout.startsWith("ERR:")) return null
        val safeLines = stdout.lineSequence()
            .filter { line -> !PRIVACY.containsMatchIn(line) }
            .toList()
        if (safeLines.isEmpty()) return null

        val checkin = parseCheckin(safeLines, limit)
        if (checkin != null && hasTraffic(checkin)) return checkin
        return parseDefaultDump(safeLines, limit) ?: checkin
    }

    private fun hasTraffic(snap: NetstatsSnapshot): Boolean =
        snap.wifiRxBytes + snap.wifiTxBytes + snap.mobileRxBytes + snap.mobileTxBytes > 0 ||
            snap.topUids.isNotEmpty()

    private fun parseCheckin(lines: List<String>, limit: Int): NetstatsSnapshot? {
        var wifiRx = 0L
        var wifiTx = 0L
        var mobileRx = 0L
        var mobileTx = 0L
        val byUid = LinkedHashMap<Int, LongArray>()

        for (raw in lines) {
            val line = raw.trim()
            if (!line.startsWith("t,")) continue
            val parts = line.split(',')
            if (parts.size < 6) continue
            val type = parts.getOrNull(1)?.lowercase().orEmpty()
            val ifaceHint = parts.drop(2).take(3).joinToString(",").lowercase()
            val kind = ifaceKind(ifaceHint)

            when (type) {
                "xt" -> {
                    val rx = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                    val tx = parts.getOrNull(6)?.toLongOrNull() ?: parts.getOrNull(7)?.toLongOrNull() ?: 0L
                    when (kind) {
                        IfaceKind.WIFI -> {
                            wifiRx += rx
                            wifiTx += tx
                        }

                        IfaceKind.MOBILE -> {
                            mobileRx += rx
                            mobileTx += tx
                        }

                        IfaceKind.OTHER -> Unit
                    }
                }

                "uid" -> accumulateCheckinUid(parts, byUid)
            }
        }

        return snapshotOrNull(wifiRx, wifiTx, mobileRx, mobileTx, byUid, limit)
    }

    private fun accumulateCheckinUid(parts: List<String>, byUid: MutableMap<Int, LongArray>) {
        val uid = parts.getOrNull(3)?.toIntOrNull()?.takeIf { it >= APP_UID_MIN }
            ?: parts.drop(3).firstNotNullOfOrNull { token ->
                token.toIntOrNull()?.takeIf { it >= APP_UID_MIN }
            }
            ?: return
        val uidIdx = parts.indexOfFirst { it == uid.toString() }.takeIf { it >= 0 } ?: return
        val byteCandidates = parts.drop(uidIdx + 1).mapNotNull { it.toLongOrNull() }
        if (byteCandidates.size < 3) return
        val rx = byteCandidates.getOrNull(2) ?: byteCandidates.getOrNull(0) ?: 0L
        val tx = byteCandidates.getOrNull(4) ?: byteCandidates.getOrNull(2) ?: 0L
        val bucket = byUid.getOrPut(uid) { longArrayOf(0L, 0L) }
        bucket[0] += rx
        bucket[1] += tx
    }

    private fun parseDefaultDump(lines: List<String>, limit: Int): NetstatsSnapshot? {
        var wifiRx = 0L
        var wifiTx = 0L
        var mobileRx = 0L
        var mobileTx = 0L
        val byUid = LinkedHashMap<Int, LongArray>()
        var currentUid: Int? = null
        var section = IfaceKind.OTHER

        val uidBlock = Regex("""uid[=:](\d+)""", RegexOption.IGNORE_CASE)
        val rxBytes = Regex("""(?:rxBytes|rb)=(\d+)""", RegexOption.IGNORE_CASE)
        val txBytes = Regex("""(?:txBytes|tb)=(\d+)""", RegexOption.IGNORE_CASE)

        for (raw in lines) {
            val line = raw.trim()
            section = detectSection(line.lowercase(), section)
            uidBlock.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { currentUid = it }
            val rx = rxBytes.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            val tx = txBytes.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            if (rx == 0L && tx == 0L && !line.contains("rxBytes", ignoreCase = true) &&
                !line.contains("rb=", ignoreCase = true)
            ) {
                continue
            }
            if (rx == 0L && tx == 0L) continue
            when (section) {
                IfaceKind.WIFI -> {
                    wifiRx += rx
                    wifiTx += tx
                }

                IfaceKind.MOBILE -> {
                    mobileRx += rx
                    mobileTx += tx
                }

                IfaceKind.OTHER -> Unit
            }
            val uid = currentUid
            if (uid != null && uid >= APP_UID_MIN) {
                val bucket = byUid.getOrPut(uid) { longArrayOf(0L, 0L) }
                bucket[0] += rx
                bucket[1] += tx
            }
        }

        return snapshotOrNull(wifiRx, wifiTx, mobileRx, mobileTx, byUid, limit)
    }

    private fun detectSection(lower: String, current: IfaceKind): IfaceKind = when {
        lower.contains("wifi") && (lower.contains("network") || lower.contains("iface") || lower.startsWith("ident")) ->
            IfaceKind.WIFI

        lower.contains("mobile") || lower.contains("rmnet") || lower.contains("telephony") ->
            IfaceKind.MOBILE

        else -> current
    }

    private fun ifaceKind(hint: String): IfaceKind = when {
        hint.contains("wifi") || hint.contains("wlan") -> IfaceKind.WIFI

        hint.contains("mobile") || hint.contains("rmnet") || hint.contains("telephony") ||
            hint.contains("cellular") -> IfaceKind.MOBILE

        else -> IfaceKind.OTHER
    }

    private fun snapshotOrNull(
        wifiRx: Long,
        wifiTx: Long,
        mobileRx: Long,
        mobileTx: Long,
        byUid: Map<Int, LongArray>,
        limit: Int,
    ): NetstatsSnapshot? {
        val top = byUid.entries
            .map { (uid, bytes) ->
                NetstatsUidRow(uid = uid, label = "uid:$uid", rxBytes = bytes[0], txBytes = bytes[1])
            }
            .sortedByDescending { it.totalBytes }
            .take(limit.coerceAtLeast(0))
        if (wifiRx == 0L && wifiTx == 0L && mobileRx == 0L && mobileTx == 0L && top.isEmpty()) {
            return null
        }
        return NetstatsSnapshot(
            wifiRxBytes = wifiRx,
            wifiTxBytes = wifiTx,
            mobileRxBytes = mobileRx,
            mobileTxBytes = mobileTx,
            topUids = top,
        )
    }

    private enum class IfaceKind { WIFI, MOBILE, OTHER }
}
