package ro.bankar.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.valentinilk.shimmer.shimmer
import ro.bankar.app.R

val NoImage = ByteArray(0)

@Composable
fun Avatar(
    image: ByteArray?,
    modifier: Modifier = Modifier,
    contentDescription: Int = R.string.avatar,
    size: Dp = 100.dp,
    nullIsLoading: Boolean = false
) {
    val mod = modifier.size(size).run { if (image == null && nullIsLoading) shimmer() else this }
    val desc = stringResource(contentDescription)
    if (image == null || image === NoImage)
        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = desc, modifier = mod)
    else
        AsyncImage(model = image, contentDescription = desc, modifier = mod.clip(CircleShape))
}