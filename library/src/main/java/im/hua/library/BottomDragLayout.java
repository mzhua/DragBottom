package im.hua.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import im.hua.library.utils.DensityUtil;

/**
 * Created by hua on 16/1/27.
 */
public class BottomDragLayout extends ViewGroup {

    private ViewDragHelper mViewDragHelper;

    private View mShadowView;
    private View mContentView;
    private View mBottomView;
    private Point mAutoBackOriginPos = new Point();

    private int mBottomInitialHeight;

    private int mMinAlpha = 0;
    private int mMaxAlpha = 200;

    private boolean mIsBottomViewOpen = false;

    /**
     * 当BottomView的Width小于Layout时是否居中
     */
    private boolean mGravityCenter = true;

    /**
     * BottomView展开时距离顶部距离
     * 单位：dp
     */
    private int mFinalMarginTop = 48;

    public BottomDragLayout(Context context) {
        super(context);
        init();
    }

    public BottomDragLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BottomDragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BottomDragLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setFocusableInTouchMode(true);

        mBottomInitialHeight = DensityUtil.dp2px(getContext(), 48);

        mShadowView = new View(getContext());
        mShadowView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mShadowView.setBackgroundColor(Color.rgb(0, 0, 0));
        mShadowView.setAlpha(0);
        mShadowView.setClickable(false);
        mShadowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBottomView();
            }
        });

        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return child == mBottomView;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                if (child == mBottomView) {
                    return child.getLeft();
                }
                return left;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                int topBound = getMeasuredHeight() - mBottomView.getMeasuredHeight();
                int finalMarginTopPx = DensityUtil.dp2px(getContext(), mFinalMarginTop);
                topBound = (topBound < getPaddingTop() + finalMarginTopPx ? getPaddingTop() + finalMarginTopPx : topBound);

                int bottomBound = getMeasuredHeight() - mBottomInitialHeight;
                bottomBound = (bottomBound < getPaddingTop() ? getPaddingTop() : bottomBound);

                return Math.min((Math.max(top, topBound)), bottomBound);
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                if (releasedChild == mBottomView) {
                    int settleTop;
                    if (getMeasuredHeight() - mBottomView.getTop() > (mBottomView.getMeasuredHeight() / 2)) {
                        settleTop = getMeasuredHeight() - mBottomView.getMeasuredHeight();
                    } else {
                        settleTop = mAutoBackOriginPos.y;
                    }
                    int finalMarginTopPx = DensityUtil.dp2px(getContext(), mFinalMarginTop);
                    settleTop = (settleTop < getPaddingTop() + finalMarginTopPx ? getPaddingTop() + finalMarginTopPx : settleTop);
                    mViewDragHelper.settleCapturedViewAt(mAutoBackOriginPos.x, settleTop);
                    invalidate();
                }
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                if (changedView == mBottomView) {
                    int alp = (mMaxAlpha - mMinAlpha) * (getMeasuredHeight() - mBottomInitialHeight - top) / (getMeasuredHeight() - mBottomInitialHeight) + mMinAlpha;
                    if (alp >= mMinAlpha && alp <= mMaxAlpha) {
                        Log.d("BottomDragLayout", "alp:" + alp);
                        Log.d("BottomDragLayout", "1 - 1.0f * alp / 255:" + (1 - 1.0f * alp / 255));
//                        mShadowView.setBackgroundColor(Color.argb(alp, 0, 0, 0));
                        mShadowView.setAlpha(1.0f * alp / 255);
                    }
                }
            }

            /**
             * 将ShadowView的位置设置到底部，防止其阻止ContentView的点击事件，因为将其设置为Gone会导致重新刷新界面
             * @param state
             */
            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
                switch (state) {
                    case ViewDragHelper.STATE_IDLE:
                        if (mShadowView.getAlpha() == (1.0f * mMinAlpha / 255)) {
                            mIsBottomViewOpen = false;
                            mShadowView.setTop(getBottom());
                        } else {
                            mIsBottomViewOpen = true;
                        }
                        break;
                    case ViewDragHelper.STATE_DRAGGING:
                        mShadowView.setTop(getTop());
                        break;
                    case ViewDragHelper.STATE_SETTLING:
                        mShadowView.setTop(getTop());
                        break;
                }
            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                int horizontalRange = getMeasuredWidth() - child.getMeasuredWidth();
                return horizontalRange <= getPaddingLeft() ? getMeasuredWidth() : horizontalRange;
            }

            @Override
            public int getViewVerticalDragRange(View child) {
                int verticalRange = getMeasuredHeight() - child.getMeasuredHeight();
                return verticalRange <= getPaddingTop() ? getMeasuredHeight() : verticalRange;
            }
        });
        mViewDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    }

    private void hideBottomView() {
        mIsBottomViewOpen = false;
        int finalLeft = 0;
        int finalTop = getMeasuredHeight() - mBottomInitialHeight;

        mViewDragHelper.smoothSlideViewTo(mBottomView, finalLeft, finalTop);
        invalidate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    /**
     * 计算所有ChildView的宽度和高度 然后根据ChildView的计算结果，设置自己的宽和高
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /**
         * 获得此ViewGroup上级容器为其推荐的宽和高，以及计算模式
         */
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        /**
         * 计算出所有的childView的宽和高
         */
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        /**
         * 用于当ViewGroup设置为wrap_content时，设置其size
         */
        int width = 0;
        int height = 0;

        int childCounts = getChildCount();
        for (int i = 0; i < childCounts; i++) {
            if (i == 0) {
                width = getChildAt(0).getMeasuredWidth();
                height = getChildAt(0).getMeasuredHeight();
            }
        }

        /**
         * 如果此ViewGroup的height设置为wrap_content，则此ViewGroup的height和width的最大值为ContentView的大小，并且BottomView出现的位置也是从ContentView的底部开始出现
         */
        setMeasuredDimension((widthMode == MeasureSpec.EXACTLY) ? widthSize : width,
                (heightMode == MeasureSpec.EXACTLY) ? heightSize : height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int childCounts = getChildCount();
        for (int i = 0; i < childCounts; i++) {
            View childView = getChildAt(i);
            int left = 0;
            int right = 0;
            int top = 0;
            int bottom = 0;
            switch (i) {
                case 0:
                    right = childView.getMeasuredWidth();
                    bottom = childView.getMeasuredHeight() - mBottomInitialHeight;
                    break;
                case 1:
                    top = getBottom();
                    right = getMeasuredWidth();
                    bottom = getMeasuredHeight();
                    break;
                case 2:
                    if (mGravityCenter) {
                        left = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - childView.getMeasuredWidth()) / 2;
                    } else {
                        left = childView.getPaddingLeft();
                    }
                    right = left + childView.getMeasuredWidth();

                    if (childView.getMeasuredHeight() < mBottomInitialHeight) {
                        top = getMeasuredHeight() - childView.getMeasuredHeight();
                    } else {
                        top = getMeasuredHeight() - mBottomInitialHeight;
                    }
                    bottom = top + childView.getMeasuredHeight() - DensityUtil.dp2px(getContext(), mFinalMarginTop);

                    mAutoBackOriginPos.x = left;
                    mAutoBackOriginPos.y = top;

                    break;
            }
            childView.layout(left, top, right, bottom);
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && mIsBottomViewOpen) {
            hideBottomView();
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new IllegalArgumentException("this layout can only contain 2 child views");
        }
        mContentView = getChildAt(0);
        mBottomView = getChildAt(1);

        addView(mShadowView, 1);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }
}
