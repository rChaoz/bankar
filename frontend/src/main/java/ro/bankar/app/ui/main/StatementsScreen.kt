package ro.bankar.app.ui.main

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build.VERSION
import android.util.Range
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.maxkeppeker.sheets.core.models.base.UseCaseState
import com.maxkeppeler.sheets.calendar.CalendarView
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import ro.bankar.app.LocalActivity
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.handle
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.components.AccountsComboBox
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.ButtonField
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.SurfaceList
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.home.InfoCard
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.color
import ro.bankar.model.SBankAccount
import ro.bankar.model.SStatementRequest
import ro.bankar.util.format
import ro.bankar.util.todayHere
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
fun StatementsScreen(onDismiss: () -> Unit) {
    // Load data
    val repository = LocalRepository.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    val accounts by repository.accounts.collectAsState(null)
    val statements by repository.statements.collectAsState(null)

    // Create statement dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val name = remember {
        verifiableStateOf("") {
            if (it.trim().length > SStatementRequest.maxNameLength) getString(R.string.statement_name_too_long, SStatementRequest.maxNameLength)
            else null
        }
    }
    val account = remember { mutableStateOf<SBankAccount?>(null) }
    var startDate by remember { mutableStateOf(Clock.System.todayHere() - DatePeriod(months = 1)) }
    var endDate by remember { mutableStateOf(Clock.System.todayHere()) }

    BottomDialog(
        visible = showCreateDialog,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false, usePlatformDefaultWidth = false),
        onDismissRequest = { showCreateDialog = false },
        confirmButtonText = R.string.create,
        onConfirmButtonClick = {
            name.check(context)
            if (!name.verified) return@BottomDialog
            isLoading = true
            scope.launch {
                repository.sendStatementRequest(name.value.trim().ifEmpty { null }, account.value!!.id, startDate, endDate).handle(context) {
                    repository.statements.emitNow()
                    showCreateDialog = false
                    null
                }
                isLoading = false
            }
        },
        confirmButtonEnabled = account.value != null
    ) {
        var showCalendar by remember { mutableStateOf(false) }
        BackHandler { if (showCalendar) showCalendar = false else showCreateDialog = false }

        LaunchedEffect(true) {
            name.value = ""
            account.value = null
            startDate = Clock.System.todayHere() - DatePeriod(months = 1)
            endDate = Clock.System.todayHere()
        }

        LoadingOverlay(isLoading) {
            Box(modifier = Modifier.padding(vertical = 6.dp)) {
                Column(modifier = Modifier
                    .padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountsComboBox(selectedAccount = account, accounts = accounts, pickText = R.string.choose_statement_account)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(R.string.optional_statement_name))
                        VerifiableField(name, label = R.string.name, type = KeyboardType.Text, isLast = true, modifier = Modifier.fillMaxWidth())
                    }
                    ButtonField(value = startDate.format(), onClick = { showCalendar = true }, label = R.string.from_date, modifier = Modifier.fillMaxWidth())
                    ButtonField(value = endDate.format(), onClick = { showCalendar = true }, label = R.string.to_date, modifier = Modifier.fillMaxWidth())
                }
                if (showCalendar) Popup(onDismissRequest = { showCalendar = false }) {
                    Surface(tonalElevation = 1.dp, shadowElevation = 2.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.padding(8.dp)) {
                        CalendarView(
                            useCaseState = remember { UseCaseState(visible = true, embedded = true) },
                            selection = CalendarSelection.Period(
                                selectedRange = Range(startDate.toJavaLocalDate(), endDate.toJavaLocalDate()),
                                onSelectRange = { start, end ->
                                    startDate = start.toKotlinLocalDate()
                                    endDate = end.toKotlinLocalDate()
                                    showCalendar = false
                                },
                                onNegativeClick = { showCalendar = false }
                            ),
                            config = CalendarConfig(boundary = LocalDate.of(2023, 1, 1)..LocalDate.now())
                        )
                    }
                }
            }
        }
    }

    // Content
    val scrollState = rememberScrollState()
    val (fabVisible, setFabVisible) = remember { mutableStateOf(true) }
    HideFABOnScroll(scrollState, setFabVisible)
    NavScreen(
        onDismiss,
        title = R.string.statements,
        snackbar = snackbar,
        isFABVisible = fabVisible && accounts != null,
        fabContent = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.create_statement))
            }
        }
    ) {
        var isRefreshing by remember { mutableStateOf(false) }
        @Suppress("DEPRECATION")
        SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing), onRefresh = {
            isRefreshing = true
            scope.launch {
                coroutineScope {
                    launch { repository.statements.emitNow() }
                    launch { repository.accounts.emitNow() }
                }
                isRefreshing = false
            }
        }) {
            SurfaceList(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .verticalScroll(scrollState)
            ) {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                data class Download(val id: Long, val job: Job)

                val downloads = remember { mutableListOf<Download>() }
                val activity = LocalActivity.current
                if (activity != null) DisposableEffect(downloadManager) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            val index = downloads.indexOfFirst { it.id == id }
                            if (index != -1) {
                                val download = downloads.removeAt(index)
                                download.job.run { if (isActive) cancel() }
                                val uri = downloadManager.getUriForDownloadedFile(download.id)
                                if (uri != null) scope.launch {
                                    if (snackbar.showSnackbar(
                                            message = context.getString(R.string.download_complete),
                                            actionLabel = context.getString(R.string.open_file),
                                            duration = SnackbarDuration.Long
                                        ) == SnackbarResult.ActionPerformed
                                    ) {
                                        val view = Intent(Intent.ACTION_VIEW)
                                        view.data = uri
                                        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        context.startActivity(view)
                                    }
                                }
                            }
                        }
                    }

                    if (VERSION.SDK_INT >= 33)
                        activity.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
                    else
                        activity.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

                    onDispose {
                        activity.unregisterReceiver(receiver)
                    }
                }

                if (statements == null || accounts == null) repeat(3) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Use icon instead of image to tint (remove red color)
                        Icon(
                            painter = painterResource(R.drawable.pdf_file_icon),
                            contentDescription = stringResource(R.string.pdf),
                            modifier = Modifier.shimmer(shimmer)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp, 15.dp)
                                    .grayShimmer(shimmer)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(100.dp, 13.dp)
                                    .grayShimmer(shimmer)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(100.dp, 13.dp)
                                    .grayShimmer(shimmer)
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.baseline_download_24),
                            contentDescription = stringResource(R.string.download),
                            modifier = Modifier.shimmer(shimmer)
                        )
                    }
                } else if (statements!!.isEmpty()) InfoCard(text = R.string.no_statements, onClick = { showCreateDialog = true })
                else for (statement in statements!!) Surface(onClick = {
                    val job = scope.launch { snackbar.showSnackbar(context.getString(R.string.download_started), withDismissAction = true) }
                    val id = downloadManager.enqueue(repository.createDownloadStatementRequest(statement))
                    downloads += Download(id, job)
                }) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Image(painter = painterResource(R.drawable.pdf_file_icon), contentDescription = stringResource(R.string.pdf))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (statement.name == null) stringResource(R.string.statement_no_name)
                                else stringResource(R.string.statement_name, statement.name!!),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                            )
                            val acc = accounts!!.find { it.id == statement.accountID }
                            // Should never be null, still, if somehow it is, don't crash the app
                            if (acc != null) Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = stringResource(R.string.for_account))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(acc.color(), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = acc.name)
                            }
                            Text(text = statement.dateTime.format(true), color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(painter = painterResource(R.drawable.baseline_download_24), contentDescription = stringResource(R.string.download))
                    }
                }
            }
        }
    }
}

// Only use with emulator to prevent 'download service not supported'
@Preview
@Composable
private fun StatementsScreenPreview() {
    AppTheme {
        StatementsScreen(onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatementsScreenPreviewDark() {
    AppTheme {
        StatementsScreen(onDismiss = {})
    }
}