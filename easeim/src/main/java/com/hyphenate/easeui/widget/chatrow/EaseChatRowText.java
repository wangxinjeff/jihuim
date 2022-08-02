package com.hyphenate.easeui.widget.chatrow;

import android.content.Context;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chat.EMTranslationResult;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.common.utils.ToastUtils;
import com.hyphenate.easeim.section.base.WebViewActivity;
import com.hyphenate.easeui.manager.EaseDingMessageHelper;
import com.hyphenate.easeui.utils.EaseSmileUtils;
import com.hyphenate.util.EMLog;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EaseChatRowText extends EaseChatRow {
	private TextView contentView;
    private boolean triggerLongClick = false;

    public EaseChatRowText(Context context, boolean isSender) {
		super(context, isSender);
	}

    public EaseChatRowText(Context context, EMMessage message, int position, Object adapter) {
		super(context, message, position, adapter);
	}

	@Override
	protected void onInflateView() {
		inflater.inflate(!showSenderType ? R.layout.ease_row_received_message
                : R.layout.ease_row_sent_message, this);
	}

	@Override
	protected void onFindViewById() {
		contentView = (TextView) findViewById(R.id.tv_chatcontent);
	}

    @Override
    public void onSetUpView() {
        EMTextMessageBody txtBody = (EMTextMessageBody) message.getBody();
        if(txtBody != null){
            Spannable span = EaseSmileUtils.getSmiledText(context, txtBody.getMessage());
            // 设置内容
            contentView.setText(span, BufferType.SPANNABLE);
            contentView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    triggerLongClick = true;
                    contentView.setTag(R.id.action_chat_long_click,true);
                    if (itemClickListener != null) {
                        return itemClickListener.onBubbleLongClick(v, message);
                    }
                    return false;
                }
            });
            replaceSpan();
            contentView.setMovementMethod(new LinkMovementMethodEx(new LinkClickListener() {
                @Override
                public boolean onLinkClick(String mURL) {
                    WebViewActivity.actionStart(context, mURL);
                    return true;
                }
            }));
        }
    }

    /**
     * 解决长按事件与relink冲突，参考：https://www.jianshu.com/p/d3bef8449960
     */
    private void replaceSpan() {
        Spannable spannable = (Spannable) contentView.getText();
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (int i = 0; i < spans.length; i++) {
            String url = spans[i].getURL();
            int index = spannable.toString().indexOf(url);
            int end = index + url.length();
            if (index == -1) {
                if (url.contains("http://")) {
                    url = url.replace("http://", "");
                } else if (url.contains("https://")) {
                    url = url.replace("https://", "");
                } else if (url.contains("rtsp://")) {
                    url = url.replace("rtsp://", "");
                }
                index = spannable.toString().indexOf(url);
                end = index + url.length();
            }
            if (index != -1) {
                spannable.removeSpan(spans[i]);
                spannable.setSpan(new AutolinkSpan(spans[i].getURL()), index
                        , end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
    }

    @Override
    protected void onMessageCreate() {
        setStatus(View.VISIBLE, View.GONE);
    }

    @Override
    protected void onMessageSuccess() {
        super.onMessageSuccess();
        setStatus(View.GONE, View.GONE);

        // Show "1 Read" if this msg is a ding-type msg.
//        if (isSender() && EaseDingMessageHelper.get().isDingMessage(message) && ackedView != null && ackedBg != null) {
//            ackedView.setVisibility(VISIBLE);
//            ackedBg.setVisibility(VISIBLE);
//            int count = message.groupAckCount();
//            ackedView.setText(count == 0 ? "" : count+"");
//        }

        // Set ack-user list change listener.
        // Only use the group ack count from message. - 2022.04.27
//        EaseDingMessageHelper.get().setUserUpdateListener(message, userUpdateListener);
    }

    @Override
    protected void onMessageError() {
        super.onMessageError();
        setStatus(View.GONE, View.VISIBLE);
    }

    @Override
    protected void onMessageInProgress() {
        setStatus(View.VISIBLE, View.GONE);
    }

    /**
     * set progress and status view visible or gone
     * @param progressVisible
     * @param statusVisible
     */
    private void setStatus(int progressVisible, int statusVisible) {
        if(progressBar != null) {
            progressBar.setVisibility(progressVisible);
        }
        if(statusView != null) {
            statusView.setVisibility(statusVisible);
        }
    }

    private EaseDingMessageHelper.IAckUserUpdateListener userUpdateListener = list -> onAckUserUpdate(list);

    public void onAckUserUpdate(List<String> userList) {
        if(ackedView == null) {
            return;
        }
        ackedView.post(()->{
            if (isSender()) {
                ackedView.setVisibility(VISIBLE);
                ackedView.setText(userList.size() == 0 ? "" : userList.size()+"");

            }
        });
    }

    public interface LinkClickListener {
        boolean onLinkClick(String mURL);
    }

    public class LinkMovementMethodEx extends LinkMovementMethod {
        private LinkClickListener listener;

        public LinkMovementMethodEx(LinkClickListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();
                x += widget.getScrollX();
                y += widget.getScrollY();
                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);
                if (links.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        if (links[0] instanceof URLSpan) {
                            URLSpan url = (URLSpan) links[0];
                            if (!triggerLongClick && listener != null)
                            {
                                listener.onLinkClick(url.getURL());
                                return true;
                            } else {
                                triggerLongClick = false;
                            }
                        }
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(links[0]),
                                buffer.getSpanEnd(links[0]));
                    }
                    return true;
                } else {
                    Selection.removeSelection(buffer);
                }
            }
            return false;
        }
    }
}
