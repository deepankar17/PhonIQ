package com.phoniq.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.phoniq.app.ui.theme.LocalContactAvatarStyle

@Composable
fun Modifier.contactAvatarClip(size: Dp): Modifier {
    val style = LocalContactAvatarStyle.current
    return clip(ContactAvatarShapes.shapeFor(style, size))
}

/** Border / clip share the same outline for a given avatar size. */
@Composable
fun contactAvatarShapeForSize(size: Dp) = ContactAvatarShapes.shapeFor(LocalContactAvatarStyle.current, size)
