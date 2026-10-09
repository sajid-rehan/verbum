package com.rehan.verbum

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

// 5 albums named with LRM + space repeated; space makes each name unique on the filesystem
// while LRM keeps the displayed label invisible in the Samsung Gallery widget
private const val LRM = "‎"
private val ALBUMS = (1..5).map { n -> (LRM + " ").repeat(n) }

object GalleryManager {

    fun saveImage(ctx: Context, imageBytes: ByteArray, filename: String, albumIndex: Int) {
        val album = ALBUMS[albumIndex]
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$album")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Failed to create MediaStore entry for $filename (album $albumIndex)")

        resolver.openOutputStream(uri)?.use { it.write(imageBytes) }
            ?: error("Failed to open output stream for $filename")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    fun deleteAllAlbumImages(ctx: Context) {
        val resolver = ctx.contentResolver
        val deleted = resolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
            arrayOf("vbm_3dca06b7_%"),
        )
        android.util.Log.d("GalleryManager", "Deleted $deleted old cards")
    }
}
