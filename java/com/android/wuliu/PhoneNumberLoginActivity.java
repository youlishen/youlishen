package com.android.wuliu;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.TextView;

import com.android.dialer.R;
import com.urovo.dialercloud.base.BaseActivity;


/**
 * 电话号码登录页面
 */
public class PhoneNumberLoginActivity extends BaseActivity
        implements OnLoginEventListener {

    private FragmentManager fragmentManager;
    private PasswordLoginFragment passwordLoginFragment;
    private VerifyCodeLoginFragment verifyCodeLoginFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wu_liu_activity_phone_number_login);
        fragmentManager = getFragmentManager();
        passwordLoginFragment = PasswordLoginFragment.newInstance();
        verifyCodeLoginFragment = VerifyCodeLoginFragment.newInstance();
        passwordLoginFragment.setOnEventListener(this);
        verifyCodeLoginFragment.setOnEventListener(this);

        fragmentManager.beginTransaction().add(
                R.id.fragment_container, passwordLoginFragment).commit();

        ((TextView) findViewById(R.id.tv_title)).setText(R.string.dialer_login_phone);
        findViewById(R.id.ib_left_action).setOnClickListener(view -> onBackPressed());
    }

    @Override
    public void onSwitchLoginWay(int i) {
        if (i == OnLoginEventListener.PASSWORD_LOGIN) {
            //切换手机号+密码登录
            replaceFragment(passwordLoginFragment);
        } else {
            //切换手机号+验证码登录
            replaceFragment(verifyCodeLoginFragment);
        }
    }

    @Override
    public void onLoginResult(boolean isSuccess) {
        if (isSuccess) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }
}