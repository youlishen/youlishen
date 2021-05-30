package com.android.wuliu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.android.dialer.R;
import com.android.dialer.common.LogUtil;
import com.urovo.dialercloud.base.BaseActivity;

/**
 * 首次进入应用进入登录页面
 * 登录页面登录后获取token,然后使用token更新个人信息
 * 再次进入应用，判断是否有登录的token，若有则使用token去请求更新个人信息，
 * 直接进入dialer主界面,若无则需再次进入登录页面进行登录
 * 退出登录清除token
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "LoginActivity";

    private static final int REQUEST_CODE_PHONE_NUMBER_LOGIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.wu_liu_activity_dialer_login);
    }

    @Override
    public void onClick(View view) {
        int resId = view.getId();
        if (resId == R.id.btn_login_by_phone) {
            //手机号登录
            Intent intent = new Intent(this, PhoneNumberLoginActivity.class);
            startActivityForResult(intent, REQUEST_CODE_PHONE_NUMBER_LOGIN);
        } else if (resId == R.id.btn_login_by_app) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d(TAG, "onActivityResult requestCode=" + requestCode + "; resultCode=" + resultCode);
        if (requestCode == REQUEST_CODE_PHONE_NUMBER_LOGIN && resultCode == RESULT_OK) {
            if (!WuLiuManager.getInstance().isNeedLogin()) {
                finish();
            }
        }
    }
}