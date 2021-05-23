package com.android.wuliu;

import com.urovo.dialercloud.net.model.TrackInfoModel;

public class WuLiuTrackInfoBean {
  private String scanType;
  private String scanTime;
  private String desc;
  private String exception;
  private boolean isSessionEnd;
  public WuLiuTrackInfoBean(String scanType, String scanTime, String desc) {
    this.scanType = scanType;
    this.scanTime = scanTime;
    this.desc = desc;
  }

  public static WuLiuTrackInfoBean getWuLiuInfoBean(TrackInfoModel info) {
    if (info == null) {
      return null;
    }
    return new WuLiuTrackInfoBean(info.getScantype(), info.getScantime(), info.getDesc());
  }

  public String getScanType() {
    return scanType;
  }

  public String getScanTime() {
    return scanTime;
  }

  public String getDesc() {
    return desc;
  }

  public String getException() {
    return exception;
  }

  public void setException(String exception) {
    this.exception = exception;
  }

  public boolean isSessionEnd() {
    return isSessionEnd;
  }

  public void setSessionEnd(boolean sessionEnd) {
    isSessionEnd = sessionEnd;
  }
}
