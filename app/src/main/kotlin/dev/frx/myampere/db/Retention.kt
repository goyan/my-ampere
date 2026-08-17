package dev.frx.myampere.db

const val RAW_RETENTION_MS = 24L * 3600 * 1000
const val DOWNSAMPLE_BUCKET_MS = 60_000L
const val PURGE_AFTER_MS = 30L * 24 * 3600 * 1000

fun selectDownsampleDeletions(timestamps: List<Long>, bucketMs: Long): List<Long> {
    val keep = mutableSetOf<Long>()
    val seen = mutableSetOf<Long>()
    for (ts in timestamps.sorted()) {
        val bucket = ts / bucketMs
        if (seen.add(bucket)) keep.add(ts)
    }
    return timestamps.filter { it !in keep }
}
