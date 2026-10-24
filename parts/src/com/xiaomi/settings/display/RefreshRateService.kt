/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.core.os.postDelayed

/**
 * With the prebuilt kernel the panel gets a green tint once the refresh rate drops while the
 * screen is idle. Keep the minimum refresh rate at 60Hz and pulse it to 120Hz whenever the screen
 * turns on, so the panel leaves that state.
 */
class RefreshRateService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (DEBUG) Log.d(TAG, "onReceive: ${intent.action}")
                if (intent.action != Intent.ACTION_SCREEN_ON) return

                handler.removeCallbacksAndMessages(null)
                setMinRefreshRate(HIGH_REFRESH_RATE)
                handler.postDelayed(RESET_DELAY_MS) { setMinRefreshRate(NORMAL_REFRESH_RATE) }
            }
        }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")
        registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        setMinRefreshRate(NORMAL_REFRESH_RATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(screenStateReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setMinRefreshRate(rate: Float) {
        if (DEBUG) Log.d(TAG, "setMinRefreshRate: ${rate}Hz")
        Settings.System.putFloat(contentResolver, KEY_MIN_REFRESH_RATE, rate)
    }

    companion object {
        private const val TAG = "RefreshRateService"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

        private const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
        private const val HIGH_REFRESH_RATE = 120f
        private const val NORMAL_REFRESH_RATE = 60f
        private const val RESET_DELAY_MS = 100L

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, RefreshRateService::class.java),
                UserHandle.CURRENT,
            )
        }
    }
}
