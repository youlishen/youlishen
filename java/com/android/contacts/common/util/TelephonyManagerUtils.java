/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.common.util;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import com.android.dialer.common.LogUtil;

import org.litepal.util.SharedUtil;

import java.util.Random;

/** This class provides several TelephonyManager util functions. */
public class TelephonyManagerUtils {
  private final static String TAG = "TelephonyManagerUtils";

  /**
   * Gets the voicemail tag from Telephony Manager.
   *
   * @param context Current application context
   * @return Voicemail tag, the alphabetic identifier associated with the voice mail number.
   */
  public static String getVoiceMailAlphaTag(Context context) {
    final TelephonyManager telephonyManager =
      (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    final String voiceMailLabel = telephonyManager.getVoiceMailAlphaTag();
    return voiceMailLabel;
  }

  public static String getDeviceId(Context context) {
    final TelephonyManager telephonyManager =
      (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
      != PackageManager.PERMISSION_GRANTED) {
      return "";
    }
    String deviceId = telephonyManager.getDeviceId();
    LogUtil.d(TAG, "getDeviceId 1 deviceId: " + deviceId);
    if (TextUtils.isEmpty(deviceId)) {
      deviceId = telephonyManager.getImei();
    }
    LogUtil.d(TAG, "getDeviceId 2 deviceId: " + deviceId);
    if (TextUtils.isEmpty(deviceId)) {
      deviceId = telephonyManager.getMeid();
    }
    LogUtil.d(TAG, "getDeviceId 3 deviceId: " + deviceId);
    if (TextUtils.isEmpty(deviceId)) {
      deviceId = Settings.Secure.getString(context.getContentResolver(), "android_id");
    }
    LogUtil.d(TAG, "getDeviceId 4 deviceId: " + deviceId);
    return deviceId;
  }

  private static String getRandom(int count) {
    StringBuilder builder = new StringBuilder(count);
    for (int i=0; i<count;i++) {
      builder.append(new Random().nextInt(9));
    }
    return builder.toString();
  }
}
