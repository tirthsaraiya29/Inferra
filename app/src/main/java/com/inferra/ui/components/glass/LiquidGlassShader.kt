package com.inferra.ui.components.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Corrected AGSL Shader for True Liquid Glass optical refraction distortion,
 * localized theme backdrop tinting, 3D specular edge highlights, and rim lighting.
 */
const val LIQUID_GLASS_AGSL = """
uniform shader composable;
uniform vec2 resolution;
uniform float cornerRadius;
uniform float refraction;
uniform float specular;
uniform float rimThickness;
uniform vec3 lightDir;
uniform vec4 themeTint;

float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(vec2 fragCoord) {
    vec2 center = resolution * 0.5;
    vec2 p = fragCoord - center;
    vec2 halfSize = resolution * 0.5;

    float d = sdRoundedBox(p, halfSize, cornerRadius);

    if (d > 0.5) {
        return half4(0.0);
    }

    vec2 uv = fragCoord / resolution;
    vec2 norm = vec2(0.0);

    if (d > -rimThickness) {
        float factor = (d + rimThickness) / rimThickness;
        vec2 edgeDir = (length(p) > 0.0) ? normalize(-p) : vec2(0.0);
        norm = edgeDir * sin(factor * 3.14159265) * refraction;
    }

    vec2 distortedUv = clamp(uv + norm, vec2(0.001), vec2(0.999));
    half4 backdropColor = composable.eval(distortedUv * resolution);

    // Composite theme tint over blurred/refracted backdrop
    half4 color = mix(backdropColor, themeTint, themeTint.a);

    // Compute top-left specular highlight
    vec3 surfaceNormal = normalize(vec3(norm * 5.0, 1.0));
    float spec = pow(max(0.0, dot(surfaceNormal, normalize(lightDir))), 28.0) * specular;
    float rim = (1.0 - smoothstep(-rimThickness, 0.0, d)) * 0.20;

    color.rgb += vec3(spec + rim);
    color.a = clamp(color.a + 0.15, 0.0, 1.0);

    return color;
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class LiquidGlassRuntimeShader {
    private val shader = RuntimeShader(LIQUID_GLASS_AGSL)

    fun createComposeRenderEffect(
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        themeTintRed: Float = 0.06f,
        themeTintGreen: Float = 0.09f,
        themeTintBlue: Float = 0.14f,
        themeTintAlpha: Float = 0.72f,
        refraction: Float = 0.06f,
        specular: Float = 0.35f,
        rimThicknessPx: Float = 14f,
        blurRadiusPx: Float = 12f,
    ): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("resolution", widthPx.coerceAtLeast(1f), heightPx.coerceAtLeast(1f))
        shader.setFloatUniform("cornerRadius", cornerRadiusPx)
        shader.setFloatUniform("refraction", refraction)
        shader.setFloatUniform("specular", specular)
        shader.setFloatUniform("rimThickness", rimThicknessPx)
        shader.setFloatUniform("lightDir", -0.4f, -0.7f, 0.60f)
        shader.setFloatUniform("themeTint", themeTintRed, themeTintGreen, themeTintBlue, themeTintAlpha)

        // Localized 12dp hardware blur
        val blurEffect = RenderEffect.createBlurEffect(
            blurRadiusPx,
            blurRadiusPx,
            Shader.TileMode.CLAMP
        )

        val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
        val chainedEffect = RenderEffect.createChainEffect(shaderEffect, blurEffect)

        return chainedEffect.asComposeRenderEffect()
    }
}
