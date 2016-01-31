package im.hua.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
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

    private int mBottomInitialHeight;

    private int mMinAlpha = 0;
    private int mMaxAlpha = 180;

    private boolean mIsBottomViewOpen = false;

    /**
     * 当BottomView的Width小于Layout时是否居中
     */
    private boolean mGravityCenter = true;

    /**
     * BottomView展开时距离顶部距离
     * 单位：dp
     */
    private float mFinalMarginTop = 48f;
    private int mFinalMarginTopPx;

    private State state = State.HIDDEN;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public enum State {
        HIDDEN,
        EXPANDED,
        PEEKED,
        SETTLING
    }

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

        mBottomInitialHeight = DensityUtil.dp2px(getContext(), 56);
        mFinalMarginTopPx = DensityUtil.dp2px(getContext(), mFinalMarginTop);

        mShadowView = new View(getContext());
        mShadowView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mShadowView.setBackgroundColor(Color.rgb(0, 0, 0));
        mShadowView.setAlpha(0);
        mShadowView.setClickable(false);
        mShadowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setState(State.SETTLING);
                hideBottomView();
            }
        });

        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                boolean capture = (child == mBottomView);
                if (capture) {
                    setState(State.PEEKED);
                }
                return capture;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                return child.getLeft();
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                int topBound = getMeasuredHeight() - mBottomView.getMeasuredHeight();
                topBound = (topBound < getPaddingTop() + mFinalMarginTopPx ? getPaddingTop() + mFinalMarginTopPx : topBound);

                int bottomBound = getMeasuredHeight() - mBottomInitialHeight;
                bottomBound = (bottomBound < getPaddingTop() ? getPaddingTop() : bottomBound);

                return Math.min((Math.max(top, topBound)), bottomBound);
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                setState(State.SETTLING);
                if (releasedChild == mBottomView) {
                    int settleTop;
                    if (yvel < 0) {
                        if (yvel <= -10 || (yvel < 0 && getMeasuredHeight() - mBottomView.getTop() > (mBottomView.getMeasuredHeight() / 2))) {
                            //展开
                            settleTop = getMeasuredHeight() - mBottomView.getMeasuredHeight();
                        } else {
                            settleTop = getMeasuredHeight() - mBottomInitialHeight;
                        }
                    } else {
                        if (yvel >= 10 || (yvel >= 0 && getMeasuredHeight() - mBottomView.getTop() <= (mBottomView.getMeasuredHeight() / 2))) {
                            settleTop = getMeasuredHeight() - mBottomInitialHeight;
                        } else {
                            settleTop = getMeasuredHeight() - mBottomView.getMeasuredHeight();
                        }
                    }
                    settleTop = (settleTop < getPaddingTop() + mFinalMarginTopPx ? getPaddingTop() + mFinalMarginTopPx : settleTop);
                    mViewDragHelper.settleCapturedViewAt(mBottomView.getLeft(), settleTop);
                    invalidate();
                }
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                if (changedView == mBottomView) {
                    int alp = (mMaxAlpha - mMinAlpha) * (getMeasuredHeight() - mBottomInitialHeight - top) / (mBottomView.getMeasuredHeight() - mBottomInitialHeight) + mMinAlpha;
                    if (alp >= mMinAlpha && alp <= mMaxAlpha) {
                        mShadowView.setAlpha(1.0f * alp / 255);
                    }
                }
            }

            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
                switch (state) {
                    case ViewDragHelper.STATE_IDLE:
                        if (mShadowView.getAlpha() == (1.0f * mMinAlpha / 255)) {
                            mIsBottomViewOpen = false;
                            setState(State.HIDDEN);
                            // 将ShadowView的位置设置到底部，防止其阻止ContentView的点击事件，因为将其设置为Gone会导致重新刷新界面
                            mShadowView.setTop(getBottom());
                        } else {
                            mIsBottomViewOpen = true;
                            setState(State.EXPANDED);
                        }
                        break;
                    case ViewDragHelper.STATE_DRAGGING:
                        setState(State.PEEKED);
                        mShadowView.setTop(getTop());
                        break;
                    case ViewDragHelper.STATE_SETTLING:
                        setState(State.SETTLING);
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
                int verticalRange = child.getMeasuredHeight() - mBottomInitialHeight;
                return verticalRange <= getPaddingTop() ? getMeasuredHeight() : verticalRange;
            }
        });
    }

    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        measureChildren(widthMeasureSpec, heightMeasureSpec);

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
                    top = getBottom();//default set the shadow view to the bottom,so it won't interrupt the click ecent
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
                    bottom = top + childView.getMeasuredHeight() - mFinalMarginTopPx;
                    break;
            }
            childView.layout(left, top, right, bottom);
        }
    }

    private boolean mShouldIntercept = true;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mShouldIntercept && mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_MOVE && getState() == State.EXPANDED) {
            mBottomView.dispatchTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && getState() == State.EXPANDED) {
            hideBottomView();
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new IllegalArgumentException("this ViewGroup can only contains 2 child views");
        }
        mContentView = getChildAt(0);
        mBottomView = getChildAt(1);
        mBottomView.setClickable(true);
        ViewCompat.setElevation(mBottomView, DensityUtil.dp2px(getContext(), 16f));

        mBottomView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mShouldIntercept = interceptBottomView(v);
                return false;
            }
        });

        addView(mShadowView, 1);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    private boolean interceptBottomView(View view) {
        if (view != null) {
            if (view.getScrollY() > 0) {
                return false;
            }
        }
        return true;
    }

    public void hideBottomView() {
        if (mIsBottomViewOpen) {
            mIsBottomViewOpen = false;
            mViewDragHelper.smoothSlideViewTo(mBottomView, mBottomView.getLeft(), getMeasuredHeight() - mBottomInitialHeight);
            invalidate();
        }
    }

    public void showBottomView() {
        if (!mIsBottomViewOpen) {
            mIsBottomViewOpen = true;
            int settleTop = getMeasuredHeight() - mBottomView.getMeasuredHeight();
            settleTop = (settleTop < getPaddingTop() + mFinalMarginTopPx ? getPaddingTop() + mFinalMarginTopPx : settleTop);
            mViewDragHelper.smoothSlideViewTo(mBottomView, mBottomView.getLeft(), settleTop);
            invalidate();
        }
    }
}
