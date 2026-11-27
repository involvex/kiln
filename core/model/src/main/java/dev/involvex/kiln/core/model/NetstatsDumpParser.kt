package dev.involvex.kiln.core.model

/**
 * Pure parser for `dumpsys netstats` / `--checkin` output.
 * Redacts subscriberId / wifiNetworkKey; never surfaces them in snapshots.
 *
 * HyperOS checkin (compact):
 * ```
 * v1,…
 * xt
 * c,wifi,-1,all,0
 * d,3600
 * b,<start>,<rxBytes>,<rxPackets>,<txBytes>,<txPackets>,…
 * ```
 * Full dump also has `mAppUidStatsMap:` (`uid rxBytes …`) and Xt `rb=`/`tb=` history.
 */
@Suppress("CyclomaticComplexMethod", "ComplexCondition", "LoopWithTooManyJumpStatements", "MagicNumber")
object NetstatsDumpParser {
    private const val DEFAULT_LIMIT = 8
    private const val APP_UID_MIN = 1000
    private val SUBSCRIBER = Regex("""subscriberId=[^,\]\s]+""", RegexOption.IGNORE_CASE)
    private val WIFI_KEY = Regex("""wifiNetworkKey="[^"]*"""", RegexOption.IGNORE_CASE)
    private val NETWORK_ID = Regex("""networkId="[^"]*"""", RegexOption.IGNORE_CASE)
    private val IMSI = Regex("""imsi=[^,\]\s]+""", RegexOption.IGNORE_CASE)

    fun parse(stdout: String, limit: Int = DEFAULT_LIMIT): NetstatsSnapshot? {
        if (stdout.isBlank() || stdout.startsWith("ERR:")) return null
        val lines = stdout.lineSequence()
            .map { redactPrivacy(it) }
            .toList()
        if (lines.isEmpty()) return null

        val hyperOs = parseHyperOsCheckin(lines, limit)
        if (hyperOs != null && hasTraffic(hyperOs)) return mergeUidTable(hyperOs, lines, limit)

        val legacy = parseLegacyCheckin(lines, limit)
        if (legacy != null && hasTraffic(legacy)) return mergeUidTable(legacy, lines, limit)

        val xt = parseXtHistory(lines, limit)
        if (xt != null && hasTraffic(xt)) return mergeUidTable(xt, lines, limit)

        val uidOnly = parseUidStatsMap(lines, limit)
        return uidOnly ?: hyperOs ?: legacy ?: xt
    }

    private fun hasTraffic(snap: NetstatsSnapshot): Boolean =
        snap.wifiRxBytes + snap.wifiTxBytes + snap.mobileRxBytes + snap.mobileTxBytes > 0 ||
            snap.topUids.isNotEmpty()

    private fun redactPrivacy(line: String): String = line
        .replace(SUBSCRIBER, "subscriberId=REDACTED")
        .replace(WIFI_KEY, "wifiNetworkKey=REDACTED")
        .replace(NETWORK_ID, "networkId=REDACTED")
        .replace(IMSI, "imsi=REDACTED")

    private fun mergeUidTable(base: NetstatsSnapshot, lines: List<String>, limit: Int): NetstatsSnapshot {
        if (base.topUids.isNotEmpty()) return base
        val uids = parseUidStatsMap(lines, limit) ?: return base
        return base.copy(topUids = uids.topUids)
    }

    /** HyperOS / modern Connectivity: `c,wifi` then `b,start,rxB,rxP,txB,txP,…`. */
    private fun parseHyperOsCheckin(lines: List<String>, limit: Int): NetstatsSnapshot? {
        var wifiRx = 0L
        var wifiTx = 0L
        var mobileRx = 0L
        var mobileTx = 0L
        var kind = IfaceKind.OTHER
        var sawBucket = false

        for (raw in lines) {
            val line = raw.trim()
            when {
                line.startsWith("c,") -> {
                    val hint = line.lowercase()
                    kind = when {
                        hint.contains("wifi") || hint.contains("wlan") -> IfaceKind.WIFI

                        hint.contains("mobile") || hint.contains("rmnet") ||
                            hint.contains("cellular") || hint.contains("telephony") -> IfaceKind.MOBILE

                        else -> IfaceKind.OTHER
                    }
                }

                line.startsWith("b,") -> {
                    val parts = line.split(',')
                    // b,<start>,<rxBytes>,<rxPackets>,<txBytes>,<txPackets>,…
                    if (parts.size < 6) continue
                    val rx = parts[2].toLongOrNull() ?: continue
                    val tx = parts[4].toLongOrNull() ?: continue
                    sawBucket = true
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

                line.equals("xt", ignoreCase = true) ||
                    line.equals("uid", ignoreCase = true) ||
                    line.startsWith("v1,") -> Unit
            }
        }
        if (!sawBucket) return null
        return snapshotOrNull(wifiRx, wifiTx, mobileRx, mobileTx, emptyMap(), limit)
    }

    /** Older AOSP checkin: `t,xt,…` / `t,uid,…`. */
    private fun parseLegacyCheckin(lines: List<String>, limit: Int): NetstatsSnapshot? {
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

                "uid" -> accumulateLegacyUid(parts, byUid)
            }
        }

        return snapshotOrNull(wifiRx, wifiTx, mobileRx, mobileTx, byUid, limit)
    }

    private fun accumulateLegacyUid(parts: List<String>, byUid: MutableMap<Int, LongArray>) {
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

    /** Full dump Xt history: `ident=[{type=0|1…}]` then `st=… rb=… tb=…`. */
    private fun parseXtHistory(lines: List<String>, limit: Int): NetstatsSnapshot? {
        var wifiRx = 0L
        var wifiTx = 0L
        var mobileRx = 0L
        var mobileTx = 0L
        var kind = IfaceKind.OTHER
        var saw = false
        val rbRe = Regex("""\brb=(\d+)""")
        val tbRe = Regex("""\btb=(\d+)""")

        for (raw in lines) {
            val line = raw.trim()
            if (line.contains("ident=", ignoreCase = true) && line.contains("type=")) {
                kind = when {
                    Regex("""type=1\b""").containsMatchIn(line) ||
                        line.contains("transports={1}") -> IfaceKind.WIFI

                    Regex("""type=0\b""").containsMatchIn(line) ||
                        line.contains("transports={0}") -> IfaceKind.MOBILE

                    else -> IfaceKind.OTHER
                }
            }
            val rx = rbRe.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
            val tx = tbRe.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (rx == null && tx == null) continue
            saw = true
            when (kind) {
                IfaceKind.WIFI -> {
                    wifiRx += rx ?: 0L
                    wifiTx += tx ?: 0L
                }

                IfaceKind.MOBILE -> {
                    mobileRx += rx ?: 0L
                    mobileTx += tx ?: 0L
                }

                IfaceKind.OTHER -> Unit
            }
        }
        if (!saw) return null
        return snapshotOrNull(wifiRx, wifiTx, mobileRx, mobileTx, emptyMap(), limit)
    }

    /** `mAppUidStatsMap:` table: `uid rxBytes rxPackets txBytes txPackets`. */
    private fun parseUidStatsMap(lines: List<String>, limit: Int): NetstatsSnapshot? {
        val headerIdx = lines.indexOfFirst { line ->
            val t = line.trim()
            t.startsWith("uid ") && t.contains("rxBytes") && t.contains("txBytes")
        }
        if (headerIdx < 0) return null
        val byUid = LinkedHashMap<Int, LongArray>()
        for (line in lines.drop(headerIdx + 1)) {
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size < 5) {
                if (byUid.isNotEmpty()) break
                continue
            }
            val uid = parts[0].toIntOrNull() ?: break
            if (uid < APP_UID_MIN) continue
            val rx = parts[1].toLongOrNull() ?: continue
            val tx = parts[3].toLongOrNull() ?: continue
            val bucket = byUid.getOrPut(uid) { longArrayOf(0L, 0L) }
            bucket[0] += rx
            bucket[1] += tx
        }
        return snapshotOrNull(0L, 0L, 0L, 0L, byUid, limit)
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
