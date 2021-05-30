package com.android.wuliu;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.dialer.R;


public class WuLiuOrderInfoFragment extends Fragment {

    private WuLiuOrderInfoBean bean;
    private AlertDialog alertDialog;

    public static Fragment newInstance(WuLiuOrderInfoBean bean) {
        WuLiuOrderInfoFragment fragment = new WuLiuOrderInfoFragment();
//        Bundle bundle = new Bundle();
//        bundle.putParcelable("order_info", bean);
//        fragment.setArguments(bundle);
        fragment.bean = bean;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (bean == null) {
            return null;
        }
        View view = inflater.inflate(R.layout.wu_liu_incall_order_info_fragment, container, false);

        TextView orderStatus = view.findViewById(R.id.incall_order_status);
        TextView orderView = view.findViewById(R.id.incall_order_view);

        TextView orderNumber = view.findViewById(R.id.incall_order_info_number);
        TextView name = view.findViewById(R.id.incall_contacts_info_name);
        TextView address = view.findViewById(R.id.incall_address_info);

        orderView.setOnClickListener(this::showWuLiuInfoDialog);

        orderStatus.setText(bean.getOrderStatus());
        name.setText(bean.getName());
        orderNumber.setText(bean.getOrderNumber());
        address.setText(bean.getAddress());
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        alertDialog = null;
    }

    private void showWuLiuInfoDialog(View view) {
        if (bean.getTrackList() == null || bean.getTrackList().size() == 0) {
            Toast.makeText(getActivity(), "没有查询到订单信息", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.WuLiuDialog);
        View detailView = LayoutInflater.from(getActivity())
          .inflate(R.layout.wu_liu_track_detail_layout, null, false);
        detailView.findViewById(R.id.incall_track_detail_close)
          .setOnClickListener(v -> alertDialog.dismiss());
        TextView title = detailView.findViewById(R.id.incall_track_detail_title);
        title.setText(getString(R.string.wuliu_search_order_number_title) + bean.getOrderNumber());

        WuLiuTrackView trackView = detailView.findViewById(R.id.detail_info_view);
        trackView.setTrackList(bean.getTrackList());
        builder.setView(detailView);
        alertDialog = builder.create();
        alertDialog.show();
    }
}
