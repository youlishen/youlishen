package com.android.wuliu;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.R;
import com.android.dialer.common.LogUtil;
import com.google.gson.Gson;
import com.urovo.dialercloud.IDataResponseListener;
import com.urovo.dialercloud.mine.model.EmployeeModel;
import com.urovo.dialercloud.net.DialerNetRequest;
import com.urovo.dialercloud.net.responseBean.GetCaptchaResp;
import com.urovo.dialercloud.net.responseBean.VerifyCodeLoginResp;
import com.urovo.dialercloud.util.OperationCountdownTimer;
import com.urovo.dialercloud.util.PhoneFormatCheckUtils;
import com.urovo.dialercloud.util.SettingsUtil;


/**
 * 手机验号 + 验证码登录
 *
 * @author daiquan
 */
public class VerifyCodeLoginFragment extends Fragment implements View.OnClickListener,
  OperationCountdownTimer.Callback {

  private static final String TAG = "VerifyCodeLoginFragment";
  private OnLoginEventListener onEventListener;

  private EditText phoneNumberEditView;
  private EditText verifyCodeEditView;
  private String phoneNum;
  private String verifyCode;

  private LinearLayout layoutErrTips;
  private TextView errInfoView;
  private Button btnGetVerifyCode;

  private OperationCountdownTimer countdownTimer;

  public VerifyCodeLoginFragment() {
    // Required empty public constructor
  }

  public static VerifyCodeLoginFragment newInstance() {
    //VerifyCodeLoginFragment fragment = new VerifyCodeLoginFragment();
    //Bundle args = new Bundle();
    //fragment.setArguments(args);
    return new VerifyCodeLoginFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View rootView = inflater.inflate(R.layout.fragment_verifycode_login, container, false);
    rootView.findViewById(R.id.tv_pwd_login).setOnClickListener(this);
    rootView.findViewById(R.id.btn_login_confirm).setOnClickListener(this);

    phoneNumberEditView = rootView.findViewById(R.id.et_phone_number);
    verifyCodeEditView = rootView.findViewById(R.id.et_verifycode);
    layoutErrTips = rootView.findViewById(R.id.linear_err_tips);
    errInfoView = rootView.findViewById(R.id.tv_info_err_tips);
    btnGetVerifyCode = rootView.findViewById(R.id.btn_get_verifycode);
    btnGetVerifyCode.setOnClickListener(this);
    btnGetVerifyCode.setClickable(true);

    return rootView;
  }


  @Override
  public void onClick(View v) {
    LogUtil.d(TAG, "onClick view: " + v);
    int viewId = v.getId();
    if (viewId == R.id.tv_pwd_login) {
      if (onEventListener != null) {
        onEventListener.onSwitchLoginWay(OnLoginEventListener.PASSWORD_LOGIN);
      }
    } else if (viewId == R.id.btn_login_confirm) {
      userLogin();
    } else if (viewId == R.id.btn_get_verifycode) {
      if (checkNumberParams()) {
        btnGetVerifyCode.setClickable(false);
        startCountDown();
        DialerNetRequest.getInstance().getVerifycode(getActivity(), phoneNum,
          new IDataResponseListener<GetCaptchaResp>() {
            @Override
            public void onDataResponse(GetCaptchaResp data) {
              LogUtil.d(TAG, "onClick onDataResponse data: " + data);
            }

            @Override
            public void onError(String errCode, String message) {
              showErrTips(message);
            }
          });
      } else {
        showErrTips("请输入正确的手机号码");
      }
    }
  }

  private void userLogin() {
    LogUtil.d(TAG, "userLogin");
    //账号密码不正确
    if (!checkLoginParams()) {
      showErrTips("手机号或者验证码不正确！");
      return;
    }

    DialerNetRequest.getInstance().loginByCaptcha(getActivity(), phoneNum, verifyCode,
      new IDataResponseListener<VerifyCodeLoginResp>() {
        @Override
        public void onDataResponse(VerifyCodeLoginResp data) {
          boolean isSuccess = data != null && data.isSuccess();
          LogUtil.d(TAG, "userLogin onDataResponse isSuccess=" + isSuccess +"; data: " + data);
          if (isSuccess) {
            EmployeeModel employeeMode = data.getResult();
            //登录成功后，保存登录的token,加密秘钥，员工信息
            SettingsUtil.setSettingValues(getActivity(), SettingsUtil.DIALER_STRING_TOKEN,
              employeeMode.getToken());
            SettingsUtil.setSettingValues(getActivity(), SettingsUtil.DIALER_SETTING_APPEND_KEY,
              employeeMode.getSecret());
            SettingsUtil.setSettingValues(getActivity(), SettingsUtil.DIALER_SETTING_EMPLOEE_INFO,
              new Gson().toJson(employeeMode));
            if (onEventListener != null) {
              onEventListener.onLoginResult(true);
            }
          } else {
            showErrTips("登录异常，请稍后再试");
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
   *
   * @return
   */
  private boolean checkLoginParams() {
    phoneNum = phoneNumberEditView.getText().toString().trim();
    verifyCode = verifyCodeEditView.getText().toString().trim();
    return !TextUtils.isEmpty(phoneNum) && !TextUtils.isEmpty(verifyCode)
      && PhoneFormatCheckUtils.isChinaPhoneLegal(phoneNum);
  }


  /**
   * 校验登录参数
   *
   * @return
   */
  private boolean checkNumberParams() {
    phoneNum = phoneNumberEditView.getText().toString().trim();
    return PhoneFormatCheckUtils.isChinaPhoneLegal(phoneNum);
  }

  public void setOnEventListener(OnLoginEventListener onEventListener) {
    this.onEventListener = onEventListener;
  }

  @Override
  public void onTick(long millisUntilFinished) {
    getActivity().runOnUiThread(() -> btnGetVerifyCode.setText(
      getString(R.string.wuliu_verify_code_again_time, millisUntilFinished / 1000)));
  }

  @Override
  public void onCountdownFinish() {
    getActivity().runOnUiThread(() -> {
      btnGetVerifyCode.setClickable(true);
      btnGetVerifyCode.setText(R.string.wuliu_obtain_verify_code);
    });
  }

  //错误信息展示两秒后隐藏
  private void showErrTips(String errInof) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        errInfoView.setText(errInof);
        layoutErrTips.setVisibility(View.VISIBLE);
        //2秒后再隐藏
        layoutErrTips.postDelayed(new Runnable() {
          @Override
          public void run() {
            layoutErrTips.setVisibility(View.INVISIBLE);
          }
        }, 2 * 1000);
      }
    });
  }

  /**
   * 启动倒计时
   */
  protected void startCountDown() {
    cancelCountDown();
    countdownTimer = new OperationCountdownTimer(60000, 1000, this);
    countdownTimer.start();
  }

  /**
   * 停止倒计时
   */
  protected void cancelCountDown() {
    if (countdownTimer != null) {
      countdownTimer.release();
      countdownTimer = null;
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    cancelCountDown();
  }
}