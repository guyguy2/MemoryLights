package com.happypuppy.memorylights.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a 1080x1080 PNG score card with native android.graphics primitives,
 * writes it to the cache dir, and fires ACTION_SEND. Native Canvas (not Compose)
 * keeps the rendering deterministic and side-effect free of recomposition timing.
 */
object ScoreShareHelper {

    private const val TAG = "ScoreShareHelper"
    private const val CARD_SIZE = 1080
    private const val FILE_NAME = "memory_lights_score.png"

    fun shareScore(
        context: Context,
        level: Int,
        bestScore: Int,
        is6ButtonMode: Boolean
    ) {
        try {
            val bitmap = renderCard(level, bestScore, is6ButtonMode)
            val uri = writeToCache(context, bitmap)
            bitmap.recycle()

            val mode = if (is6ButtonMode) "Memory Lights+" else "Memory Lights"
            val text = "I scored level $level in $mode! Best: $bestScore"

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                // clipData carries the read permission across the system chooser
                // (Sharesheet preview otherwise hits Permission Denial on the URI).
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(send, "Share your score").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share score", e)
        }
    }

    private fun writeToCache(context: Context, bitmap: Bitmap): Uri {
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(sharedDir, FILE_NAME)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun renderCard(level: Int, bestScore: Int, is6ButtonMode: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.BLACK)

        val accent = Color.parseColor("#4CAF50")
        val white = Color.WHITE
        val gray = Color.parseColor("#B0B0B0")

        val center = CARD_SIZE / 2f

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 64f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        canvas.drawText("MEMORY LIGHTS", center, 180f, brandPaint)

        // Six color discs across top to brand the card.
        val discs = listOf(
            Color.parseColor("#43A047"), // green
            Color.parseColor("#E53935"), // red
            Color.parseColor("#FDD835"), // yellow
            Color.parseColor("#1E88E5"), // blue
            Color.parseColor("#8E24AA"), // purple
            Color.parseColor("#FB8C00")  // orange
        )
        val discRadius = 28f
        val discSpacing = 96f
        val discY = 280f
        val discCount = if (is6ButtonMode) 6 else 4
        val rowWidth = discSpacing * (discCount - 1)
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until discCount) {
            discPaint.color = discs[i]
            canvas.drawCircle(center - rowWidth / 2 + discSpacing * i, discY, discRadius, discPaint)
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray
            textSize = 56f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.4f
        }
        canvas.drawText("LEVEL REACHED", center, 480f, labelPaint)

        val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 360f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(level.toString(), center, 760f, levelPaint)

        val bestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 56f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Best: $bestScore", center, 840f, bestPaint)

        val modeText = if (is6ButtonMode) "MEMORY LIGHTS+" else "STANDARD MODE"
        val modePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.3f
        }
        canvas.drawText(modeText, center, 940f, modePaint)

        // Subtle border so the all-black card has a visible edge on light themes.
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F1F1F")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(
            RectF(8f, 8f, CARD_SIZE - 8f, CARD_SIZE - 8f),
            32f, 32f, borderPaint
        )

        return bitmap
    }
}
