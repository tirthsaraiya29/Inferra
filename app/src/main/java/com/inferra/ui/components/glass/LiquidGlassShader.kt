package com.inferra.ui.components.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * AGSL Shader code for True Liquid Glass optical refraction distortion,
 * 3D specular highlights, normal vector edge displacement, and rim lighting.
 */
const val LIQUID_GLASS_AGSL = """
uniform shader composable;
uniform vec2 resolution;
uniform float cornerRadius;
uniform float refraction;
uniform float specular;
uniform float rimThickness;
uniform vec3 lightDir;

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
    half4 color = composable.eval(distortedUv * resolution);

    vec3 surfaceNormal = normalize(vec3(norm * 4.0, 1.0));
    float spec = pow(max(0.0, dot(surfaceNormal, normalize(lightDir))), 24.0) * specular;
    float rim = (1.0 - smoothstep(-rimThickness, 0.0, d)) * 0.25;

    color.rgb += vec3(spec + rim);
    color.rgb = mix(color.rgb, vec3(0.06, 0.10, 0.16), 0.12);

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
        refraction: Float = 0.05f,
        specular: Float = 0.30f,
        rimThicknessPx: Float = 16f,
        blurRadiusPx: Float = 32f,
    ): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("resolution", widthPx.coerceAtLeast(1f), heightPx.coerceAtLeast(1f))
        shader.setFloatUniform("cornerRadius", cornerRadiusPx)
        shader.setFloatUniform("refraction", refraction)
        shader.setFloatUniform("specular", specular)
        shader.setFloatUniform("rimThickness", rimThicknessPx)
        shader.setFloatUniform("lightDir", -0.3f, -0.6f, 0.74f)

        // Chain hardware blur with AGSL shader effect
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
