package com.sal.privacykit.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale


class ProfileWallpaperManager (
    private val context: Context,
) {
    fun apply(profileId: Long) {
        val dir = File(context.filesDir, "profile_wallpapers")
        if (!dir.exists() && !dir.mkdirs()) {
            error("壁紙保存ディレクトリを作成できません: ${dir.absolutePath}")
        }
        val file = File(dir, "wallpaper_${profileId}.png")

        if (!file.exists()) {
            createWallpaperImage(file, profileId)
        }

        val wallpaperManager = WallpaperManager.getInstance(context)

        FileInputStream(file).use { input ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setStream(
                    input,
                    null,   // crop hint: Androidに表示範囲を任せる
                    false,  // allowBackup
                    WallpaperManager.FLAG_SYSTEM, // ホーム画面のみ
                )
            } else {
                wallpaperManager.setStream(input)
            }
        }
    }

    private fun createWallpaperImage(file: File, profileId: Long) {
        val profileLabel = String.format(Locale.US, "%02d", profileId)
        // 端末の一般的な縦長壁紙サイズ。必要なら実機の解像度に合わせて変更。
        val width = 1080
        val height = 2400

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        paint.textSize = width * 0.1f
        // 字形の中心が画面中央に来るよう、ベースラインを補正する。
        val posX = width / 2f
        val posY = height / 5f - paint.fontMetrics.top 

        canvas.drawText(profileLabel, posX, posY, paint)

        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        
        bitmap.recycle()
    }
}