package com.inferra.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferra.ui.theme.CyberCyan
import com.inferra.ui.theme.CyberViolet
import com.inferra.ui.theme.GlassBorderLight
import com.inferra.ui.theme.GlassBorderSubtle
import com.inferra.ui.theme.GlassFillDark
import com.inferra.ui.theme.GlassFillHover
import com.inferra.ui.theme.ObsidianBg
import com.inferra.ui.theme.TextMuted
import com.inferra.ui.theme.TextPrimary

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Ambient Cyber Glow Orbs in background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Cyan top-left ambient orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.18f), Color.Transparent),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = Offset(0f, 0f)
                    )
                    // Violet bottom-right ambient orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberViolet.copy(alpha = 0.14f), Color.Transparent),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(size.width, size.height * 0.9f)
                    )
                }
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassFillDark,
    borderColor: Color = GlassBorderSubtle,
    borderWidth: Dp = 1.dp,
    blurRadiusDp: Float = 25f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1.0f,
        animationSpec = spring(stiffness = 500f),
        label = "glassCardScale"
    )

    val currentFill = if (isPressed) GlassFillHover else backgroundColor

    Box(
        modifier = modifier
            .scale(scaleAnim)
            .clip(shape)
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusDp > 0f) {
                    renderEffect = RenderEffect
                        .createBlurEffect(
                            blurRadiusDp,
                            blurRadiusDp,
                            Shader.TileMode.MIRROR
                        )
                        .asComposeRenderEffect()
                }
            }
            .background(currentFill)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.01f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.5f),
                        borderColor.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    accentColor: Color = CyberCyan,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(stiffness = 600f),
        label = "glassBtnScale"
    )

    val btnAlpha = if (enabled) 1.0f else 0.4f

    Box(
        modifier = modifier
            .scale(scaleAnim)
            .graphicsLayer { alpha = btnAlpha }
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.25f),
                        accentColor.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.60f),
                        accentColor.copy(alpha = 0.20f)
                    )
                ),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = CyberCyan
) {
    val fill = if (isSelected) accentColor.copy(alpha = 0.20f) else GlassFillDark
    val border = if (isSelected) accentColor.copy(alpha = 0.80f) else GlassBorderSubtle
    val textColor = if (isSelected) accentColor else TextPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                color = textColor
            )
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassFillDark,
        borderColor = GlassBorderLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Box(modifier = Modifier.padding(end = 12.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = TextStyle(color = TextMuted, fontSize = 14.sp)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingIcon != null) {
                Box(modifier = Modifier.padding(start = 12.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CyberCyan
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 11.sp,
                color = color
            )
        )
    }
}
