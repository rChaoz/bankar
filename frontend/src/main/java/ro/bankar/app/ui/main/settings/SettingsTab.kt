package ro.bankar.app.ui.main.settings

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreIntSettingState
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import ro.bankar.app.R
import ro.bankar.app.ui.theme.AppTheme

private enum class SettingsNavigation(val route: String){
    Language("language"),
    Theme("theme"),
    Access("access"),
    Contact("contact"),
    PrimaryCurrency("primary_currency"),
    DefaultBankAccount("default_bank_account");

    companion object{
        const val route = "settings"
    }

}
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsTab() {
    val navigation = rememberAnimatedNavController()
    AnimatedNavHost(navController = navigation, startDestination = SettingsNavigation.route){
        composable(SettingsNavigation.route){
            SettingsScreen(navigation)
        }
        composable(SettingsNavigation.Language.route){
            LanguageScreen()
        }
        composable(SettingsNavigation.Theme.route){
            ThemeScreen()
        }
        composable(SettingsNavigation.Access.route){
            AccessScreen()
        }
        composable(SettingsNavigation.Contact.route){
            ContactScreen()
        }
        composable(SettingsNavigation.PrimaryCurrency.route){
            PrimaryCurrencyScreen()
        }
        composable(SettingsNavigation.DefaultBankAccount.route){
            DefaultBankAccountScreen()
        }
    }
    
}

private enum class Language(val text: Int) {
    SystemDefault(R.string.system_default_language),
    Romanian(R.string.romanian),
    English(R.string.english);
}
@Composable
fun LanguageScreen() {
    val state = rememberPreferenceDataStoreIntSettingState(key = "sloboz")

    Column {
        for(language in Language.values()){
            if(language.ordinal != 0){
                Divider()
            }
            SettingsMenuLink(

                //icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "EN") },
                title = { Text(text = stringResource(id = language.text), style = MaterialTheme.typography.titleMedium) },
                action = if (state.value==language.ordinal){
                    {Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {state.value=language.ordinal},
            )
        }
    }
}

@Composable
fun AccessScreen() {
    Column {
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default., contentDescription = "PIN") },
            title = { Text(text = "PIN", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Password") },
            title = { Text(text = stringResource(id = R.string.password), style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Fingerprint") },
            title = { Text(text = stringResource(id = R.string.fingerprint), style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
    }
}
@Composable
fun ThemeScreen(){
    Column {
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Password") },
            title = { Text(text = "Light", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Fingerprint") },
            title = { Text(text = "Dark", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
    }
}

@Composable
fun ContactScreen() {
    Column {
        SettingsMenuLink(
            title = { Text(text = "About", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = "FAQ", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = "Send e-mail", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = "Live Chat", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = "Call us", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
    }
}
@Composable
fun PrimaryCurrencyScreen(){
    Column {
        SettingsMenuLink(
            title = { Text(text = "EUR", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = "USD", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = "RON", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
    }
}

@Composable
fun DefaultBankAccountScreen(){
    SettingsMenuLink(
        //icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Fingerprint") },
        title = { Text(text = "Dark", style = MaterialTheme.typography.titleMedium) },
        onClick = {/*TODO*/},
    )
}

@Composable
fun SettingsScreen(navigation: NavHostController) {

    Column {
        SettingsMenuLink(

            icon = { Icon(painter = painterResource(R.drawable.baseline_language_24), contentDescription = "Language") },
            title = { Text(text = stringResource(id = R.string.language), style = MaterialTheme.typography.titleMedium) },
            //subtitle = { Text(text = "This is a longer text") },
            onClick = { navigation.navigate(SettingsNavigation.Language.route) }
        )
        Divider()
        SettingsMenuLink(

            icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Access Option") },
            title = { Text(text = "Access Options", style = MaterialTheme.typography.titleMedium) },
            onClick = { navigation.navigate(SettingsNavigation.Access.route) },
        )
        Divider()
        SettingsMenuLink(

            icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = "Theme") },
            title = { Text(text = "Theme", style = MaterialTheme.typography.titleMedium) },
            //subtitle = { Text(text = "This is a longer text") },
            onClick = { navigation.navigate(SettingsNavigation.Theme.route) },
        )
        Divider()
        SettingsMenuLink(

            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Contact") },
            title = { Text(text = "Contact", style = MaterialTheme.typography.titleMedium) },
            onClick = { navigation.navigate(SettingsNavigation.Contact.route) },
        )
        Divider()
        SettingsMenuLink(

            icon = {  Icon(painter = painterResource(R.drawable.baseline_money_24), contentDescription = "Primary Currency") },
            title = { Text(text = "Primary Currency", style = MaterialTheme.typography.titleMedium) },
            onClick = {navigation.navigate(SettingsNavigation.PrimaryCurrency.route) },
        )
        Divider()
        SettingsMenuLink(

            icon = { Icon(painter = painterResource(R.drawable.baseline_card_24), contentDescription = "Default Bank Account") },
            title = { Text(text = "Default Bank Account", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/},
        )
        Divider()
        SettingsMenuLink(

            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "Disable Account", tint = Color.Red) },
            title = { Text(text = stringResource(id = R.string.disable_account), style = MaterialTheme.typography.titleMedium, color = Color.Red) },
            onClick = {/*TODO*/},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsTabPreview() {
    AppTheme {
        SettingsTab()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsTabPreviewDark() {
    AppTheme {
        SettingsTab()
    }
}