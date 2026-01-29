package com.audio.mp3cutter.ringtone.maker.utils

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.util.Locale

object FFmpegManager {
    private const val TAG = "FFmpegManager"

    interface FFmpegCallback {
        fun onProgress(percentage: Int)
        fun onSuccess(outputPath: String)
        fun onError(errorMessage: String)
    }

    /**
     * Trims the audio to keep only the selected range [startMs, endMs]. Uses re-encoding for
     * precision.
     */
    fun executeTrim(
            inputPath: String,
            outputPath: String,
            startMs: Long,
            endMs: Long,
            durationMs: Long,
            callback: FFmpegCallback
    ) {
        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0

        // Command: -ss [start] -i [input] -t [duration] -c:a libmp3lame -q:a 2 [output]
        // Using -ss before -i is faster (seeking) but less precise.
        // Using -ss after -i is slower (decoding) but precise. We want precision.
        // Actually, for mp3, -ss before -i is often precise enough if we re-encode,
        // but let's put it before for speed and combine with re-encoding to fix timestamps.

        // Better command for precision:
        // -i input -ss start -t duration -c:a libmp3lame -b:a 192k output

        val cmd =
                "-i \"$inputPath\" -ss ${String.format(Locale.US, "%.3f", startSec)} -t ${String.format(Locale.US, "%.3f", durationSec)} -c:a libmp3lame -b:a 192k \"$outputPath\""

        executeCommand(cmd, outputPath, durationMs, callback)
    }

    /**
     * Cuts out the selected range [cutStartMs, cutEndMs] (keeps before and after). Concatenates the
     * two parts.
     */
    fun executeCut(
            inputPath: String,
            outputPath: String,
            cutStartMs: Long,
            cutEndMs: Long,
            totalDurationMs: Long,
            callback: FFmpegCallback
    ) {
        val cutStartSec = cutStartMs / 1000.0
        val cutEndSec = cutEndMs / 1000.0

        // Complex filter to split and concat
        // [0:a]atrim=end=cutStart,asetpts=PTS-STARTPTS[p1];
        // [0:a]atrim=start=cutEnd,asetpts=PTS-STARTPTS[p2];
        // [p1][p2]concat=n=2:v=0:a=1[out]

        val filter =
                "[0:a]atrim=end=${String.format(Locale.US, "%.3f", cutStartSec)},asetpts=PTS-STARTPTS[p1];" +
                        "[0:a]atrim=start=${String.format(Locale.US, "%.3f", cutEndSec)},asetpts=PTS-STARTPTS[p2];" +
                        "[p1][p2]concat=n=2:v=0:a=1[out]"

        val cmd =
                "-i \"$inputPath\" -filter_complex \"$filter\" -map \"[out]\" -c:a libmp3lame -b:a 192k \"$outputPath\""

        executeCommand(cmd, outputPath, totalDurationMs, callback)
    }

    /**
     * Changes the volume of the audio. [volumeMultiplier]: 1.0 is original, 0.5 is half, 2.0 is
     * double.
     */
    fun executeVolume(
            inputPath: String,
            outputPath: String,
            volumeMultiplier: Float,
            durationMs: Long,
            callback: FFmpegCallback
    ) {
        // Filter: volume=1.5
        val cmd =
                "-i \"$inputPath\" -filter:a \"volume=$volumeMultiplier\" -c:a libmp3lame -b:a 192k \"$outputPath\""

        executeCommand(cmd, outputPath, durationMs, callback)
    }

    /** Changes the speed of the audio (tempo), preserving pitch. [speedMultiplier]: 0.5 to 2.0. */
    fun executeSpeed(
            inputPath: String,
            outputPath: String,
            speedMultiplier: Float,
            durationMs: Long,
            callback: FFmpegCallback
    ) {
        // Filter: atempo=1.5
        // Note: atempo is limited to [0.5, 2.0]. For wider ranges, we'd need to chain them.
        // We will restrict UI to this range for simplicity.
        val safeSpeed = speedMultiplier.coerceIn(0.5f, 2.0f)

        val cmd =
                "-i \"$inputPath\" -filter:a \"atempo=$safeSpeed\" -c:a libmp3lame -b:a 192k \"$outputPath\""

        // New duration will be roughly old_duration / speed
        val newDurationMs = (durationMs / safeSpeed).toLong()

        executeCommand(cmd, outputPath, newDurationMs, callback)
    }

    /**
     * Applies fade in and/or fade out to the audio. [fadeInDurationMs]: Duration of fade in effect
     * (0 to skip) [fadeOutDurationMs]: Duration of fade out effect (0 to skip)
     */
    fun executeFade(
            inputPath: String,
            outputPath: String,
            fadeInDurationMs: Long,
            fadeOutDurationMs: Long,
            totalDurationMs: Long,
            callback: FFmpegCallback
    ) {
        val fadeInSec = fadeInDurationMs / 1000.0
        val fadeOutSec = fadeOutDurationMs / 1000.0
        val totalSec = totalDurationMs / 1000.0

        // Build filter chain
        val filters = mutableListOf<String>()

        // Fade in: afade=t=in:st=0:d=<duration>
        if (fadeInDurationMs > 0) {
            filters.add("afade=t=in:st=0:d=${String.format(Locale.US, "%.3f", fadeInSec)}")
        }

        // Fade out: afade=t=out:st=<start>:d=<duration>
        // Start time = total duration - fade out duration
        if (fadeOutDurationMs > 0) {
            val fadeOutStart = totalSec - fadeOutSec
            filters.add(
                    "afade=t=out:st=${String.format(Locale.US, "%.3f", fadeOutStart)}:d=${String.format(Locale.US, "%.3f", fadeOutSec)}"
            )
        }

        if (filters.isEmpty()) {
            callback.onError("No fade effect specified")
            return
        }

        val filterString = filters.joinToString(",")
        val cmd =
                "-i \"$inputPath\" -af \"$filterString\" -c:a libmp3lame -b:a 192k \"$outputPath\""

        executeCommand(cmd, outputPath, totalDurationMs, callback)
    }

    /**
     * Merges multiple audio files into one by concatenating them in order. Uses filter_complex with
     * concat filter for reliable merging across formats. [inputPaths]: List of audio file paths to
     * merge in order [totalDurationMs]: Sum of all input file durations for progress tracking
     */
    fun executeMerge(
            inputPaths: List<String>,
            outputPath: String,
            totalDurationMs: Long,
            callback: FFmpegCallback
    ) {
        if (inputPaths.size < 2) {
            callback.onError("At least 2 audio files required for merging")
            return
        }

        // Build FFmpeg command with multiple inputs
        val inputsCmd = inputPaths.joinToString(" ") { "-i \"$it\"" }

        // Build filter_complex for concat
        // [0:a][1:a][2:a]...concat=n=N:v=0:a=1[out]
        val streamLabels = inputPaths.indices.joinToString("") { "[$it:a]" }
        val filter = "${streamLabels}concat=n=${inputPaths.size}:v=0:a=1[out]"

        val cmd =
                "$inputsCmd -filter_complex \"$filter\" -map \"[out]\" -c:a libmp3lame -b:a 192k \"$outputPath\""

        executeCommand(cmd, outputPath, totalDurationMs, callback)
    }

    /**
     * Exports audio to specified format and quality. [format]: "mp3", "m4a" (AAC), or "wav"
     * [bitrate]: Bitrate in kbps (128, 192, 320). Ignored for WAV.
     */
    fun executeExport(
            inputPath: String,
            outputPath: String,
            format: String,
            bitrate: Int,
            durationMs: Long,
            callback: FFmpegCallback
    ) {
        val codecCmd =
                when (format.lowercase()) {
                    "mp3" -> "-c:a libmp3lame -b:a ${bitrate}k -write_xing 1"
                    "m4a", "aac" -> "-c:a aac -b:a ${bitrate}k"
                    "wav" -> "-c:a pcm_s16le"
                    else -> "-c:a libmp3lame -b:a ${bitrate}k -write_xing 1"
                }

        // -y flag to overwrite output file without asking
        // -vn to ignore video streams if any
        // -nostdin to prevent FFmpeg from trying to read stdin
        val cmd = "-y -nostdin -i \"$inputPath\" -vn $codecCmd \"$outputPath\""

        executeCommand(cmd, outputPath, durationMs, callback)
    }

    private fun executeCommand(
            cmd: String,
            outputPath: String,
            totalDurationMs: Long,
            callback: FFmpegCallback
    ) {
        Log.d(TAG, "Executing: $cmd")

        FFmpegKit.executeAsync(
                cmd,
                { session ->
                    val returnCode = session.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        // Verify output file exists and has content
                        val outputFile = java.io.File(outputPath)
                        if (outputFile.exists() && outputFile.length() > 0) {
                            Log.d(TAG, "Success - Output file size: ${outputFile.length()} bytes")
                            callback.onSuccess(outputPath)
                        } else {
                            Log.e(TAG, "Success but output file is empty or missing")
                            callback.onError("Output file is empty or missing")
                        }
                    } else {
                        Log.e(
                                TAG,
                                "Failed with state ${session.state} and rc ${session.returnCode}"
                        )
                        Log.e(TAG, session.failStackTrace ?: "No stack trace")
                        callback.onError("Failed to edit audio. Error: ${session.failStackTrace ?: "Unknown error"}")
                    }
                },
                { log ->
                    // Log callback
                    // Log.d(TAG, log.message)
                }
        ) { statistics ->
            // Statistics callback for progress
            val timeInMs = statistics.time
            if (timeInMs > 0 && totalDurationMs > 0) {
                val progress = ((timeInMs.toDouble() / totalDurationMs.toDouble()) * 100).toInt()
                callback.onProgress(progress.coerceIn(0, 100))
            }
        }
    }
}
