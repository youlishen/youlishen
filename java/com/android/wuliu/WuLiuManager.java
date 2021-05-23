package com.android.wuliu;

import android.annotation.WorkerThread;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.android.contacts.common.util.TelephonyManagerUtils;
import com.android.dialer.binary.common.DialerApplication;
import com.android.dialer.common.LogUtil;
import com.urovo.dialercloud.CDialerDataManager;
import com.urovo.dialercloud.CDialerManager;
import com.urovo.dialercloud.CDialerStatusManager;
import com.urovo.dialercloud.IDataResponseListener;
import com.urovo.dialercloud.net.model.OrderInfoModel;
import com.urovo.dialercloud.net.model.TrackInfoModel;

import java.util.ArrayList;
import java.util.List;

public class WuLiuManager {

  private static final String TAG = "WuLiuQueryManager";
  private Handler childHandler;
  private HandlerThread childThread;
  private Context context;
  private static WuLiuManager instance;
  private static final Object OBJ = new Object();

  private final Handler mainHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
    }
  };

  private WuLiuManager(Context context) {
    this.context = context;
    String deviceId = TelephonyManagerUtils.getDeviceId(context);
    CDialerManager.getInstance().init(context, "urovo", deviceId);
    childThread = new HandlerThread("wu-liu-manager");
    childThread.start();
    childHandler = new Handler(childThread.getLooper());
  }

  public static void initManager(Context context) {
    if (instance == null) {
      synchronized (OBJ) {
        if (instance == null) {
          instance = new WuLiuManager(context);
        }
      }
    }
  }

  public static WuLiuManager getInstance() {
    if (instance == null) {
      initManager(DialerApplication.getContext());
    }
    return instance;
  }

  public boolean isAutoDialer() {
    return CDialerStatusManager.isAutoDialer(context);
  }

  public boolean isNeedLogin() {
    return !CDialerStatusManager.isDialerLogin(context);
  }

  /**
   * slotId 1或者2
   */
  public boolean isAutoRecord(int slotId) {
    return CDialerStatusManager.isAutoRecord(context, slotId);
  }

  @WorkerThread
  public ArrayList<WuLiuOrderInfoBean> syncQueryOrdersByPhoneNum(String phoneNum) {
    final WuLiuInfoBean infoBean = new WuLiuInfoBean();
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum infoBean: " + infoBean);
    final IDataResponseListener<List<OrderInfoModel>> iDataResponseListener
      = new IDataResponseListener<List<OrderInfoModel>>() {
      @Override
      public void onDataResponse(List<OrderInfoModel> orderInfoModels) {
        infoBean.setOrderInfoModels(orderInfoModels);
        infoBean.setSessionEnd(true);
        LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onDataResponse infoBean: " + infoBean);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onDataResponse 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }

      @Override
      public void onError(String s, String s1) {
        infoBean.setSessionEnd(true);
        LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onError infoBean: " + infoBean);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onError 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }
    };
    CDialerDataManager.getInstance().queryOrdersByPhoneNum(context, phoneNum, iDataResponseListener);
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 1 infoBean isSessionEnd: " + infoBean.isSessionEnd());
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 1 infoBean: " + infoBean);
    if (infoBean.isSessionEnd()) {
      return infoBean.convertOrderInfo();
    }
    synchronized (infoBean) {
      try {
        infoBean.wait();
        LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 2 infoBean: " + infoBean);
      } catch (Exception e) {
        LogUtil.w(TAG, "syncQueryOrdersByPhoneNum Exception: ", e);
      }
    }
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 3 infoBean: " + infoBean);
    return infoBean.convertOrderInfo();
  }


  @WorkerThread
  public String syncQueryOrdersNumberByPhoneNum(String phoneNum) {
    final WuLiuInfoBean infoBean = new WuLiuInfoBean();
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum infoBean: " + infoBean);
    final IDataResponseListener<List<OrderInfoModel>> iDataResponseListener
      = new IDataResponseListener<List<OrderInfoModel>>() {
      @Override
      public void onDataResponse(List<OrderInfoModel> orderInfoModels) {
        infoBean.setOrderInfoModels(orderInfoModels);
        infoBean.setSessionEnd(true);
        LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onDataResponse infoBean: " + infoBean);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onDataResponse 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }

      @Override
      public void onError(String s, String s1) {
        infoBean.setSessionEnd(true);
        LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onError infoBean: " + infoBean);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryOrdersByPhoneNum onError 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }
    };
    CDialerDataManager.getInstance().queryOrdersByPhoneNum(context, phoneNum, iDataResponseListener);
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 1 infoBean isSessionEnd: " + infoBean.isSessionEnd());
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 1 infoBean: " + infoBean);
    if (infoBean.isSessionEnd()) {
      return infoBean.getFirstOrderInfoOrderNumber();
    }
    synchronized (infoBean) {
      try {
        infoBean.wait();
        LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 2 infoBean: " + infoBean);
      } catch (Exception e) {
        LogUtil.w(TAG, "syncQueryOrdersByPhoneNum Exception: ", e);
      }
    }
    LogUtil.d(TAG, "syncQueryOrdersByPhoneNum 3 infoBean: " + infoBean);
    return infoBean.getFirstOrderInfoOrderNumber();
  }

  @WorkerThread
  public WuLiuOrderInfoBean syncQueryOrderByOrderNum(String orderNumber) {
    final WuLiuOrderInfoBean infoBean = new WuLiuOrderInfoBean();
    LogUtil.d(TAG, "syncQueryOrderByOrderNum infoBean: " + infoBean);
    final IDataResponseListener<OrderInfoModel> iDataResponseListener
      = new IDataResponseListener<OrderInfoModel>() {
      @Override
      public void onDataResponse(OrderInfoModel orderInfoModel) {
        infoBean.setAddress(orderInfoModel.getCustomer_addr());
        infoBean.setName(orderInfoModel.getCustomer_name());
        infoBean.setOrderNumber(orderInfoModel.getOrder_no());
        infoBean.setPhoneNumber(orderInfoModel.getCustomer_phone());
        infoBean.setOrderStatus(orderInfoModel.getOrder_status());
        infoBean.setException(null);
        infoBean.setSessionEnd(true);
        LogUtil.d(TAG, "syncQueryOrderByOrderNum onDataResponse infoBean: " + infoBean);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryOrderByOrderNum onDataResponse 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }

      @Override
      public void onError(String s, String s1) {
        LogUtil.d(TAG, "syncQueryOrderByOrderNum onError infoBean: " + infoBean);
        infoBean.setException(s + s1);
        infoBean.setSessionEnd(true);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryOrderByOrderNum onError 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }
    };
    CDialerDataManager.getInstance().queryOrderByOrderNum(context, orderNumber, iDataResponseListener);
    LogUtil.d(TAG, "syncQueryTrackByPhoneNum 1 infoBean isSessionEnd: " + infoBean.isSessionEnd());
    LogUtil.d(TAG, "syncQueryOrderByOrderNum 1 infoBean: " + infoBean);
    if (infoBean.isSessionEnd()) {
      return infoBean;
    }
    synchronized (infoBean) {
      try {
        infoBean.wait();
        LogUtil.d(TAG, "syncQueryOrderByOrderNum 2 infoBean: " + infoBean);
      } catch (Exception e) {
        LogUtil.w(TAG, "syncQueryOrderByOrderNum Exception: ", e);
      }
    }
    LogUtil.d(TAG, "syncQueryOrderByOrderNum 3 infoBean: " + infoBean);
    return infoBean;
  }

  @WorkerThread
  public ArrayList<WuLiuTrackInfoBean> syncQueryTrackByPhoneNum(String phoneNum) {
    final WuLiuInfoBean infoBean = new WuLiuInfoBean();
    final IDataResponseListener<List<TrackInfoModel>> iDataResponseListener = new IDataResponseListener<List<TrackInfoModel>>() {
      @Override
      public void onDataResponse(List<TrackInfoModel> trackInfoModels) {
        infoBean.setTrackInfoModels(trackInfoModels);
        infoBean.setSessionEnd(true);
        LogUtil.d(TAG, "syncQueryTrackByPhoneNum onDataResponse infoBean: " + infoBean);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryTrackByPhoneNum onDataResponse 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }

      @Override
      public void onError(String s, String s1) {
        infoBean.setSessionEnd(true);
        LogUtil.d(TAG, "syncQueryTrackByPhoneNum onError infoBean: " + infoBean);
        synchronized (infoBean) {
          LogUtil.d(TAG, "syncQueryTrackByPhoneNum onError 1 infoBean: " + infoBean);
          infoBean.notify();
        }
      }
    };
    CDialerDataManager.getInstance().queryTracksByOrderNum(context, phoneNum, iDataResponseListener);
    LogUtil.d(TAG, "syncQueryTrackByPhoneNum 1 infoBean isSessionEnd: " + infoBean.isSessionEnd());
    LogUtil.d(TAG, "syncQueryTrackByPhoneNum 1 infoBean: " + infoBean);
    if (infoBean.isSessionEnd()) {
      return infoBean.convertTrackInfo();
    }
    synchronized (infoBean) {
      try {
        infoBean.wait();
        LogUtil.d(TAG, "syncQueryTrackByPhoneNum 2 infoBean: " + infoBean);
      } catch (Exception e) {
        LogUtil.d(TAG, "syncQueryTrackByPhoneNum Exception: ", e);
      }
    }
    LogUtil.d(TAG, "syncQueryTrackByPhoneNum 3 infoBean: " + infoBean);
    return infoBean.convertTrackInfo();
  }
}
