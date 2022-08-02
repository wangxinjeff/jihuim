package com.hyphenate.easeim.section.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.model.EMOrder;
import com.hyphenate.easeui.adapter.EaseBaseRecyclerViewAdapter;

public class OrderListAdapter extends EaseBaseRecyclerViewAdapter<EMOrder> {

    private OnOrderClickSendListener listener;

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new OrderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.em_order_item, parent, false));
    }

    public void setListener(OnOrderClickSendListener listener){
        this.listener = listener;
    }

    class OrderViewHolder extends EaseBaseRecyclerViewAdapter.ViewHolder<EMOrder>{
        private AppCompatTextView orderTitle;
        private AppCompatTextView orderName;
        private AppCompatTextView orderDate;
        private AppCompatTextView btnSend;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void initView(View itemView) {
            orderTitle = findViewById(R.id.order_title);
            orderName = findViewById(R.id.order_name);
            orderDate = findViewById(R.id.order_date);
            btnSend = findViewById(R.id.send_btn);
        }

        @Override
        public void setData(EMOrder item, int position) {
            orderTitle.setText(item.getId() + "订单");
            orderName.setText("商品名称: " + item.getName());
            orderDate.setText("下单日期: " + item.getDate());
            btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null){
                        listener.onClick(item);
                    }
                }
            });
        }
    }

    public interface OnOrderClickSendListener{
        void onClick(EMOrder order);
    }
}
