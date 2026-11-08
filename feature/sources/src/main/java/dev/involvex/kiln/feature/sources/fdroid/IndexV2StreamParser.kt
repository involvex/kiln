package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import timber.log.Timber

/**
 * Streams F-Droid index-v2.json and materializes packages without loading the full
 * ~70MB document as a String/Json tree.
 */
object IndexV2StreamParser {
    fun parseWantedPackages(input: InputStream, wantedPackages: Set<String>): Result<List<ParsedIndexPackage>> {
        if (wantedPackages.isEmpty()) {
            Timber.i("index stream: no wanted packages; skipping package materialization")
            return Result.success(emptyList())
        }
        return try {
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8), BUFFER_SIZE).use { reader ->
                Result.success(scanPackages(JsonValueScanner(reader), wantedPackages))
            }
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "index_parse_failed", error)
        } catch (error: IllegalStateException) {
            Result.failure(error.message ?: "index_parse_failed", error)
        } catch (error: OutOfMemoryError) {
            // Prefer a sync error over an uncaught crash after a huge index allocation.
            Result.failure("index_oom", error)
        }
    }

    /**
     * Streams every package and invokes [onPackage] for each.
     * Caller should keep only the suggested release to bound memory.
     */
    fun parseAllPackages(input: InputStream, onPackage: (ParsedIndexPackage) -> Unit): Result<Int> = try {
        BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8), BUFFER_SIZE).use { reader ->
            val count = scanAllPackages(JsonValueScanner(reader), onPackage)
            Timber.i("index stream: catalog materialization kept %d packages", count)
            Result.success(count)
        }
    } catch (error: IllegalArgumentException) {
        Result.failure(error.message ?: "index_parse_failed", error)
    } catch (error: IllegalStateException) {
        Result.failure(error.message ?: "index_parse_failed", error)
    } catch (error: OutOfMemoryError) {
        Result.failure("index_oom", error)
    }

    private fun scanPackages(scanner: JsonValueScanner, wanted: Set<String>): List<ParsedIndexPackage> {
        scanner.expect('{')
        val out = ArrayList<ParsedIndexPackage>(wanted.size.coerceAtMost(256))
        var first = true
        while (!scanner.tryConsume('}')) {
            if (!first) {
                scanner.expect(',')
            }
            first = false
            if (scanner.tryConsume('}')) break
            val key = scanner.readString()
            scanner.expect(':')
            if (key == "packages") {
                readPackagesObject(scanner, wanted, out)
            } else {
                scanner.skipValue()
            }
        }
        Timber.i("index stream: kept %d of %d wanted packages", out.size, wanted.size)
        return out
    }

    private fun scanAllPackages(scanner: JsonValueScanner, onPackage: (ParsedIndexPackage) -> Unit): Int {
        scanner.expect('{')
        var count = 0
        var first = true
        while (!scanner.tryConsume('}')) {
            if (!first) {
                scanner.expect(',')
            }
            first = false
            if (scanner.tryConsume('}')) break
            val key = scanner.readString()
            scanner.expect(':')
            if (key == "packages") {
                count = readAllPackagesObject(scanner, onPackage)
            } else {
                scanner.skipValue()
            }
        }
        return count
    }

    private fun readPackagesObject(scanner: JsonValueScanner, wanted: Set<String>, out: MutableList<ParsedIndexPackage>) {
        scanner.expect('{')
        var first = true
        while (!scanner.tryConsume('}')) {
            if (!first) {
                scanner.expect(',')
            }
            first = false
            if (scanner.tryConsume('}')) break
            val packageName = scanner.readString()
            scanner.expect(':')
            if (packageName in wanted) {
                val raw = scanner.readValueAsString()
                val parsed = IndexV2Parser.parsePackageJson(packageName, raw)
                if (parsed != null) {
                    out += parsed
                }
            } else {
                scanner.skipValue()
            }
            if (out.size >= wanted.size) {
                // All wanted packages found; skip the rest of this object then stop.
                drainRemainingObjectEntries(scanner)
                return
            }
        }
    }

    private fun readAllPackagesObject(scanner: JsonValueScanner, onPackage: (ParsedIndexPackage) -> Unit): Int {
        scanner.expect('{')
        var count = 0
        var first = true
        while (!scanner.tryConsume('}')) {
            if (!first) {
                scanner.expect(',')
            }
            first = false
            if (scanner.tryConsume('}')) {
                return count
            }
            val packageName = scanner.readString()
            scanner.expect(':')
            val raw = scanner.readValueAsString()
            val parsed = IndexV2Parser.parsePackageJson(packageName, raw)
            if (parsed != null) {
                // Bound memory: discovery only needs the newest APK metadata.
                onPackage(parsed.copy(releases = parsed.releases.take(1)))
                count++
            }
        }
        return count
    }

    private fun drainRemainingObjectEntries(scanner: JsonValueScanner) {
        while (!scanner.tryConsume('}')) {
            scanner.expect(',')
            if (scanner.tryConsume('}')) return
            scanner.readString()
            scanner.expect(':')
            scanner.skipValue()
        }
    }

    private const val BUFFER_SIZE = 64 * 1024
}
