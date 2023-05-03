package ro.bankar.model

import android.graphics.BitmapFactory

actual fun validateImage(imageData: ByteArray): Boolean {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(imageData, 0, imageData.size, opts)
    return opts.outWidth == SUserValidation.avatarSize && opts.outHeight == SUserValidation.avatarSize
}