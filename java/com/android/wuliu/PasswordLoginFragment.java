package com.android.wuliu;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.R;
import com.android.dialer.common.LogUtil;
import com.google.gson.Gson;
import com.urovo.dialercloud.IDataResponseListener;
import com.urovo.dialercloud.mine.model.EmployeeModel;
import com.urovo.dialercloud.net.DialerNetRequest;
import com.urovo.dialercloud.net.responseBean.AccountLoginResp;
import com.urovo.dialercloud.util.SettingsUtil;


/**
 * 手机验号 + 密码登录
 */
public class PasswordLoginFragment extends Fragment implements View.OnClickListener {

  private static final String TAG = "PasswordLoginFragment";

  private OnLoginEventListener onEventListener;

  private EditText phoneNumberEditView;
  private EditText passwordEditView;
  private String phoneNum;
  private String pwd;
  private LinearLayout layoutErrTips;
  private TextView errorInfoView;

  public PasswordLoginFragment() {
    // Required empty public constructor
  }

  public static PasswordLoginFragment newInstance() {
    //PasswordLoginFragment fragment = new PasswordLoginFragment();
    //Bundle args = new Bundle();
    //fragment.setArguments(args);
    return new PasswordLoginFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View rootView = inflater.inflate(R.layout.fragment_phone_pwd_login, container, false);
    rootView.findViewById(R.id.tv_verifycode_login).setOnClickListener(this);
    rootView.findViewById(R.id.btn_login_confirm).setOnClickListener(this);

    phoneNumberEditView = rootView.findViewById(R.id.et_phone_number);
    passwordEditView = rootView.findViewById(R.id.et_password);
    layoutErrTips = rootView.findViewById(R.id.linear_err_tips);
    errorInfoView = rootView.findViewById(R.id.tv_info_err_tips);
    return rootView;
  }

  @Override
  public void onClick(View v) {
    int viewId = v.getId();
    if (viewId == R.id.tv_verifycode_login) {
      //切换成手机验证码登录
      if (onEventListener != null) {
        onEventListener.onSwitchLoginWay(OnLoginEventListener.VERIFY_CODE_LOGIN);
      }
    } else if (viewId == R.id.btn_login_confirm) {
      userLogin();
    }
  }

  private void userLogin() {
    //账号密码不正确
    if (!checkLoginParams()) {
      showErrTips("账号或者密码错误！");
      return;
    }

    DialerNetRequest.getInstance().loginByTelePhoneNum(getActivity(), phoneNum, pwd,
      new IDataResponseListener<AccountLoginResp>() {
        @Override
        public void onDataResponse(AccountLoginResp data) {
          boolean isSuccess = data.isSuccess() && data.getResult() != null;
          LogUtil.d(TAG, "userLogin onDataResponse isSuccess=" + isSuccess + "; data: " + data);
          if (isSuccess) {
            EmployeeModel employeeMode = data.getResult();
            //登录成功后，保存登录的token,加密秘钥，员工信息
            SettingsUtil.setSettingValues(getActivity(), SettingsUtil.DIALER_STRING_TOKEN,
              employeeMode.getToken());
            SettingsUtil.setSettingValues(getActivity(), SettingsUtil.DIALER_SETTING_APPEND_KEY,
              employeeMode.getSecret());
            SettingsUtil.setSettingValues(getActivity(), SettingsUtil.DIALER_SETTING_EMPLOEE_INFO,
              new Gson().toJson(employeeMode));
          }
          if (onEventListener != null) {
            onEventListener.onLoginResult(isSuccess);
          }
        }

        @Override
        public void onError(String errCode, String message) {
          showErrTips(message);
        }
      });
  }

  /**
   * 校验登录参数
   */
  private boolean checkLoginParams() {
    phoneNum = phoneNumberEditView.getText().toString().trim();
    pwd = passwordEditView.getText().toString().trim();
    //账号和密码为空
    return !TextUtils.isEmpty(phoneNum) && !TextUtils.isEmpty(pwd);
  }

  public void setOnEventListener(OnLoginEventListener onEventListener) {
    this.onEventListener = onEventListener;
  }

  //错误信息展示两秒后隐藏
  private void showErrTips(String errInfo) {
    getActivity().runOnUiThread(() -> {
      errorInfoView.setText(errInfo);
      layoutErrTips.setVisibility(View.VISIBLE);
      //2秒后再隐藏
      layoutErrTips.postDelayed(
        () -> layoutErrTips.setVisibility(View.INVISIBLE), 2 * 1000);
    });
  }
}