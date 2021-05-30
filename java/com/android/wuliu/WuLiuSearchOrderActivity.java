package com.android.wuliu;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.android.contacts.common.dialog.ClearFrequentsDialog;
import com.android.dialer.R;
import com.android.dialer.app.MainComponent;
import com.android.dialer.app.calllog.CallLogActivity;
import com.android.dialer.app.settings.DialerSettingsActivity;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.logging.UiAction;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.simulator.Simulator;
import com.android.dialer.simulator.SimulatorComponent;
import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.util.TransactionSafeActivity;

public class WuLiuSearchOrderActivity extends TransactionSafeActivity
  implements PopupMenu.OnMenuItemClickListener {
  private static final String TAG = "WuLiuSearchOrderActivity";
  private EditText searchBox;
  private PopupMenu overflowMenu;
  private TextView searchNoDataView;
  private View searchDataView;
  private TextView orderNumber;
  private TextView name;
  private TextView address;
  private final Intent intent = new Intent();
  private final Bundle bundle = new Bundle();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.wu_liu_search_order_activity);
    getWindow().setBackgroundDrawable(null);
    final ActionBar actionBar = getActionBarSafely();
    actionBar.setCustomView(R.layout.wu_liu_search_order_action_bar);
    actionBar.setDisplayShowCustomEnabled(true);
    searchBox = actionBar.getCustomView().findViewById(R.id.wu_liu_search_view);
    searchBox.setOnEditorActionListener((v, actionId, event) -> {
      LogUtil.d(TAG, "onEditorAction actionId=" + actionId + "; event: " + event);
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        queryOrderNumber();
      }
      return true;
    });
    View menuView = actionBar.getCustomView().findViewById(R.id.wu_liu_search_menu);
    menuView.setOnClickListener((v) -> overflowMenu.show());
    overflowMenu = buildOptionsMenu(menuView);
    //actionBar.setBackgroundDrawable(null);

    searchNoDataView = findViewById(R.id.wu_liu_search_no_data);
    searchDataView = findViewById(R.id.wu_liu_order_layout);

    orderNumber = findViewById(R.id.wu_liu_order_number_content);
    name = findViewById(R.id.wu_liu_order_name_content);
    address = findViewById(R.id.wu_liu_order_address_content);

    searchDataView.setVisibility(View.GONE);
    searchNoDataView.setVisibility(View.GONE);
  }

  @NonNull
  private ActionBar getActionBarSafely() {
    return Assert.isNotNull(getSupportActionBar());
  }

  @Override
  protected void onResume() {
    super.onResume();
    searchBox.requestFocus();
    new Handler().postDelayed(this::showInputMethod, 500);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    LogUtil.d(TAG, "onBackPressed isFinishing=" + isFinishing());
    if (!isFinishing()) {
      queryOrderNumber();
    }
  }

  private void queryOrderNumber() {
    WuLiuExecutor.execute(
      () -> {
        WuLiuOrderInfoBean bean = WuLiuManager.getInstance().syncQueryOrderByOrderNum(
          searchBox.getText().toString());
        if (bean == null) {
          runOnUiThread(() -> Toast.makeText(getApplicationContext(),
            R.string.wuliu_get_bill_info_failed, Toast.LENGTH_SHORT).show());
        } else if (bean.getException() != null) {
          runOnUiThread(() -> Toast.makeText(getApplicationContext(),
            getString(R.string.wuliu_get_bill_info_exception, bean.getException()),
            Toast.LENGTH_SHORT).show());
        }
        runOnUiThread(() -> updateView(bean));
      });
  }

  private void updateView(WuLiuOrderInfoBean bean) {
    if (bean == null || !TextUtils.isEmpty(bean.getException())) {
      searchNoDataView.setVisibility(View.VISIBLE);
      searchDataView.setVisibility(View.GONE);
      return;
    }
    searchNoDataView.setVisibility(View.GONE);
    searchDataView.setVisibility(View.VISIBLE);
    bundle.putString(WuLiuContant.KEY_ORDER_NUMBER, bean.getOrderNumber());
    bundle.putString(WuLiuContant.KEY_NAME, bean.getName());
    bundle.putString(WuLiuContant.KEY_ADDRESS, bean.getAddress());
    bundle.putString(WuLiuContant.KEY_PHONE_NUMBER, bean.getPhoneNumber());
    setResult(RESULT_OK, intent.putExtras(bundle));
    name.setText(bean.getName());
    orderNumber.setText(bean.getOrderNumber());
    address.setText(bean.getAddress());
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onPause() {
    super.onPause();

  }

  public void showInputMethod() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInputFromWindow(searchBox.getWindowToken(),
      0, InputMethodManager.SHOW_FORCED);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    LogUtil.d(TAG, "onMenuItemClick item: " + item);
    if (!isSafeToCommitTransactions()) {
      return true;
    }
    int resId = item.getItemId();
    if (resId == R.id.menu_history) {
      PerformanceReport.recordClick(UiAction.Type.OPEN_CALL_HISTORY);
      final Intent intent = new Intent(this, CallLogActivity.class);
      startActivity(intent);
    } else if (resId == R.id.menu_clear_frequents) {
      ClearFrequentsDialog.show(getFragmentManager());
      return true;
    } else if (resId == R.id.menu_call_settings) {
      final Intent intent = new Intent(this, DialerSettingsActivity.class);
      startActivity(intent);
      return true;
    } else if (resId == R.id.menu_new_ui_launcher_shortcut) {
      MainComponent.createNewUiLauncherShortcut(this);
      return true;
    }
    return false;
  }

  protected OptionsPopupMenu buildOptionsMenu(View invoker) {
    final OptionsPopupMenu popupMenu = new OptionsPopupMenu(this, invoker);
    popupMenu.inflate(R.menu.dialtacts_options);
    popupMenu.setOnMenuItemClickListener(this);
    return popupMenu;
  }

  protected class OptionsPopupMenu extends PopupMenu {

    public OptionsPopupMenu(Context context, View anchor) {
      super(context, anchor, Gravity.END);
    }

    @Override
    public void show() {
      Menu menu = getMenu();
      MenuItem clearFrequents = menu.findItem(R.id.menu_clear_frequents);
      clearFrequents.setVisible(false);

      menu.findItem(R.id.menu_history)
        .setVisible(PermissionsUtil.hasPhonePermissions(WuLiuSearchOrderActivity.this));

      Context context = WuLiuSearchOrderActivity.this.getApplicationContext();
      MenuItem simulatorMenuItem = menu.findItem(R.id.menu_simulator_submenu);
      Simulator simulator = SimulatorComponent.get(context).getSimulator();
      if (simulator.shouldShow()) {
        simulatorMenuItem.setVisible(true);
        simulatorMenuItem.setActionProvider(simulator.getActionProvider(WuLiuSearchOrderActivity.this));
      } else {
        simulatorMenuItem.setVisible(false);
      }

      menu.findItem(R.id.menu_new_ui_launcher_shortcut)
        .setVisible(MainComponent.isNewUiEnabled(context));

      super.show();
    }
  }
}
