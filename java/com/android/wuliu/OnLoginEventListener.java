package com.android.wuliu;

public interface OnLoginEventListener {
  int PASSWORD_LOGIN = 1;
  int VERIFY_CODE_LOGIN = 2;

  void onSwitchLoginWay(int loginWay);

  void onLoginResult(boolean isSuccess);
}
