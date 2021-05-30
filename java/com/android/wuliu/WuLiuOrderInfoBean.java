package com.android.wuliu;

import com.urovo.dialercloud.net.model.OrderInfoModel;

import java.util.ArrayList;

public class WuLiuOrderInfoBean {
  private String orderNumber;
  private String orderStatus;
  private String name;
  private String phoneNumber;
  private String address;
  private String exception;
  private boolean isSessionEnd;

  private ArrayList<WuLiuTrackInfoBean> list;

  public WuLiuOrderInfoBean() {
  }

  public WuLiuOrderInfoBean(String orderNumber, String orderStatus, String name,
                            String phoneNumber, String address) {
    this.orderNumber = orderNumber;
    this.orderStatus = orderStatus;
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.address = address;
  }

  public static WuLiuOrderInfoBean getWuLiuInfoBean(OrderInfoModel info) {
    if (info == null) {
      return null;
    }
    return new WuLiuOrderInfoBean(info.getOrder_no(), info.getOrder_status(),
      info.getCustomer_name(), info.getCustomer_phone(), info.getCustomer_addr());
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public void setOrderStatus(String orderStatus) {
    this.orderStatus = orderStatus;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public void setAddress(String address) {
    this.address = address;
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

  public String getOrderNumber() {
    return orderNumber;
  }

  public String getOrderStatus() {
    return orderStatus;
  }

  public String getName() {
    return name;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getAddress() {
    return address;
  }

  public ArrayList<WuLiuTrackInfoBean> getTrackList() {
    return list;
  }

  public void setTrackList(ArrayList<WuLiuTrackInfoBean> list) {
    this.list = list;
  }


}
