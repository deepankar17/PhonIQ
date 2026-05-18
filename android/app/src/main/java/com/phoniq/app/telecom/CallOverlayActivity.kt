package com.phoniq.app.telecom

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoniq.app.ui.phone.InCallScreen
import com.phoniq.app.ui.phone.InCallUiPhase
import com.phoniq.app.R
import com.phoniq.app.ui.phone.IncomingCallScreen
import com.phoniq.app.ui.theme.PhonIQTheme
import com.phoniq.app.util.PersonalizationStore
import com.phoniq.app.telecom.CallRecordingPreferences
import android.widget.Toast

/**
 * Full-screen incoming / in-call UI when the app is in the background or the device is locked.
 * Launched from [IncomingCallNotification] full-screen intent and from [PhonIQInCallService].
 */
class CallOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()
        setContent {
            val perso = remember { PersonalizationStore.load(this@CallOverlayActivity) }
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = if (perso.followSystemTheme) systemDark else perso.darkTheme
            val effectiveAmoled = !perso.followSystemTheme && perso.amoledBlack
            PhonIQTheme(
                darkTheme = effectiveDark,
                themePreset = perso.themePreset,
                accentArgb = perso.accentArgb,
                useAmoledBlack = effectiveAmoled,
                denseThreads = perso.denseThreads,
                materialYou = perso.materialYou,
                fontFamilyName = perso.fontFamily,
                fontSizeTier = perso.fontSizeTier,
                hapticsEnabled = perso.hapticsEnabled,
                showInCallTimer = perso.showInCallTimer,
                rcsBadgesEnabled = perso.rcsUiEnabled,
                blurMoneyAmounts = perso.blurMoneyAmounts,
                contactAvatarStyle = perso.contactAvatarStyle,
            ) {
                val activeCall by CallStateRepository.callInfo.collectAsStateWithLifecycle()
                val isCallRecording by CallStateRepository.isCallRecording.collectAsStateWithLifecycle()

                LaunchedEffect(activeCall?.state) {
                    if (activeCall == null || activeCall?.state == CallState.DISCONNECTED) {
                        finish()
                    }
                }

                AnimatedVisibility(
                    visible = activeCall != null && activeCall!!.state != CallState.DISCONNECTED,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    activeCall?.let { info ->
                        when (info.state) {
                            CallState.RINGING ->
                                IncomingCallScreen(
                                    callerName = info.callerName,
                                    callerNumber = info.callerNumber,
                                    deviceContactId = info.deviceContactId,
                                    dialpadStyle = perso.dialpadStyle,
                                    answerCallStyle = perso.answerCallStyle,
                                    onAnswer = {
                                        CallStateRepository.answer()
                                        IncomingCallNotification.dismiss(this@CallOverlayActivity)
                                    },
                                    onDecline = {
                                        CallStateRepository.reject()
                                        IncomingCallNotification.dismiss(this@CallOverlayActivity)
                                        finish()
                                    },
                                    onUserMessage = { Toast.makeText(this@CallOverlayActivity, it, Toast.LENGTH_SHORT).show() },
                                )
                            CallState.DIALING ->
                                InCallScreen(
                                    callerName = info.callerName,
                                    callerNumber = info.callerNumber,
                                    deviceContactId = info.deviceContactId,
                                    phase = InCallUiPhase.Dialing,
                                    dialpadStyle = perso.dialpadStyle,
                                    answerCallStyle = perso.answerCallStyle,
                                    isCallRecordingActive = false,
                                    canControlCallRecording = false,
                                    onToggleCallRecording = {},
                                    onHangUp = {
                                        CallStateRepository.hangUp()
                                        finish()
                                    },
                                    onUserMessage = { Toast.makeText(this@CallOverlayActivity, it, Toast.LENGTH_SHORT).show() },
                                )
                            else ->
                                InCallScreen(
                                    callerName = info.callerName,
                                    callerNumber = info.callerNumber,
                                    deviceContactId = info.deviceContactId,
                                    phase = InCallUiPhase.Active,
                                    dialpadStyle = perso.dialpadStyle,
                                    answerCallStyle = perso.answerCallStyle,
                                    isCallRecordingActive = isCallRecording,
                                    canControlCallRecording = true,
                                    onToggleCallRecording = {
                                        if (!CallRecordingPreferences.isEnabled(this@CallOverlayActivity)) {
                                            Toast.makeText(
                                                this@CallOverlayActivity,
                                                getString(R.string.toast_incall_record_pref_off),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        } else if (isCallRecording) {
                                            CallStateRepository.requestStopCallRecording()
                                        } else {
                                            CallStateRepository.requestStartCallRecording()
                                        }
                                    },
                                    onHangUp = {
                                        CallStateRepository.hangUp()
                                        finish()
                                    },
                                    onUserMessage = { Toast.makeText(this@CallOverlayActivity, it, Toast.LENGTH_SHORT).show() },
                                )
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            val intent =
                Intent(context, CallOverlayActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                    )
                }
            context.startActivity(intent)
        }
    }
}
