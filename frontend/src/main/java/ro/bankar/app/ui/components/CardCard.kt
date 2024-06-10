package ro.bankar.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import ro.bankar.app.R
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.Currency
import ro.bankar.model.SBankCard

@Composable
fun CardCard(data: SBankCard?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    if (data != null && onClick != null)
        Surface(
            modifier = modifier,
            onClick = onClick,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraSmall
        ) { CardCardContent(data) }
    else
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraSmall
        ) { CardCardContent(data) }
}

@Composable
private fun CardCardContent(data: SBankCard?) {
    val shimmer = rememberShimmer(ShimmerBounds.View)

    Column(
        modifier = Modifier
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .width(IntrinsicSize.Min),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (data == null) Text(text = "Card name", color = Color.Transparent, modifier = Modifier.grayShimmer(shimmer))
        else Text(text = data.name, softWrap = false, overflow = TextOverflow.Ellipsis)
        Row(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_card_24),
                contentDescription = stringResource(R.string.card),
                modifier = Modifier.size(28.dp)
            )
            if (data == null) Text(
                text = stringResource(R.string.card_last_4_template, "1234"),
                fontWeight = FontWeight.Bold,
                color = Color.Transparent,
                modifier = Modifier.grayShimmer(shimmer),
            )
            else Text(text = stringResource(R.string.card_last_4_template, data.lastFour), fontWeight = FontWeight.Bold)
        }
    }

}

private val sampleCard1 = SBankCard(
    1, "My Card", null, "1234", null,
    null, null, null, 0.0, 0.0, Currency.ROMANIAN_LEU, emptyList()
)
private val sampleCard2 = SBankCard(
    1, "This is a Card with a really long name that shouldn't fit", null, "1234", null,
    null, null, null, 0.0, 0.0, Currency.EURO, emptyList()
)

@Composable
@Preview
private fun CardCardPreview() {
    AppTheme {
        CardCard(sampleCard1, onClick = {})
    }
}

@Composable
@Preview
private fun CardCardPreviewLoading() {
    AppTheme {
        CardCard(null)
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun CardCardPreviewDark() {
    AppTheme {
        CardCard(sampleCard2, onClick = {})
    }
}