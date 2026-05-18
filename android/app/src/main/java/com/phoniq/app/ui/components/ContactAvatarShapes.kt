package com.phoniq.app.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.phoniq.app.util.PersonalizationStore
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Maps personalization [PersonalizationStore.Snapshot.contactAvatarStyle] to a clip [Shape] for a given avatar [size]. */
object ContactAvatarShapes {
    fun shapeFor(styleKey: String, size: Dp): Shape =
        when (styleKey) {
            PersonalizationStore.CONTACT_AVATAR_ROUND -> CircleShape
            PersonalizationStore.CONTACT_AVATAR_SQUARE -> RoundedCornerShape(0.dp)
            PersonalizationStore.CONTACT_AVATAR_SQUIRCLE -> RoundedCornerShape(size * 0.38f)
            PersonalizationStore.CONTACT_AVATAR_STAR -> StarAvatarShape
            PersonalizationStore.CONTACT_AVATAR_TEARDROP -> TeardropAvatarShape
            PersonalizationStore.CONTACT_AVATAR_EXTRA -> HexagonAvatarShape
            else -> CircleShape
        }
}

private object StarAvatarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(size.width, size.height) / 2f
        val outer = r * 0.92f
        val inner = outer * 0.4f
        val points = 5
        repeat(points * 2) { i ->
            val rad = if (i % 2 == 0) outer else inner
            val angle = -PI / 2.0 + (i * PI / points)
            val x = cx + (cos(angle) * rad).toFloat()
            val y = cy + (sin(angle) * rad).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private object TeardropAvatarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val path = Path()
        val tipY = h * 0.96f
        val top = h * 0.14f
        path.moveTo(cx, tipY)
        path.cubicTo(
            w * 0.92f,
            h * 0.74f,
            w * 0.88f,
            top + h * 0.18f,
            cx,
            top,
        )
        path.cubicTo(
            w * 0.12f,
            top + h * 0.18f,
            w * 0.08f,
            h * 0.74f,
            cx,
            tipY,
        )
        path.close()
        return Outline.Generic(path)
    }
}

private object HexagonAvatarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(size.width, size.height) / 2f * 0.9f
        repeat(6) { i ->
            val angle = -PI / 2.0 + (i * PI / 3.0)
            val x = cx + (cos(angle) * r).toFloat()
            val y = cy + (sin(angle) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}
