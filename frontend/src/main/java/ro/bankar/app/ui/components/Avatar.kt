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
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.shimmer
import ro.bankar.app.R

@Composable
fun Avatar(
    image: ByteArray?,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    contentDescription: Int = R.string.avatar,
    shimmer: Shimmer? = null
) {
    val desc = stringResource(contentDescription)
    val mod = modifier.size(size).run { if (shimmer != null) shimmer(shimmer) else this }
    if (image == null)
        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = desc, modifier = mod)
    else
        AsyncImage(
            model = image,
            contentDescription = desc,
            modifier = mod.clip(CircleShape)
        )
}