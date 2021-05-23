package com.android.wuliu;

import com.urovo.dialercloud.net.model.OrderInfoModel;
import com.urovo.dialercloud.net.model.TrackInfoModel;

import java.util.ArrayList;
import java.util.List;

public class WuLiuInfoBean {
  List<TrackInfoModel> trackInfoModels = null;
  List<OrderInfoModel> orderInfoModels = null;
  boolean isSessionEnd = false;

  public boolean isSessionEnd() {
    return isSessionEnd;
  }

  public void setSessionEnd(boolean sessionEnd) {
    isSessionEnd = sessionEnd;
  }

  public void setTrackInfoModels(List<TrackInfoModel> trackInfoModels) {
    this.trackInfoModels = trackInfoModels;
  }

  public void setOrderInfoModels(List<OrderInfoModel> orderInfoModels) {
    this.orderInfoModels = orderInfoModels;
  }

  public ArrayList<WuLiuOrderInfoBean> convertOrderInfo() {
    if (orderInfoModels == null || orderInfoModels.size() == 0) {
      return null;
    }
    ArrayList<WuLiuOrderInfoBean> infoList = new ArrayList<>(orderInfoModels.size());
    for (OrderInfoModel info : orderInfoModels) {
      infoList.add(WuLiuOrderInfoBean.getWuLiuInfoBean(info));
    }
    return infoList;
  }

  public WuLiuOrderInfoBean getFirstOrderInfo() {
    if (orderInfoModels == null || orderInfoModels.size() == 0) {
      return null;
    }
    ArrayList<WuLiuOrderInfoBean> infoList = new ArrayList<>(orderInfoModels.size());
    for (OrderInfoModel info : orderInfoModels) {
      infoList.add(WuLiuOrderInfoBean.getWuLiuInfoBean(info));
    }
    return infoList.get(0);
  }

  public String getFirstOrderInfoOrderNumber() {
    if (orderInfoModels == null || orderInfoModels.size() == 0) {
      return null;
    }
    ArrayList<WuLiuOrderInfoBean> infoList = new ArrayList<>(orderInfoModels.size());
    for (OrderInfoModel info : orderInfoModels) {
      infoList.add(WuLiuOrderInfoBean.getWuLiuInfoBean(info));
    }
    return infoList.get(0).getOrderNumber();
  }

  public ArrayList<WuLiuTrackInfoBean> convertTrackInfo() {
    if (trackInfoModels == null || trackInfoModels.size() == 0) {
      return null;
    }
    ArrayList<WuLiuTrackInfoBean> infoList = new ArrayList<>(trackInfoModels.size());
    for (TrackInfoModel info : trackInfoModels) {
      infoList.add(WuLiuTrackInfoBean.getWuLiuInfoBean(info));
    }
    return infoList;
  }
}
