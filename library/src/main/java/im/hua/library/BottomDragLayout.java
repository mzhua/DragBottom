package im.hua.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

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

    /**
     * 0 ~ 255
     */
    private int mShadowMinAlpha;

    /**
     * 0 ~ 255
     */
    private int mShadowMaxAlpha;

    /**
     * 0:left
     * 1:center
     * 2:right
     */
    private int mBottomViewGravity;

    /**
     * BottomView展开时距离顶部距离
     */
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
        this(context, null);
    }

    public BottomDragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomDragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BottomDragLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BottomDragLayout, defStyleAttr, 0);

        mShadowMinAlpha = a.getInt(R.styleable.BottomDragLayout_shadowMinAlpha, 0);
        mShadowMaxAlpha = a.getInt(R.styleable.BottomDragLayout_shadowMaxAlpha, 180);
        mBottomViewGravity = a.getInt(R.styleable.BottomDragLayout_bottomViewGravity, 1);
        mFinalMarginTopPx = a.getDimensionPixelSize(R.styleable.BottomDragLayout_finalMarginTop, DensityUtil.dp2px(context, 48));
        mBottomInitialHeight = a.getDimensionPixelSize(R.styleable.BottomDragLayout_bottomInitialHeight, DensityUtil.dp2px(context, 48));

        a.recycle();

        init();
    }

    private void init() {

        setFocusableInTouchMode(true);

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
                    int alp = (mShadowMaxAlpha - mShadowMinAlpha) * (getMeasuredHeight() - mBottomInitialHeight - top) / (mBottomView.getMeasuredHeight() - mBottomInitialHeight) + mShadowMinAlpha;
                    if (alp >= mShadowMinAlpha && alp <= mShadowMaxAlpha) {
                        mShadowView.setAlpha(1.0f * alp / 255);
                    }
                }
            }

            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
                switch (state) {
                    case ViewDragHelper.STATE_IDLE:
                        if (mShadowView.getAlpha() == (1.0f * mShadowMinAlpha / 255)) {
                            setState(State.HIDDEN);
                            // 将ShadowView的位置设置到底部，防止其阻止ContentView的点击事件，因为将其设置为Gone会导致重新刷新界面
                            mShadowView.setTop(getBottom());
                        } else {
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
                    bottom = childView.getMeasuredHeight();// - mBottomInitialHeight;
                    if (childView instanceof ViewGroup) {
                        ViewGroup viewGroup = (ViewGroup) childView;
                        viewGroup.setPadding(viewGroup.getPaddingLeft(), viewGroup.getPaddingTop(), viewGroup.getPaddingRight(), mBottomInitialHeight);
                        viewGroup.setClipToPadding(false);
                    } else {
                        childView.setPadding(childView.getPaddingLeft(), childView.getPaddingTop(), childView.getPaddingRight(), mBottomInitialHeight);
                    }
                    break;
                case 1:
                    top = getBottom();//default set the shadow view to the bottom,so it won't interrupt the click ecent
                    right = getMeasuredWidth();
                    bottom = getMeasuredHeight();
                    break;
                case 2:

                    switch (mBottomViewGravity) {
                        case 0:
                            left = childView.getPaddingLeft();
                            break;
                        case 1:
                            left = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - childView.getMeasuredWidth()) / 2;
                            break;
                        case 2:
                            left = getMeasuredWidth() - getPaddingRight() - childView.getMeasuredWidth();
                            break;
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
            return mBottomView.dispatchTouchEvent(event);
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
            if (view instanceof ScrollView || view instanceof WebView) {
                if (view.getScrollY() > 0) {
                    return false;
                }
            } else if (view instanceof AbsListView) {
                //ListView and GridView
                AbsListView listView = (AbsListView) view;
                listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        if (view.getChildCount() > 0 && view.getChildAt(0).getTop() < 0) {
                            mShouldIntercept = false;
                        } else {
                            mShouldIntercept = true;
                        }
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    }
                });
                if (listView.getChildCount() > 0 && listView.getChildAt(0).getTop() < 0) {
                    return false;
                }
            } else if (view instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) view;
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        if (recyclerView.getChildCount() > 0 && recyclerView.getChildAt(0).getTop() < 0) {
                            mShouldIntercept = false;
                        } else {
                            mShouldIntercept = true;
                        }
                    }
                });
                if (recyclerView.getChildCount() > 0 && recyclerView.getChildAt(0).getTop() < 0) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return true;
    }

    public void hideBottomView() {
        mShouldIntercept = true;
        if (isBottomViewExpand()) {
            if (mBottomView instanceof ScrollView || mBottomView instanceof WebView) {
                mBottomView.scrollTo(0, 0);
            } else if (mBottomView instanceof AbsListView) {
                //ListView and GridView
                AbsListView listView = (AbsListView) mBottomView;
                if (listView.getChildCount() > 0) {
                    listView.smoothScrollToPosition(0);
                }
            } else if (mBottomView instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) mBottomView;
                if (recyclerView.getChildCount() > 0) {
                    recyclerView.scrollToPosition(0);
                }
            }

            mViewDragHelper.smoothSlideViewTo(mBottomView, mBottomView.getLeft(), getMeasuredHeight() - mBottomInitialHeight);
            invalidate();
        }
    }

    public void showBottomView() {
        if (!isBottomViewExpand()) {
            setState(State.EXPANDED);

            int settleTop = getMeasuredHeight() - mBottomView.getMeasuredHeight();
            settleTop = (settleTop < getPaddingTop() + mFinalMarginTopPx ? getPaddingTop() + mFinalMarginTopPx : settleTop);
            mViewDragHelper.smoothSlideViewTo(mBottomView, mBottomView.getLeft(), settleTop);
            invalidate();
        }
    }

    public boolean isBottomViewExpand() {
        return getState() == State.EXPANDED;
    }
}
