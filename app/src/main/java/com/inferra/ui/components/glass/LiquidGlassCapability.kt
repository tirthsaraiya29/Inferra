package com.inferra.ui.components.glass

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Capability tier for Liquid Glass rendering.
 */
enum class LiquidGlassTier {
    /** Full live backdrop capture + AGSL optical refraction shader + hardware blur + specular lighting */
    FULL,
    /** Live backdrop capture + hardware blur + specular rim lighting */
    REDUCED,
    /** Solid high-contrast surface (for Reduced Transparency / legacy hardware) */
    BASIC
}

/**
 * Environment configuration for accessibility & material rendering capabilities.
 */
data class LiquidGlassCapabilityState(
    val tier: LiquidGlassTier,
    val isReducedMotionEnabled: Boolean,
    val isReducedTransparencyEnabled: Boolean,
    val supportsAgsl: Boolean,
    val supportsHardwareBlur: Boolean
)

@Composable
fun rememberLiquidGlassCapability(): State<LiquidGlassCapabilityState> {
    val context = LocalContext.current.applicationContext

    val capabilityState = remember {
        mutableStateOf(calculateCapabilityState(context))
    }

    DisposableEffect(context) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

        val listener = AccessibilityManager.AccessibilityStateChangeListener {
            capabilityState.value = calculateCapabilityState(context)
        }

        am?.addAccessibilityStateChangeListener(listener)

        onDispose {
            am?.removeAccessibilityStateChangeListener(listener)
        }
    }

    return capabilityState
}

private fun calculateCapabilityState(context: Context): LiquidGlassCapabilityState {
    val supportsHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // API 31+
    val supportsAgsl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU // API 33+ (AGSL RuntimeShader)

    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val isTouchExplorationEnabled = am?.isTouchExplorationEnabled == true

    // Check system animator duration scale for reduced motion
    val animatorScale = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
    } catch (_: Exception) {
        1.0f
    }
    val isReducedMotionEnabled = animatorScale == 0.0f || isTouchExplorationEnabled

    // Check system reduced transparency (if present in Settings or accessibility)
    val isReducedTransparencyEnabled = try {
        Settings.Secure.getInt(
            context.contentResolver,
            "reduce_transparency",
            0
        ) == 1
    } catch (_: Exception) {
        false
    }

    val tier = when {
        isReducedTransparencyEnabled -> LiquidGlassTier.BASIC
        supportsAgsl && supportsHardwareBlur -> LiquidGlassTier.FULL
        supportsHardwareBlur -> LiquidGlassTier.REDUCED
        else -> LiquidGlassTier.BASIC
    }

    return LiquidGlassCapabilityState(
        tier = tier,
        isReducedMotionEnabled = isReducedMotionEnabled,
        isReducedTransparencyEnabled = isReducedTransparencyEnabled,
        supportsAgsl = supportsAgsl,
        supportsHardwareBlur = supportsHardwareBlur
    )
}
