package com.phoniq.app.ui.components

import android.net.Uri
import android.content.ContentUris
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

/** Single-line initials tuned for optical centering inside circular or square avatars (no extra font padding). */
@Composable
internal fun AvatarInitialsText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier.fillMaxSize(),
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    Text(
        text = text,
        modifier = modifier,
        style = avatarInitialsStyle(fontSize = fontSize, fontWeight = fontWeight, color = color),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

private fun avatarInitialsStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
): TextStyle =
    TextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        lineHeight = fontSize,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle =
            LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
    )

@Composable
fun ContactPhotoAvatar(
    deviceContactId: Long,
    initials: String,
    gradientStart: Color,
    gradientEnd: Color,
    size: Dp,
    fontSize: TextUnit = 16.sp,
) {
    val context = LocalContext.current
    val photoUri: Uri? =
        if (deviceContactId > 0L) {
            Uri.withAppendedPath(
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, deviceContactId),
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY,
            )
        } else {
            null
        }
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(gradientStart, gradientEnd))),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            SubcomposeAsyncImage(
                model =
                    ImageRequest.Builder(context)
                        .data(photoUri)
                        .crossfade(true)
                        .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    AvatarInitialsText(text = initials, fontSize = fontSize)
                },
                success = {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                },
                error = {
                    AvatarInitialsText(text = initials, fontSize = fontSize)
                },
                contentScale = ContentScale.Crop,
            )
        } else {
            AvatarInitialsText(text = initials, fontSize = fontSize)
        }
    }
}
