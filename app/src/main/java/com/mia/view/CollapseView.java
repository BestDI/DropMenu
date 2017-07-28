package com.mia.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.res.TypedArrayUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mia.dropmenu.R;
import com.mia.util.Utils;
import com.nineoldandroids.view.ViewPropertyAnimator;

/**
 * 实现一个可以折叠和展开部分内容的View
 */
@SuppressLint("NewApi")
public class CollapseView extends LinearLayout {

    private Context mContext;

    private long duration = 200;

    private TextView mNumberTextView;

    private TextView mTitleTextView;

    private ImageView mArrowImageView;

    private RelativeLayout mContentRelativeLayout;
    private int contentView;

    public CollapseView(Context context) {
        this(context, null);
    }

    public CollapseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initView(context);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.CollapseView);
        if (typedArray != null) {
            contentView = typedArray.getResourceId(R.styleable.CollapseView_contentView, -1);
            // typedArray.getResourceId(R.styleable.collapseview)
            typedArray.recycle();
        }
    }

    private void initView(Context context) {
        mContext = context;
        LayoutInflater.from(mContext)
                .inflate(R.layout.view_collapse_layout, this);
        mNumberTextView = (TextView) findViewById(R.id.numberTextView);
        mTitleTextView = (TextView) findViewById(R.id.titleTextView);
        mContentRelativeLayout = (RelativeLayout) findViewById(R.id.contentRelativeLayout);
        if (contentView != -1) {
            setContent(contentView);
        }
        mArrowImageView = (ImageView) findViewById(R.id.arrowImageView);
        mArrowImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateArrow();
            }
        });
        collapse(mContentRelativeLayout);
    }

    //设置编号
    public void setNumber(String number) {
        if (!TextUtils.isEmpty(number)) {
            mNumberTextView.setText(number);
        }
    }

    //设置标题
    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTitleTextView.setText(title);
        }
    }

    //设置内容
    public void setContent(int resID) {
        View view = LayoutInflater.from(mContext)
                .inflate(resID, null);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(layoutParams);
        mContentRelativeLayout.addView(view);
    }


    //显示或者隐藏View,且同时改变箭头方向
    public void rotateArrow() {
        int degree = 0;
        // 反转箭头
        if (mArrowImageView.getTag() == null || mArrowImageView.getTag()
                .equals(true)) {
            mArrowImageView.setTag(false);
            degree = -180;
            expand(mContentRelativeLayout);
        } else {
            degree = 0;
            mArrowImageView.setTag(true);
            collapse(mContentRelativeLayout);
        }
        ViewPropertyAnimator.animate(mArrowImageView)
                .setDuration(duration)
                .rotation(degree);
    }

    /**
     * 展开View.
     * <p>
     * 需要注意的问题:
     * 在该处对于View的测量从而获得measuredHeight.
     * 1 View的宽度为屏幕的宽度(即为一个确定值),所以:
     * MeasureSpec.makeMeasureSpec(Utils.getScreenWidth(mContext), MeasureSpec.EXACTLY);
     * 得到widthMeasureSpec.
     * 2 View的高度为wrap_content.可以利用:
     * MeasureSpec.makeMeasureSpec((1<<30)-1, MeasureSpec.AT_MOST)
     * 得到heightMeasureSpec.
     * 此处的mode为MeasureSpec.AT_MOST,所以利用(1<<30)-1作为size.
     * 这样做才能使系统获取到View的真实高度.
     * <p>
     * 比如在TextView的源码就有这样的处理:
     * if (heightMode == MeasureSpec.AT_MOST) {
     * height = Math.min(desired, heightSize);
     * }
     * <p>
     * 这里会取desired和heightSize这两者的较小值赋值给height.
     * <p>
     * heightSize就是我们传进去的(1<<30)-1
     * desired是通过getDesiredHeight()方法获得的.
     * <p>
     * 小结如下:
     * 若View的宽或高是wrap_content我们手动调用它的measure都可以这样:
     * int widthMeasureSpec=MeasureSpec.makeMeasureSpec((1<<30)-1, MeasureSpec.AT_MOST);
     * int heightMeasureSpec=MeasureSpec.makeMeasureSpec((1<<30)-1,MeasureSpec.AT_MOST);
     * view.measure(widthMeasureSpec,heightMeasureSpec);
     * int measuredWidth =   view.getMeasuredWidth();
     * int measuredHeight = view.getMeasuredHeight();
     */
    private void expand(final View view) {
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                Utils.getScreenWidth(mContext), MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec((1 << 30) - 1, MeasureSpec.AT_MOST);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        final int measuredHeight = view.getMeasuredHeight();
        view.setVisibility(View.VISIBLE);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.getLayoutParams().height = measuredHeight;
                } else {
                    view.getLayoutParams().height = (int) (measuredHeight * interpolatedTime);
                }
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration(duration);
        view.startAnimation(animation);
    }

    // 折叠
    private void collapse(final View view) {
        final int measuredHeight = view.getMeasuredHeight();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = measuredHeight -
                            (int) (measuredHeight * interpolatedTime);
                }
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration(duration);
        view.startAnimation(animation);
    }
}
