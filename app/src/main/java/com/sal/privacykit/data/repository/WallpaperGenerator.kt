package com.sal.privacykit.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object WallpaperGenerator {

    /**
     * プロフィールIDに対応する壁紙を生成する。
     *
     * 例: profileId = 1
     *     /data/adb/privacykit/wallpaper_01
     *
     * 注意: /data/adb への書き込みには root 権限が必要。
     * この関数自体は root 権限を取得しない。
     */
    fun generate(profileId: Long): File {
        
        require(profileId > 0) { "Invalid profileId: $profileId" }

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
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        // 文字の高さを画像幅の約半分にする。
        // Paintのフォントサイズと実際の字形の高さは異なるため、
        // 描画後の字形高さを測り、フォントサイズを調整する。
        val targetTextHeight = width * 0.5f
        val bounds = Rect()

        paint.textSize = targetTextHeight
        paint.getTextBounds(profileLabel, 0, profileLabel.length, bounds)

        if (bounds.height() > 0) {
            paint.textSize *= targetTextHeight / bounds.height().toFloat()
            paint.getTextBounds(profileLabel, 0, profileLabel.length, bounds)
        }

        // 字形の中心が画面中央に来るよう、ベースラインを補正する。
        val centerX = width / 2f
        val centerY = height / 2f
        val baseline = centerY - (bounds.top + bounds.bottom) / 2f

        canvas.drawText(profileLabel, centerX, baseline, paint)

        

        val output = File(
            "/data/user/0/com.sal.privacykit.lite/cache",
            "wallpaper"
        )

        output.parentFile?.mkdirs()

        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        

        bitmap.recycle()
        return output
    }
}