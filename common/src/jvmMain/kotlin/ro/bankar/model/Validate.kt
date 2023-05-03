package ro.bankar.model

import javax.imageio.ImageIO

private val webpReader = ImageIO.getImageReadersByFormatName("webp").also { if (!it.hasNext()) throw RuntimeException("No WEBP image reader found") }.next()

actual fun validateImage(imageData: ByteArray) = try {
    ImageIO.createImageInputStream(imageData.inputStream()).use { stream ->
        webpReader.input = stream
        webpReader.read(0).let { it.width == SUserValidation.avatarSize && it.height == SUserValidation.avatarSize }
    }
} catch (e: Exception) {
    false
}