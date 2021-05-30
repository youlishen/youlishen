/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.wuliu;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.android.wuliu.WuLiuOrderInfoFragment;
import com.android.wuliu.WuLiuOrderInfoBean;

import java.util.ArrayList;

/**
 * View pager adapter for in call ui.
 */
public class WuLiuInCallPagerAdapter extends FragmentStatePagerAdapter {

  private final ArrayList<WuLiuOrderInfoBean> list = new ArrayList<>();

  public WuLiuInCallPagerAdapter(FragmentManager fragmentManager) {
    super(fragmentManager);
  }

  public void updateData(ArrayList<WuLiuOrderInfoBean> data) {
    list.clear();
    list.addAll(data);
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return list == null ? 0 : list.size();
  }

  @Override
  public Fragment getItem(int position) {
    if (position >= list.size()) {
      return null;
    }
    return WuLiuOrderInfoFragment.newInstance(list.get(position));
  }
}
