package ro.bankar.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.ui.theme.AppTheme

sealed class MultiFabItem {
    abstract val id: String
    abstract val label: Int
}

data class MultiFabItemImageVector(override val id: String, val icon: ImageVector, override val label: Int) : MultiFabItem()
data class MultiFabItemResId(override val id: String, val icon: Int, override val label: Int) : MultiFabItem()

fun MultiFabItem(id: String, icon: ImageVector, label: Int) = MultiFabItemImageVector(id, icon, label)
fun MultiFabItem(id: String, icon: Int, label: Int) = MultiFabItemResId(id, icon, label)

@Composable
fun MultiFab(icon: ImageVector, entries: List<MultiFabItem>, onClick: (String) -> Unit) {
    MultiFabBase(icon = { Icon(imageVector = icon, contentDescription = null) }, entries, onClick)
}

@Composable
fun MultiFab(icon: Int, entries: List<MultiFabItem>, onClick: (String) -> Unit) {
    MultiFabBase(icon = { Icon(painter = painterResource(icon), contentDescription = null) }, entries, onClick)
}

@Composable
private fun MultiFabBase(icon: @Composable (Modifier) -> Unit, entries: List<MultiFabItem>, onClick: (String) -> Unit) {
    var extended by remember { mutableStateOf(false) }

    val entryAnimations = remember(entries) { entries.map { Animatable(0f) } }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(extended) {
        if (extended) {
            visible = true
            for ((index, animation) in entryAnimations.withIndex())
                launch { animation.animateTo(1f, tween(200, index * 100)) }
        } else {
            coroutineScope {
                for ((index, animation) in entryAnimations.reversed().withIndex())
                    launch { animation.animateTo(0f, tween(200, index * 100)) }
            }
            visible = false
        }
    }

    ConstraintLayout {
        val fab = createRef()
        val iconsRefs = entries.map { createRef() }
        val barrier = createStartBarrier(*iconsRefs.toTypedArray())

        val iconRotation by animateFloatAsState(if (extended) 45f else 0f, label = "Icon rotation")
        FloatingActionButton(onClick = { extended = !extended }, shape = CircleShape, modifier = Modifier.constrainAs(fab) {
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        }) {
            Crossfade(extended, label = "Icon swap") { extended ->
                if (extended) Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = iconRotation - 45f })
                else icon(Modifier.graphicsLayer { rotationZ = iconRotation })
            }
        }

        if (visible) for ((index, entry) in entries.withIndex()) {
            val ref = iconsRefs[index]
            val progress by entryAnimations[index].asState()
            FilledIconButton(
                onClick = { extended = false; onClick(entry.id) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .constrainAs(ref) {
                        start.linkTo(fab.start)
                        end.linkTo(fab.end)
                        bottom.linkTo(if (index == 0) fab.top else iconsRefs[index - 1].top, 12.dp)
                    }
                    .graphicsLayer {
                        alpha = progress
                        scaleX = progress
                        scaleY = progress
                    },
            ) {
                when (entry) {
                    is MultiFabItemImageVector -> Icon(imageVector = entry.icon, contentDescription = null)
                    is MultiFabItemResId -> Icon(painter = painterResource(entry.icon), contentDescription = null)
                }
            }

            Text(text = stringResource(entry.label), modifier = Modifier
                .constrainAs(createRef()) {
                    top.linkTo(ref.top)
                    bottom.linkTo(ref.bottom)
                    end.linkTo(barrier, 16.dp)
                }
                .alpha(progress)
            )
        }
    }
}

@Preview
@Composable
private fun MultiFabPreview() {
    AppTheme {
        Scaffold(floatingActionButton = {
            MultiFab(Icons.Default.Add, listOf(
                MultiFabItem("1", Icons.Default.Share, R.string.share_iban),
                MultiFabItem("2", Icons.Default.Person, R.string.view_party),
            ), onClick = {})
        }) { paddingValues ->
            Text(text = "Sample text", modifier = Modifier.padding(paddingValues))
        }
    }
}