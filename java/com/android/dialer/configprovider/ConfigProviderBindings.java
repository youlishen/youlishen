/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.dialer.configprovider;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.UserManagerCompat;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;

/** Accessor for getting a {@link ConfigProvider}. */
public class ConfigProviderBindings {

  private static ConfigProvider configProvider;
  private static ConfigProvider configProviderStub;

  public static ConfigProvider get(@NonNull Context context) {
    Assert.isNotNull(context);
    if (configProvider != null) {
      return configProvider;
    }
    boolean isUserUnlocked = UserManagerCompat.isUserUnlocked(context);
    LogUtil.d("ConfigProviderBindings", "get isUserUnlocked: " + isUserUnlocked);
    if (!isUserUnlocked) {
      if (configProviderStub == null) {
        configProviderStub = new ConfigProviderStub();
      }
      LogUtil.d("ConfigProviderBindings", "get configProviderStub: " + configProviderStub);
      return configProviderStub;
    }
    configProvider = ConfigProviderComponent.get(context).getConfigProvider();
    LogUtil.d("ConfigProviderBindings", "get configProvider: " + configProvider);
    return configProvider;
  }

  @VisibleForTesting
  public static void setForTesting(@Nullable ConfigProvider configProviderForTesting) {
    configProvider = configProviderForTesting;
  }

  private static class ConfigProviderStub implements ConfigProvider {
    @Override
    public String getString(String key, String defaultValue) {
      return defaultValue;
    }

    @Override
    public long getLong(String key, long defaultValue) {
      return defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
      return defaultValue;
    }
  }
}
