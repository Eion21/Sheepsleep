package io.github.reposo.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DAMPING = 0.3f
private const val MAX_FRACTION = 0.45f

private class StretchOverscrollState(
    private val scope: CoroutineScope,
) : NestedScrollConnection {
    var stretch by mutableFloatStateOf(0f)
    var maxHeightPx = 1f
    private var settleJob: Job? = null

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
        settleJob?.cancel()
        settleJob = null
        val limit = maxHeightPx * MAX_FRACTION
        stretch = (stretch + available.y * DAMPING).coerceIn(-limit, limit)
        return available
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        settleJob?.cancel()
        settleJob = scope.launch {
            val start = stretch
            if (start != 0f) {
                animate(
                    initialValue = start,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 320f),
                ) { value, _ -> stretch = value }
            }
            stretch = 0f
        }
        return Velocity.Zero
    }
}

@Composable
fun Modifier.stretchOverscroll(): Modifier {
    val scope = rememberCoroutineScope()
    val state = remember { StretchOverscrollState(scope) }

    return this
        .onSizeChanged { state.maxHeightPx = it.height.toFloat().coerceAtLeast(1f) }
        .nestedScroll(state)
        .graphicsLayer {
            val s = state.stretch
            if (s != 0f) {
                translationY = s
                scaleY = 1f + abs(s) / state.maxHeightPx
                transformOrigin = TransformOrigin(0.5f, if (s >= 0f) 0f else 1f)
            } else {
                translationY = 0f
                scaleY = 1f
            }
        }
}
