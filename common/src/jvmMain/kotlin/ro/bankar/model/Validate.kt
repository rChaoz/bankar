package ro.bankar.model

import javax.imageio.ImageIO

private val jpgReader = ImageIO.getImageReadersByFormatName("jpg").also { if (!it.hasNext()) throw RuntimeException("No JPG image reader found") }.next()

actual fun validateImage(imageData: ByteArray) = try {
    ImageIO.createImageInputStream(imageData.inputStream()).use { stream ->
        jpgReader.input = stream
        jpgReader.read(0).let {
            it.width == SUserValidation.avatarSize && it.height == SUserValidation.avatarSize
        }
    }
} catch (e: Exception) {
    false
}