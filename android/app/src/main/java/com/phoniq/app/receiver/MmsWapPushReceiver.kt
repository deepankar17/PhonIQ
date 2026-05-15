package com.phoniq.app.receiver



import android.content.BroadcastReceiver

import android.content.Context

import android.content.Intent

import android.provider.Telephony



/**

 * Default SMS apps must declare a receiver for [Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION]

 * with MIME `application/vnd.wap.mms-message` so carriers can signal MMS.

 *

 * Full MMS download, MmsProvider writes, and PDU parsing are not implemented yet. This receiver

 * intentionally does nothing besides satisfy the manifest contract without crashing; battery and

 * delivery behavior depend on the system MMS stack when PhonIQ is set as the default SMS app.

 */

class MmsWapPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION != intent.action) return

        // Ordered broadcast: higher-priority handlers may still run.

    }

}


