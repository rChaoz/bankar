package ro.bankar.app.ui.main.friend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ro.bankar.app.R
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.format
import ro.bankar.model.SPublicUserBase

@Composable
fun FriendCard(friend: SPublicUserBase, country: String, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        Avatar(image = friend.avatar)
        Column {
            Text(text = friend.fullName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "@${friend.tag}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$country\n" +
                        stringResource(R.string.joined_on, friend.joinDate.format(true)),
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline
            )
        }
    }
}