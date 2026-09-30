package com.inferra.ui.components.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

@Stable
class BackdropState {
    var boundsInWindow by mutableStateOf(Rect.Zero)
    var isCaptured by mutableStateOf(value = false)
}

val LocalBackdropState = compositionLocalOf { BackdropState() }

@Composable
fun BackdropCaptureContainer(
    modifier: Modifier = Modifier,
    backdropState: BackdropState = remember { BackdropState() },
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalBackdropState provides backdropState) {
        Box(
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    backdropState.boundsInWindow = coordinates.boundsInWindow()
                    backdropState.isCaptured = true
                }
        ) {
            content()
        }
    }
}
