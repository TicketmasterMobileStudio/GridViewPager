package com.ticketmaster.mobilestudio.gridviewpager;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ScrollView;
import android.widget.Scroller;

import com.ticketmaster.mobilestudio.gridviewpager.GridPagerAdapter.OnBackgroundChangeListener;

@TargetApi(20)
public class GridViewPager extends ViewGroup {
    private static final String TAG = "GridViewPager";
    private static final boolean DEBUG_LIFECYCLE = false;
    private static final boolean DEBUG_TOUCH = false;
    private static final boolean DEBUG_TOUCHSLOP = false;
    private static final boolean DEBUG_SCROLLING = false;
    private static final boolean DEBUG_SETTLING = false;
    private static final boolean DEBUG_LISTENERS = false;
    private static final boolean DEBUG_LAYOUT = false;
    private static final boolean DEBUG_POPULATE = false;
    private static final boolean DEBUG_ADAPTER = false;
    private static final boolean DEBUG_ROUND = false;
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;
    private static final int SLIDE_ANIMATION_DURATION_NORMAL_MS = 300;
    private static final int MIN_DISTANCE_FOR_FLING_DP = 40;
    private static final int MIN_ACCURATE_VELOCITY = 200;
    private static final int[] LAYOUT_ATTRS = new int[]{16842931};
    private static final Interpolator OVERSCROLL_INTERPOLATOR = new GridViewPager.DragFrictionInterpolator();
    private static final Interpolator SLIDE_INTERPOLATOR = new DecelerateInterpolator(2.5F);
    private int mExpectedRowCount;
    private int mExpectedCurrentColumnCount;
    private boolean mConsumeInsets;
    private int mSlideAnimationDurationMs;
    private final Runnable mEndScrollRunnable;
    private final Point mTempPoint1;
    private GridPagerAdapter mAdapter;
    private final Point mCurItem;
    private Point mRestoredCurItem;
    private Parcelable mRestoredAdapterState;
    private ClassLoader mRestoredClassLoader;
    private final SimpleArrayMap<Point, ItemInfo> mItems;
    private final SimpleArrayMap<Point, GridViewPager.ItemInfo> mRecycledItems;
    private final Rect mPopulatedPages;
    private final Rect mPopulatedPageBounds;
    private final Scroller mScroller;
    private GridViewPager.PagerObserver mObserver;
    private int mRowMargin;
    private int mColMargin;
    private boolean mInLayout;
    private boolean mDelayPopulate;
    private int mOffscreenPageCount;
    private boolean mIsBeingDragged;
    private boolean mIsAbleToDrag;
    private final int mTouchSlop;
    private final int mTouchSlopSquared;
    private float mPointerLastX;
    private float mPointerLastY;
    private float mGestureInitialY;
    private float mGestureInitialX;
    private int mGestureInitialScrollY;
    private int mActivePointerId;
    private static final int NO_POINTER = -1;
    private VelocityTracker mVelocityTracker;
    private final int mMinFlingVelocity;
    private final int mMinFlingDistance;
    private final int mMinUsableVelocity;
    private final int mCloseEnough;
    private static final int CLOSE_ENOUGH = 2;
    private boolean mFirstLayout;
    private boolean mCalledSuper;
    private GridViewPager.OnPageChangeListener mOnPageChangeListener;
    private GridViewPager.OnAdapterChangeListener mAdapterChangeListener;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;
    public static final int SCROLL_STATE_CONTENT_SETTLING = 3;
    private int mScrollState;
    private static final int SCROLL_AXIS_X = 0;
    private static final int SCROLL_AXIS_Y = 1;
    private int mScrollAxis;
    private SparseIntArray mRowScrollX;
    private View mScrollingContent;
    private BackgroundController mBackgroundController;
    private WindowInsets mWindowInsets;
    private OnApplyWindowInsetsListener mOnApplyWindowInsetsListener;
    private boolean mAdapterChangeNotificationPending;
    private GridPagerAdapter mOldAdapter;
    private boolean mDatasetChangePending;

    public GridViewPager(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    public GridViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mConsumeInsets = true;
        this.mSlideAnimationDurationMs = 300;
        this.mEndScrollRunnable = new Runnable() {
            public void run() {
                GridViewPager.this.setScrollState(0);
                GridViewPager.this.populate();
            }
        };
        this.mOffscreenPageCount = 1;
        this.mActivePointerId = -1;
        this.mVelocityTracker = null;
        this.mFirstLayout = true;
        this.mScrollState = 0;
        ViewConfiguration vc = ViewConfiguration.get(this.getContext());
        float density = context.getResources().getDisplayMetrics().density;
        this.mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(vc);
        this.mTouchSlopSquared = this.mTouchSlop * this.mTouchSlop;
        this.mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        this.mMinFlingDistance = (int)(40.0F * density);
        this.mMinUsableVelocity = (int)(200.0F * density);
        this.mCloseEnough = (int)(2.0F * density);
        this.mCurItem = new Point();
        this.mItems = new SimpleArrayMap();
        this.mRecycledItems = new SimpleArrayMap();
        this.mPopulatedPages = new Rect();
        this.mPopulatedPageBounds = new Rect();
        this.mScroller = new Scroller(context, SLIDE_INTERPOLATOR, true);
        this.mTempPoint1 = new Point();
        this.setOverScrollMode(1);
        this.mRowScrollX = new SparseIntArray();
        this.mBackgroundController = new BackgroundController();
        this.mBackgroundController.attachTo(this);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mFirstLayout = true;
        this.getParent().requestFitSystemWindows();
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        int childCount = this.getChildCount();

        for(int i = 0; i < childCount; ++i) {
            View child = this.getChildAt(i);
            child.dispatchApplyWindowInsets(insets);
        }

        this.mWindowInsets = insets;
        return insets;
    }

    public void setConsumeWindowInsets(boolean consume) {
        this.mConsumeInsets = consume;
    }

    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener listener) {
        this.mOnApplyWindowInsetsListener = listener;
    }

    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        insets = this.onApplyWindowInsets(insets);
        if(this.mOnApplyWindowInsetsListener != null) {
            this.mOnApplyWindowInsetsListener.onApplyWindowInsets(this, insets);
        }

        return this.mConsumeInsets?insets.consumeSystemWindowInsets():insets;
    }

    public void requestFitSystemWindows() {
    }

    protected void onDetachedFromWindow() {
        this.removeCallbacks(this.mEndScrollRunnable);
        super.onDetachedFromWindow();
    }

    public void setAdapter(GridPagerAdapter adapter) {
        if(this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mObserver);
            this.mAdapter.setOnBackgroundChangeListener((OnBackgroundChangeListener)null);
            this.mAdapter.startUpdate(this);

            for(int oldAdapter = 0; oldAdapter < this.mItems.size(); ++oldAdapter) {
                GridViewPager.ItemInfo wasFirstLayout = (GridViewPager.ItemInfo)this.mItems.valueAt(oldAdapter);
                this.mAdapter.destroyItem(this, wasFirstLayout.positionY, wasFirstLayout.positionX, wasFirstLayout.object);
            }

            this.mAdapter.finishUpdate(this);
            this.mItems.clear();
            this.removeAllViews();
            this.scrollTo(0, 0);
            this.mRowScrollX.clear();
        }

        GridPagerAdapter var4 = this.mAdapter;
        this.mCurItem.set(0, 0);
        this.mAdapter = adapter;
        this.mExpectedRowCount = 0;
        this.mExpectedCurrentColumnCount = 0;
        if(this.mAdapter != null) {
            if(this.mObserver == null) {
                this.mObserver = new GridViewPager.PagerObserver();
            }

            this.mAdapter.registerDataSetObserver(this.mObserver);
            this.mAdapter.setOnBackgroundChangeListener(this.mBackgroundController);
            this.mDelayPopulate = false;
            boolean var5 = this.mFirstLayout;
            this.mFirstLayout = true;
            this.mExpectedRowCount = this.mAdapter.getRowCount();
            if(this.mExpectedRowCount > 0) {
                this.mCurItem.set(0, 0);
                this.mExpectedCurrentColumnCount = this.mAdapter.getColumnCount(this.mCurItem.y);
            }

            if(this.mRestoredCurItem != null) {
                this.mAdapter.restoreState(this.mRestoredAdapterState, this.mRestoredClassLoader);
                this.setCurrentItemInternal(this.mRestoredCurItem.y, this.mRestoredCurItem.x, false, true);
                this.mRestoredCurItem = null;
                this.mRestoredAdapterState = null;
                this.mRestoredClassLoader = null;
            } else if(!var5) {
                this.populate();
            } else {
                this.requestLayout();
            }
        } else if(this.mIsBeingDragged) {
            this.cancelDrag();
        }

        if(var4 != adapter) {
            if(adapter == null) {
                this.mAdapterChangeNotificationPending = false;
                this.adapterChanged(var4, adapter);
                this.mOldAdapter = null;
            } else {
                this.mAdapterChangeNotificationPending = true;
                this.mOldAdapter = var4;
            }
        } else {
            this.mAdapterChangeNotificationPending = false;
            this.mOldAdapter = null;
        }

    }

    public GridPagerAdapter getAdapter() {
        return this.mAdapter;
    }

    public void setOnPageChangeListener(GridViewPager.OnPageChangeListener listener) {
        this.mOnPageChangeListener = listener;
    }

    public void setOnAdapterChangeListener(GridViewPager.OnAdapterChangeListener listener) {
        this.mAdapterChangeListener = listener;
        if(listener != null && this.mAdapter != null && !this.mAdapterChangeNotificationPending) {
            listener.onAdapterChanged((GridPagerAdapter)null, this.mAdapter);
        }

    }

    private void adapterChanged(GridPagerAdapter oldAdapter, GridPagerAdapter newAdapter) {
        if(this.mAdapterChangeListener != null) {
            this.mAdapterChangeListener.onAdapterChanged(oldAdapter, newAdapter);
        }

        if(this.mBackgroundController != null) {
            this.mBackgroundController.onAdapterChanged(oldAdapter, newAdapter);
        }

    }

    public void scrollTo(int x, int y) {
        if(this.mScrollState == 2 && this.mScrollAxis == 1) {
            x = this.getRowScrollX(this.mCurItem.y);
        }

        super.scrollTo(0, y);
        this.scrollCurrentRowTo(x);
    }

    private void setScrollState(int newState) {
        if(this.mScrollState != newState) {
            this.mScrollState = newState;
            if(this.mOnPageChangeListener != null) {
                this.mOnPageChangeListener.onPageScrollStateChanged(newState);
            }

            if(this.mBackgroundController != null) {
                this.mBackgroundController.onPageScrollStateChanged(newState);
            }

        }
    }

    private int getRowScrollX(int row) {
        return this.mRowScrollX.get(row, 0);
    }

    private void setRowScrollX(int row, int scrollX) {
        this.mRowScrollX.put(row, scrollX);
    }

    private void scrollRowTo(int row, int x) {
        if(this.getRowScrollX(row) != x) {
            int size = this.getChildCount();
            int scrollAmount = x - this.getRowScrollX(row);

            for(int i = 0; i < size; ++i) {
                View child = this.getChildAt(i);
                GridViewPager.ItemInfo ii = this.infoForChild(child);
                if(ii != null && ii.positionY == row) {
                    child.offsetLeftAndRight(-scrollAmount);
                    this.postInvalidateOnAnimation();
                }
            }

            this.setRowScrollX(row, x);
        }
    }

    private void scrollCurrentRowTo(int x) {
        this.scrollRowTo(this.mCurItem.y, x);
    }

    private int getContentWidth() {
        return this.getMeasuredWidth() - (this.getPaddingLeft() + this.getPaddingRight());
    }

    private int getContentHeight() {
        return this.getMeasuredHeight() - (this.getPaddingTop() + this.getPaddingBottom());
    }

    public void setCurrentItem(int row, int column) {
        this.mDelayPopulate = false;
        this.setCurrentItemInternal(row, column, !this.mFirstLayout, false);
    }

    public void setCurrentItem(int row, int column, boolean smoothScroll) {
        this.mDelayPopulate = false;
        this.setCurrentItemInternal(row, column, smoothScroll, false);
    }

    public Point getCurrentItem() {
        return new Point(this.mCurItem);
    }

    void setCurrentItemInternal(int row, int column, boolean smoothScroll, boolean always) {
        this.setCurrentItemInternal(row, column, smoothScroll, always, 0);
    }

    void setCurrentItemInternal(int row, int column, boolean smoothScroll, boolean always, int velocity) {
        if(this.mAdapter != null && this.mAdapter.getRowCount() > 0) {
            if(always || !this.mCurItem.equals(column, row) || this.mItems.size() == 0) {
                row = limit(row, 0, this.mAdapter.getRowCount() - 1);
                column = limit(column, 0, this.mAdapter.getColumnCount(row) - 1);
                boolean dispatchSelected;
                if(column != this.mCurItem.x) {
                    this.mScrollAxis = 0;
                    dispatchSelected = true;
                } else if(row != this.mCurItem.y) {
                    this.mScrollAxis = 1;
                    dispatchSelected = true;
                } else {
                    dispatchSelected = false;
                }

                if(this.mFirstLayout) {
                    this.mCurItem.set(0, 0);
                    this.mAdapter.setCurrentColumnForRow(row, column);
                    if(dispatchSelected) {
                        if(this.mOnPageChangeListener != null) {
                            this.mOnPageChangeListener.onPageSelected(row, column);
                        }

                        if(this.mBackgroundController != null) {
                            this.mBackgroundController.onPageSelected(row, column);
                        }
                    }

                    this.requestLayout();
                } else {
                    this.populate(column, row);
                    this.scrollToItem(column, row, smoothScroll, velocity, dispatchSelected);
                }

            }
        }
    }

    private void scrollToItem(int x, int y, boolean smoothScroll, int velocity, boolean dispatchSelected) {
        GridViewPager.ItemInfo curInfo = this.infoForPosition(x, y);
        int destX = 0;
        int destY = 0;
        if(curInfo != null) {
            destX = this.computePageLeft(curInfo.positionX) - this.getPaddingLeft();
            destY = this.computePageTop(curInfo.positionY) - this.getPaddingTop();
        }

        this.mAdapter.setCurrentColumnForRow(y, x);
        if(dispatchSelected) {
            if(this.mOnPageChangeListener != null) {
                this.mOnPageChangeListener.onPageSelected(y, x);
            }

            if(this.mBackgroundController != null) {
                this.mBackgroundController.onPageSelected(y, x);
            }
        }

        if(smoothScroll) {
            this.smoothScrollTo(destX, destY, velocity);
        } else {
            this.completeScroll(false);
            this.scrollTo(destX, destY);
            this.pageScrolled(destX, destY);
        }

    }

    public int getOffscreenPageCount() {
        return this.mOffscreenPageCount;
    }

    public void setOffscreenPageCount(int limit) {
        if(limit < 1) {
            Log.w("GridViewPager", "Requested offscreen page limit " + limit + " too small; defaulting to " + 1);
            limit = 1;
        }

        if(limit != this.mOffscreenPageCount) {
            this.mOffscreenPageCount = limit;
            this.populate();
        }

    }

    public void setPageMargins(int rowMarginPx, int columnMarginPx) {
        int oldRowMargin = this.mRowMargin;
        this.mRowMargin = rowMarginPx;
        int oldColMargin = this.mColMargin;
        this.mColMargin = columnMarginPx;
        int width = this.getWidth();
        int height = this.getHeight();
        if(!this.mFirstLayout && !this.mItems.isEmpty()) {
            this.recomputeScrollPosition(width, width, height, height, this.mColMargin, oldColMargin, this.mRowMargin, oldRowMargin);
            this.requestLayout();
        }

    }

    public void setSlideAnimationDuration(int slideAnimationDuration) {
        this.mSlideAnimationDurationMs = slideAnimationDuration;
    }

    public int getPageRowMargin() {
        return this.mRowMargin;
    }

    public int getPageColumnMargin() {
        return this.mColMargin;
    }

    void smoothScrollTo(int x, int y) {
        this.smoothScrollTo(x, y, 0);
    }

    void smoothScrollTo(int x, int y, int velocity) {
        if(this.getChildCount() != 0) {
            int sx = this.getRowScrollX(this.mCurItem.y);
            int sy = this.getScrollY();
            int dx = x - sx;
            int dy = y - sy;
            if(dx == 0 && dy == 0) {
                this.completeScroll(false);
                this.populate();
                this.setScrollState(0);
            } else {
                this.setScrollState(2);
                int duration = this.mSlideAnimationDurationMs;
                this.mScroller.startScroll(sx, sy, dx, dy, duration);
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    void flingContent(int limitX, int limitY, int velocityX, int velocityY) {
        if(this.mScrollingContent != null) {
            if(velocityX == 0 && velocityY == 0) {
                this.completeScroll(false);
                this.setScrollState(0);
            } else {
                int sx = this.mScrollingContent.getScrollX();
                int sy = this.mScrollingContent.getScrollY();
                this.setScrollState(3);
                int minX;
                int maxX;
                if(velocityX > 0) {
                    minX = sx;
                    maxX = sx + limitX;
                } else {
                    minX = sx + limitX;
                    maxX = sx;
                }

                int minY;
                int maxY;
                if(velocityY > 0) {
                    minY = sy;
                    maxY = sy + limitY;
                } else {
                    minY = sy + limitY;
                    maxY = sy;
                }

                this.mScroller.fling(sx, sy, velocityX, velocityY, minX, maxX, minY, maxY);
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    private GridViewPager.ItemInfo addNewItem(int positionX, int positionY) {
        Point key = new Point(positionX, positionY);
        GridViewPager.ItemInfo ii = (GridViewPager.ItemInfo)this.mRecycledItems.remove(key);
        if(ii == null) {
            ii = new GridViewPager.ItemInfo();
            ii.object = this.mAdapter.instantiateItem(this, positionY, positionX);
            ii.positionX = positionX;
            ii.positionY = positionY;
        }

        key.set(positionX, positionY);
        ii.positionX = positionX;
        ii.positionY = positionY;
        this.mItems.put(key, ii);
        return ii;
    }

    void rowBackgroundChanged(int row) {
        if(this.mBackgroundController != null) {
            this.mBackgroundController.onRowBackgroundChanged(row);
        }

    }

    void pageBackgroundChanged(int row, int column) {
        if(this.mBackgroundController != null) {
            this.mBackgroundController.onPageBackgroundChanged(row, column);
        }

    }

    private void dataSetChanged() {
        int adapterRowCount = this.mAdapter.getRowCount();
        this.mExpectedRowCount = adapterRowCount;
        Point newCurrItem = new Point(this.mCurItem);
        boolean isUpdating = false;
        SimpleArrayMap newItemMap = new SimpleArrayMap();

        for(int i = this.mItems.size() - 1; i >= 0; --i) {
            Point itemKey = (Point)this.mItems.keyAt(i);
            GridViewPager.ItemInfo itemInfo = (GridViewPager.ItemInfo)this.mItems.valueAt(i);
            Point newItemPos = this.mAdapter.getItemPosition(itemInfo.object);
            this.mAdapter.applyItemPosition(itemInfo.object, newItemPos);
            if(newItemPos == GridPagerAdapter.POSITION_UNCHANGED) {
                newItemMap.put(itemKey, itemInfo);
            } else if(newItemPos == GridPagerAdapter.POSITION_NONE) {
                if(!isUpdating) {
                    this.mAdapter.startUpdate(this);
                    isUpdating = true;
                }

                this.mAdapter.destroyItem(this, itemInfo.positionY, itemInfo.positionX, itemInfo.object);
                if(this.mCurItem.equals(itemInfo.positionX, itemInfo.positionY)) {
                    newCurrItem.y = limit(this.mCurItem.y, 0, Math.max(0, adapterRowCount - 1));
                    if(newCurrItem.y < adapterRowCount) {
                        newCurrItem.x = limit(this.mCurItem.x, 0, this.mAdapter.getColumnCount(newCurrItem.y) - 1);
                    } else {
                        newCurrItem.x = 0;
                    }
                }
            } else if(!newItemPos.equals(itemInfo.positionX, itemInfo.positionY)) {
                if(this.mCurItem.equals(itemInfo.positionX, itemInfo.positionY)) {
                    newCurrItem.set(newItemPos.x, newItemPos.y);
                }

                itemInfo.positionX = newItemPos.x;
                itemInfo.positionY = newItemPos.y;
                newItemMap.put(new Point(newItemPos), itemInfo);
            }
        }

        this.mItems.clear();
        this.mItems.putAll(newItemMap);
        if(isUpdating) {
            this.mAdapter.finishUpdate(this);
        }

        if(this.mExpectedRowCount > 0) {
            this.mExpectedCurrentColumnCount = this.mAdapter.getColumnCount(newCurrItem.y);
        } else {
            this.mExpectedCurrentColumnCount = 0;
        }

        this.dispatchOnDataSetChanged();
        this.setCurrentItemInternal(newCurrItem.y, newCurrItem.x, false, true);
        this.requestLayout();
    }

    private void dispatchOnDataSetChanged() {
        if(this.mAdapterChangeListener != null) {
            this.mAdapterChangeListener.onDataSetChanged();
        }

        if(this.mBackgroundController != null) {
            this.mBackgroundController.onDataSetChanged();
        }

    }

    private void populate() {
        if(this.mAdapter != null && this.mAdapter.getRowCount() > 0) {
            this.populate(this.mCurItem.x, this.mCurItem.y);
        }

    }

    private void cancelDrag() {
        this.cancelPendingInputEvents();
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, 3, 0.0F, 0.0F, 0);
        event.setSource(4098);
        this.dispatchTouchEvent(event);
        event.recycle();
    }

    private void populate(int newX, int newY) {
        Point oldCurItem = new Point();
        if(this.mCurItem.x != newX || this.mCurItem.y != newY) {
            oldCurItem.set(this.mCurItem.x, this.mCurItem.y);
            this.mCurItem.set(newX, newY);
        }

        if(!this.mDelayPopulate) {
            if(this.getWindowToken() != null) {
                this.mAdapter.startUpdate(this);
                this.mPopulatedPageBounds.setEmpty();
                int rowCount = this.mAdapter.getRowCount();
                if(this.mExpectedRowCount != rowCount) {
                    throw new IllegalStateException("Adapter row count changed without a call to notifyDataSetChanged()");
                } else {
                    int colCount = this.mAdapter.getColumnCount(newY);
                    if(colCount < 1) {
                        throw new IllegalStateException("All rows must have at least 1 column");
                    } else {
                        this.mExpectedRowCount = rowCount;
                        this.mExpectedCurrentColumnCount = colCount;
                        int offscreenPages = Math.max(1, this.mOffscreenPageCount);
                        int startPosY = Math.max(0, newY - offscreenPages);
                        int endPosY = Math.min(rowCount - 1, newY + offscreenPages);
                        int startPosX = Math.max(0, newX - offscreenPages);
                        int endPosX = Math.min(colCount - 1, newX + offscreenPages);

                        int i;
                        GridViewPager.ItemInfo ii;
                        for(i = this.mItems.size() - 1; i >= 0; --i) {
                            ii = (GridViewPager.ItemInfo)this.mItems.valueAt(i);
                            if(ii.positionY == newY) {
                                if(ii.positionX >= startPosX && ii.positionX <= endPosX) {
                                    continue;
                                }
                            } else {
                                int key = this.mAdapter.getCurrentColumnForRow(ii.positionY, this.mCurItem.x);
                                if(ii.positionX == key && ii.positionY >= startPosY && ii.positionY <= endPosY) {
                                    continue;
                                }
                            }

                            Point var14 = (Point)this.mItems.keyAt(i);
                            this.mItems.removeAt(i);
                            var14.set(ii.positionX, ii.positionY);
                            this.mRecycledItems.put(var14, ii);
                        }

                        this.mTempPoint1.y = newY;

                        for(this.mTempPoint1.x = startPosX; this.mTempPoint1.x <= endPosX; ++this.mTempPoint1.x) {
                            if(!this.mItems.containsKey(this.mTempPoint1)) {
                                this.addNewItem(this.mTempPoint1.x, this.mTempPoint1.y);
                            }
                        }

                        for(this.mTempPoint1.y = startPosY; this.mTempPoint1.y <= endPosY; ++this.mTempPoint1.y) {
                            this.mTempPoint1.x = this.mAdapter.getCurrentColumnForRow(this.mTempPoint1.y, newX);
                            if(!this.mItems.containsKey(this.mTempPoint1)) {
                                this.addNewItem(this.mTempPoint1.x, this.mTempPoint1.y);
                            }

                            if(this.mTempPoint1.y != this.mCurItem.y) {
                                this.setRowScrollX(this.mTempPoint1.y, this.computePageLeft(this.mTempPoint1.x) - this.getPaddingLeft());
                            }
                        }

                        for(i = this.mRecycledItems.size() - 1; i >= 0; --i) {
                            ii = (GridViewPager.ItemInfo)this.mRecycledItems.removeAt(i);
                            this.mAdapter.destroyItem(this, ii.positionY, ii.positionX, ii.object);
                        }

                        this.mRecycledItems.clear();
                        this.mAdapter.finishUpdate(this);
                        this.mPopulatedPages.set(startPosX, startPosY, endPosX, endPosY);
                        this.mPopulatedPageBounds.set(this.computePageLeft(startPosX) - this.getPaddingLeft(), this.computePageTop(startPosY) - this.getPaddingTop(), this.computePageLeft(endPosX + 1) - this.getPaddingRight(), this.computePageTop(endPosY + 1) + this.getPaddingBottom());
                        if(this.mAdapterChangeNotificationPending) {
                            this.mAdapterChangeNotificationPending = false;
                            this.adapterChanged(this.mOldAdapter, this.mAdapter);
                            this.mOldAdapter = null;
                        }

                        if(this.mDatasetChangePending) {
                            this.mDatasetChangePending = false;
                            this.dispatchOnDataSetChanged();
                        }

                    }
                }
            }
        }
    }

    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        GridViewPager.SavedState state = new GridViewPager.SavedState(superState);
        state.currentX = this.mCurItem.x;
        state.currentY = this.mCurItem.y;
        return state;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof GridViewPager.SavedState)) {
            super.onRestoreInstanceState(state);
        } else {
            GridViewPager.SavedState ss = (GridViewPager.SavedState)state;
            super.onRestoreInstanceState(ss.getSuperState());
            if(this.pointInRange(ss.currentX, ss.currentY)) {
                this.mRestoredCurItem = new Point(ss.currentX, ss.currentY);
            } else {
                this.mCurItem.set(0, 0);
                this.scrollTo(0, 0);
            }

        }
    }

    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        this.infoForChild(child);
        if(!this.checkLayoutParams(params)) {
            params = this.generateLayoutParams(params);
        }

        GridViewPager.LayoutParams lp = (GridViewPager.LayoutParams)params;
        if(this.mInLayout) {
            lp.needsMeasure = true;
            this.addViewInLayout(child, index, params);
        } else {
            super.addView(child, index, params);
        }

        if(this.mWindowInsets != null) {
            child.onApplyWindowInsets(this.mWindowInsets);
        }

    }

    public void removeView(View view) {
        this.infoForChild(view);
        if(this.mInLayout) {
            this.removeViewInLayout(view);
        } else {
            super.removeView(view);
        }

    }

    private GridViewPager.ItemInfo infoForChild(View child) {
        for(int i = 0; i < this.mItems.size(); ++i) {
            GridViewPager.ItemInfo ii = (GridViewPager.ItemInfo)this.mItems.valueAt(i);
            if(ii != null && this.mAdapter.isViewFromObject(child, ii.object)) {
                return ii;
            }
        }

        return null;
    }

    private GridViewPager.ItemInfo infoForPosition(Point p) {
        return (GridViewPager.ItemInfo)this.mItems.get(p);
    }

    private GridViewPager.ItemInfo infoForPosition(int x, int y) {
        this.mTempPoint1.set(x, y);
        return (GridViewPager.ItemInfo)this.mItems.get(this.mTempPoint1);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));
        this.mInLayout = true;
        this.populate();
        this.mInLayout = false;
        int size = this.getChildCount();

        for(int i = 0; i < size; ++i) {
            View child = this.getChildAt(i);
            if(child.getVisibility() != View.GONE) {
                GridViewPager.LayoutParams lp = (GridViewPager.LayoutParams)child.getLayoutParams();
                if(lp != null) {
                    this.measureChild(child, lp);
                }
            }
        }

    }

    public void measureChild(View child, GridViewPager.LayoutParams lp) {
        int childDefaultWidth = this.getContentWidth();
        int childDefaultHeight = this.getContentHeight();
        int widthMode = lp.width == ViewGroup.LayoutParams.WRAP_CONTENT ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
        int heightMode = lp.height == ViewGroup.LayoutParams.WRAP_CONTENT ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
        int widthSpec = MeasureSpec.makeMeasureSpec(childDefaultWidth, widthMode);
        int heightSpec = MeasureSpec.makeMeasureSpec(childDefaultHeight, heightMode);
        int childWidthMeasureSpec = getChildMeasureSpec(widthSpec, lp.leftMargin + lp.rightMargin, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(heightSpec, lp.topMargin + lp.bottomMargin, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(!this.mItems.isEmpty()) {
            this.recomputeScrollPosition(w, oldw, h, oldh, this.mColMargin, this.mColMargin, this.mRowMargin, this.mRowMargin);
        }

    }

    private int computePageLeft(int column) {
        return column * (this.getContentWidth() + this.mColMargin) + this.getPaddingLeft();
    }

    private int computePageTop(int row) {
        return row * (this.getContentHeight() + this.mRowMargin) + this.getPaddingTop();
    }

    private void recomputeScrollPosition(int width, int oldWidth, int height, int oldHeight, int colMargin, int oldColMargin, int rowMargin, int oldRowMargin) {
        int targetX;
        int targetY;
        if(oldWidth > 0 && oldHeight > 0) {
            int ii1 = width - this.getPaddingLeft() - this.getPaddingRight() + colMargin;
            targetX = oldWidth - this.getPaddingLeft() - this.getPaddingRight() + oldColMargin;
            targetY = height - this.getPaddingTop() - this.getPaddingBottom() + rowMargin;
            int oldHeightWithMargin = oldHeight - this.getPaddingTop() - this.getPaddingBottom() + oldRowMargin;
            int xpos = this.getRowScrollX(this.mCurItem.y);
            float pageOffset = (float)xpos / (float)targetX;
            int newOffsetXPixels = (int)(pageOffset * (float)ii1);
            int ypos = this.getScrollY();
            float pageOffsetY = (float)ypos / (float)oldHeightWithMargin;
            int newOffsetYPixels = (int)(pageOffsetY * (float)targetY);
            this.scrollTo(newOffsetXPixels, newOffsetYPixels);
            if(!this.mScroller.isFinished()) {
                GridViewPager.ItemInfo targetInfo = this.infoForPosition(this.mCurItem);
                int targetX1 = this.computePageLeft(targetInfo.positionX) - this.getPaddingLeft();
                int targetY1 = this.computePageTop(targetInfo.positionY) - this.getPaddingTop();
                int newDuration = this.mScroller.getDuration() - this.mScroller.timePassed();
                this.mScroller.startScroll(newOffsetXPixels, newOffsetYPixels, targetX1, targetY1, newDuration);
            }
        } else {
            GridViewPager.ItemInfo ii = this.infoForPosition(this.mCurItem);
            if(ii != null) {
                targetX = this.computePageLeft(ii.positionX) - this.getPaddingLeft();
                targetY = this.computePageTop(ii.positionY) - this.getPaddingTop();
                if(targetX != this.getRowScrollX(ii.positionY) || targetY != this.getScrollY()) {
                    this.completeScroll(false);
                    this.scrollTo(targetX, targetY);
                }
            }
        }

    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int children = this.getChildCount();

        for(int i = 0; i < children; ++i) {
            View view = this.getChildAt(i);
            GridViewPager.LayoutParams lp = (GridViewPager.LayoutParams)view.getLayoutParams();
            if(lp == null) {
                Log.w("GridViewPager", "Got null layout params for child: " + view);
            } else {
                GridViewPager.ItemInfo ii = this.infoForChild(view);
                if(ii == null) {
                    Log.w("GridViewPager", "Unknown child view, not claimed by adapter: " + view);
                } else {
                    if(lp.needsMeasure) {
                        lp.needsMeasure = false;
                        this.measureChild(view, lp);
                    }

                    int left = this.computePageLeft(ii.positionX);
                    int top = this.computePageTop(ii.positionY);
                    left -= this.getRowScrollX(ii.positionY);
                    left += lp.leftMargin;
                    top += lp.topMargin;
                    view.layout(left, top, left + view.getMeasuredWidth(), top + view.getMeasuredHeight());
                }
            }
        }

        if(this.mFirstLayout && !this.mItems.isEmpty()) {
            this.scrollToItem(this.mCurItem.x, this.mCurItem.y, false, 0, false);
        }

        this.mFirstLayout = false;
    }

    public void computeScroll() {
        if(!this.mScroller.isFinished() && this.mScroller.computeScrollOffset()) {
            int oldX;
            int oldY;
            if(this.mScrollState == 3) {
                if(this.mScrollingContent == null) {
                    this.mScroller.abortAnimation();
                } else {
                    oldX = this.mScroller.getCurrX();
                    oldY = this.mScroller.getCurrY();
                    this.mScrollingContent.scrollTo(oldX, oldY);
                }
            } else {
                oldX = this.getRowScrollX(this.mCurItem.y);
                oldY = this.getScrollY();
                int x = this.mScroller.getCurrX();
                int y = this.mScroller.getCurrY();
                if(oldX != x || oldY != y) {
                    this.scrollTo(x, y);
                    if(!this.pageScrolled(x, y)) {
                        this.mScroller.abortAnimation();
                        this.scrollTo(0, 0);
                    }
                }
            }

            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            this.completeScroll(true);
        }
    }

    private static String scrollStateToString(int state) {
        switch(state) {
            case 0:
                return "IDLE";
            case 1:
                return "DRAGGING";
            case 2:
                return "SETTLING";
            case 3:
                return "CONTENT_SETTLING";
            default:
                return "";
        }
    }

    private boolean pageScrolled(int xpos, int ypos) {
        if(this.mItems.size() == 0) {
            this.mCalledSuper = false;
            this.onPageScrolled(0, 0, 0.0F, 0.0F, 0, 0);
            if(!this.mCalledSuper) {
                throw new IllegalStateException("onPageScrolled did not call superclass implementation");
            } else {
                return false;
            }
        } else {
            GridViewPager.ItemInfo ii = this.infoForCurrentScrollPosition();
            int pageLeft = this.computePageLeft(ii.positionX);
            int pageTop = this.computePageTop(ii.positionY);
            int offsetLeftPx = xpos + this.getPaddingLeft() - pageLeft;
            int offsetTopPx = ypos + this.getPaddingTop() - pageTop;
            float offsetLeft = this.getXIndex((float)offsetLeftPx);
            float offsetTop = this.getYIndex((float)offsetTopPx);
            this.mCalledSuper = false;
            this.onPageScrolled(ii.positionX, ii.positionY, offsetLeft, offsetTop, offsetLeftPx, offsetTopPx);
            if(!this.mCalledSuper) {
                throw new IllegalStateException("onPageScrolled did not call superclass implementation");
            } else {
                return true;
            }
        }
    }

    public void onPageScrolled(int positionX, int positionY, float offsetX, float offsetY, int offsetLeftPx, int offsetTopPx) {
        this.mCalledSuper = true;
        if(this.mOnPageChangeListener != null) {
            this.mOnPageChangeListener.onPageScrolled(positionY, positionX, offsetY, offsetX, offsetTopPx, offsetLeftPx);
        }

        if(this.mBackgroundController != null) {
            this.mBackgroundController.onPageScrolled(positionY, positionX, offsetY, offsetX, offsetTopPx, offsetLeftPx);
        }

    }

    private void completeScroll(boolean postEvents) {
        boolean needPopulate = this.mScrollState == 2;
        if(needPopulate) {
            this.mScroller.abortAnimation();
            int oldX = this.getRowScrollX(this.mCurItem.y);
            int oldY = this.getScrollY();
            int x = this.mScroller.getCurrX();
            int y = this.mScroller.getCurrY();
            if(oldX != x || oldY != y) {
                this.scrollTo(x, y);
            }
        }

        this.mScrollingContent = null;
        this.mDelayPopulate = false;
        if(needPopulate) {
            if(postEvents) {
                ViewCompat.postOnAnimation(this, this.mEndScrollRunnable);
            } else {
                this.mEndScrollRunnable.run();
            }
        }

    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction() & 255;
        if(action != 3 && action != 1) {
            if(action != 0) {
                if(this.mIsBeingDragged) {
                    return true;
                }

                if(!this.mIsAbleToDrag) {
                    return false;
                }
            }

            switch(action) {
                case 0:
                    this.handlePointerDown(ev);
                    break;
                case 2:
                    this.handlePointerMove(ev);
                    break;
                case 6:
                    this.onSecondaryPointerUp(ev);
            }

            return this.mIsBeingDragged;
        } else {
            this.mIsBeingDragged = false;
            this.mIsAbleToDrag = false;
            this.mActivePointerId = -1;
            if(this.mVelocityTracker != null) {
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }

            return false;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if(this.mAdapter == null) {
            return false;
        } else {
            int action = ev.getAction();
            switch(action & 255) {
                case 0:
                    this.handlePointerDown(ev);
                    break;
                case 1:
                case 3:
                    this.handlePointerUp(ev);
                    break;
                case 2:
                    this.handlePointerMove(ev);
                    break;
                case 4:
                case 5:
                default:
                    Log.e("GridViewPager", "Unknown action type: " + action);
                    break;
                case 6:
                    this.onSecondaryPointerUp(ev);
            }

            return true;
        }
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        ViewParent parent = this.getParent();
        if(parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }

    }

    private static float limit(float input, int limit) {
        return limit > 0?Math.max(0.0F, Math.min(input, (float)limit)):Math.min(0.0F, Math.max(input, (float)limit));
    }

    private boolean performDrag(float x, float y) {
        float deltaX = this.mPointerLastX - x;
        float deltaY = this.mPointerLastY - y;
        this.mPointerLastX = x;
        this.mPointerLastY = y;
        Rect pages = this.mPopulatedPages;
        int leftBound = this.computePageLeft(pages.left) - this.getPaddingLeft();
        int rightBound = this.computePageLeft(pages.right) - this.getPaddingLeft();
        int topBound = this.computePageTop(pages.top) - this.getPaddingTop();
        int bottomBound = this.computePageTop(pages.bottom) - this.getPaddingTop();
        float scrollX = (float)this.getRowScrollX(this.mCurItem.y);
        float scrollY = (float)this.getScrollY();
        int targetY;
        boolean wouldOverscroll;
        float overscrollY;
        if(this.mScrollAxis == 1) {
            targetY = this.getContentHeight() + this.mRowMargin;
            float targetX;
            if(deltaY < 0.0F) {
                targetX = -(scrollY % (float)targetY);
            } else {
                targetX = ((float)targetY - scrollY % (float)targetY) % (float)targetY;
            }

            wouldOverscroll = false;
            if(Math.abs(targetX) <= Math.abs(deltaY)) {
                deltaY -= targetX;
                scrollY += targetX;
                wouldOverscroll = true;
            }

            if(wouldOverscroll) {
                View mode = this.getChildForInfo(this.infoForScrollPosition((int)scrollX, (int)scrollY));
                if(mode != null) {
                    int couldScroll = (int)Math.signum(deltaY);
                    int overscrollX = this.getScrollableDistance(mode, couldScroll);
                    overscrollY = limit(deltaY, overscrollX);
                    mode.scrollBy(0, (int)overscrollY);
                    deltaY -= overscrollY;
                    this.mPointerLastY += overscrollY - (float)((int)overscrollY);
                }
            }
        }

        int targetX1 = (int)(scrollX + (float)((int)deltaX));
        targetY = (int)(scrollY + (float)((int)deltaY));
        wouldOverscroll = targetX1 < leftBound || targetX1 > rightBound || targetY < topBound || targetY > bottomBound;
        if(wouldOverscroll) {
            int mode1 = this.getOverScrollMode();
            boolean couldScroll1 = this.mScrollAxis == 0 && leftBound < rightBound || this.mScrollAxis == 1 && topBound < bottomBound;
            if(mode1 != 0 && (!couldScroll1 || mode1 != 1)) {
                deltaX = limit(deltaX, (float)leftBound - scrollX, (float)rightBound - scrollX);
                deltaY = limit(deltaY, (float)topBound - scrollY, (float)bottomBound - scrollY);
            } else {
                float overscrollX1 = scrollX > (float)rightBound?scrollX - (float)rightBound:(scrollX < (float)leftBound?scrollX - (float)leftBound:0.0F);
                overscrollY = scrollY > (float)bottomBound?scrollY - (float)bottomBound:(scrollY < (float)topBound?scrollY - (float)topBound:0.0F);
                if(Math.abs(overscrollX1) > 0.0F && Math.signum(overscrollX1) == Math.signum(deltaX)) {
                    deltaX *= OVERSCROLL_INTERPOLATOR.getInterpolation(1.0F - Math.abs(overscrollX1) / (float)this.getContentWidth());
                }

                if(Math.abs(overscrollY) > 0.0F && Math.signum(overscrollY) == Math.signum(deltaY)) {
                    deltaY *= OVERSCROLL_INTERPOLATOR.getInterpolation(1.0F - Math.abs(overscrollY) / (float)this.getContentHeight());
                }
            }
        }

        scrollX += deltaX;
        scrollY += deltaY;
        this.mPointerLastX += scrollX - (float)((int)scrollX);
        this.mPointerLastY += scrollY - (float)((int)scrollY);
        this.scrollTo((int)scrollX, (int)scrollY);
        this.pageScrolled((int)scrollX, (int)scrollY);
        return true;
    }

    private int getScrollableDistance(View child, int dir) {
        int scrollable = 0;
        if(child instanceof CardScrollView) {
            scrollable = ((CardScrollView)child).getAvailableScrollDelta(dir);
        } else if(child instanceof ScrollView) {
            scrollable = this.getScrollableDistance((ScrollView)child, dir);
        }

        return scrollable;
    }

    private int getScrollableDistance(ScrollView view, int direction) {
        int distance = 0;
        if(view.getChildCount() > 0) {
            View content = view.getChildAt(0);
            int height = view.getHeight();
            int contentHeight = content.getHeight();
            int extra = contentHeight - height;
            if(contentHeight > height) {
                if(direction > 0) {
                    distance = Math.min(extra - view.getScrollY(), 0);
                } else if(direction < 0) {
                    distance = -view.getScrollY();
                }
            }
        }

        return distance;
    }

    private View getChildForInfo(GridViewPager.ItemInfo ii) {
        if(ii.object != null) {
            int childCount = this.getChildCount();

            for(int i = 0; i < childCount; ++i) {
                View child = this.getChildAt(i);
                if(this.mAdapter.isViewFromObject(child, ii.object)) {
                    return child;
                }
            }
        }

        return null;
    }

    private GridViewPager.ItemInfo infoForCurrentScrollPosition() {
        int y = (int)this.getYIndex((float)this.getScrollY());
        return this.infoForScrollPosition(this.getRowScrollX(y), this.getScrollY());
    }

    private GridViewPager.ItemInfo infoForScrollPosition(int scrollX, int scrollY) {
        int y = (int)this.getYIndex((float)scrollY);
        int x = (int)this.getXIndex((float)scrollX);
        GridViewPager.ItemInfo ii = this.infoForPosition(x, y);
        if(ii == null) {
            ii = new GridViewPager.ItemInfo();
            ii.positionX = x;
            ii.positionY = y;
        }

        return ii;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = MotionEventCompat.getActionIndex(ev);
        int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if(pointerId == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0?1:0;
            this.mPointerLastX = MotionEventCompat.getX(ev, newPointerIndex);
            this.mPointerLastY = MotionEventCompat.getY(ev, newPointerIndex);
            this.mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
            if(this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }

    }

    private void endDrag() {
        this.mIsBeingDragged = false;
        this.mIsAbleToDrag = false;
        if(this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }

    }

    public boolean canScrollHorizontally(int direction) {
        if(this.getVisibility() == VISIBLE && this.mAdapter != null && !this.mItems.isEmpty()) {
            int scrollX = this.getRowScrollX(this.mCurItem.y);
            int lastColumnIndex = this.mExpectedCurrentColumnCount - 1;
            return direction > 0?scrollX + this.getPaddingLeft() < this.computePageLeft(lastColumnIndex):scrollX > 0;
        } else {
            return false;
        }
    }

    public boolean canScrollVertically(int direction) {
        if(this.getVisibility() == VISIBLE && this.mAdapter != null && !this.mItems.isEmpty()) {
            int scrollY = this.getScrollY();
            int lastRowIndex = this.mExpectedRowCount - 1;
            return direction > 0?scrollY + this.getPaddingTop() < this.computePageTop(lastRowIndex):scrollY > 0;
        } else {
            return false;
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event) || this.executeKeyEvent(event);
    }

    private boolean executeKeyEvent(KeyEvent event) {
        boolean handled = false;
        switch(event.getKeyCode()) {
            case 19:
                handled = this.pageUp();
                break;
            case 20:
                handled = this.pageDown();
                break;
            case 21:
                handled = this.pageLeft();
                break;
            case 22:
                handled = this.pageRight();
            case 61:
            default:
                break;
            case 62:
                this.debug();
                return true;
        }

        return handled;
    }

    private boolean pageLeft() {
        if(this.mCurItem.x > 0) {
            this.setCurrentItem(this.mCurItem.x - 1, this.mCurItem.y, true);
            return true;
        } else {
            return false;
        }
    }

    private boolean pageRight() {
        if(this.mAdapter != null && this.mCurItem.x < this.mAdapter.getColumnCount(this.mCurItem.y) - 1) {
            this.setCurrentItem(this.mCurItem.x + 1, this.mCurItem.y, true);
            return true;
        } else {
            return false;
        }
    }

    private boolean pageUp() {
        if(this.mCurItem.y > 0) {
            this.setCurrentItem(this.mCurItem.x, this.mCurItem.y - 1, true);
            return true;
        } else {
            return false;
        }
    }

    private boolean pageDown() {
        if(this.mAdapter != null && this.mCurItem.y < this.mAdapter.getRowCount() - 1) {
            this.setCurrentItem(this.mCurItem.x, this.mCurItem.y + 1, true);
            return true;
        } else {
            return false;
        }
    }

    private boolean handlePointerDown(MotionEvent ev) {
        if(this.mIsBeingDragged) {
            return false;
        } else {
            this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
            this.mGestureInitialX = ev.getX();
            this.mGestureInitialY = ev.getY();
            this.mGestureInitialScrollY = this.getScrollY();
            this.mPointerLastX = this.mGestureInitialX;
            this.mPointerLastY = this.mGestureInitialY;
            this.mIsAbleToDrag = true;
            this.mVelocityTracker = VelocityTracker.obtain();
            this.mVelocityTracker.addMovement(ev);
            this.mScroller.computeScrollOffset();
            if((this.mScrollState != 2 && this.mScrollState != 3 || this.mScrollAxis != 0 || Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX()) <= this.mCloseEnough) && (this.mScrollAxis != 1 || Math.abs(this.mScroller.getFinalY() - this.mScroller.getCurrY()) <= this.mCloseEnough)) {
                this.completeScroll(false);
                this.mIsBeingDragged = false;
            } else {
                this.mScroller.abortAnimation();
                this.mDelayPopulate = false;
                this.populate();
                this.mIsBeingDragged = true;
                this.requestParentDisallowInterceptTouchEvent(true);
                this.setScrollState(1);
            }

            return false;
        }
    }

    private boolean handlePointerMove(MotionEvent ev) {
        int activePointerId = this.mActivePointerId;
        if(activePointerId == -1) {
            return false;
        } else {
            int pointerIndex = ev.findPointerIndex(activePointerId);
            if(pointerIndex == -1) {
                return this.mIsBeingDragged;
            } else {
                float x = MotionEventCompat.getX(ev, pointerIndex);
                float y = MotionEventCompat.getY(ev, pointerIndex);
                float dx = x - this.mPointerLastX;
                float xDiff = Math.abs(dx);
                float dy = y - this.mPointerLastY;
                float yDiff = Math.abs(dy);
                if(this.mIsBeingDragged) {
                    ;
                }

                float dragX;
                float dragY;
                if(!this.mIsBeingDragged && xDiff * xDiff + yDiff * yDiff > (float)this.mTouchSlopSquared) {
                    this.mIsBeingDragged = true;
                    this.requestParentDisallowInterceptTouchEvent(true);
                    this.setScrollState(1);
                    if(yDiff >= xDiff) {
                        this.mScrollAxis = 1;
                    } else {
                        this.mScrollAxis = 0;
                    }

                    if(yDiff > 0.0F && xDiff > 0.0F) {
                        double h = Math.sqrt((double)(xDiff * xDiff + yDiff * yDiff));
                        double t = Math.acos((double)xDiff / h);
                        dragX = (float)(Math.sin(t) * (double)this.mTouchSlop);
                        dragY = (float)(Math.cos(t) * (double)this.mTouchSlop);
                    } else if(yDiff == 0.0F) {
                        dragY = (float)this.mTouchSlop;
                        dragX = 0.0F;
                    } else {
                        dragY = 0.0F;
                        dragX = (float)this.mTouchSlop;
                    }

                    this.mPointerLastX = dx > 0.0F?this.mPointerLastX + dragY:this.mPointerLastX - dragY;
                    this.mPointerLastY = dy > 0.0F?this.mPointerLastY + dragX:this.mPointerLastY - dragX;
                }

                if(this.mIsBeingDragged) {
                    dragX = this.mScrollAxis == 0?x:this.mPointerLastX;
                    dragY = this.mScrollAxis == 1?y:this.mPointerLastY;
                    if(this.performDrag(dragX, dragY)) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }

                this.mVelocityTracker.addMovement(ev);
                return this.mIsBeingDragged;
            }
        }
    }

    private boolean handlePointerUp(MotionEvent ev) {
        if(this.mIsBeingDragged && this.mExpectedRowCount != 0) {
            VelocityTracker velocityTracker = this.mVelocityTracker;
            velocityTracker.addMovement(ev);
            velocityTracker.computeCurrentVelocity(1000);
            int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
            int targetPageX = this.mCurItem.x;
            int targetPageY = this.mCurItem.y;
            int velocity = 0;
            GridViewPager.ItemInfo ii = this.infoForCurrentScrollPosition();
            switch(this.mScrollAxis) {
                case 0:
                    float x = ev.getRawX();
                    int totalDeltaX = (int)(x - this.mGestureInitialX);
                    velocity = (int)velocityTracker.getXVelocity(this.mActivePointerId);
                    int currentPageX = ii.positionX;
                    int distanceX = this.getRowScrollX(ii.positionY) - this.computePageLeft(ii.positionX);
                    float pageOffsetX = this.getXIndex((float)distanceX);
                    targetPageX = this.determineTargetPage(this.mCurItem.x, currentPageX, pageOffsetX, this.mPopulatedPages.left, this.mPopulatedPages.right, velocity, totalDeltaX);
                    break;
                case 1:
                    ev.getX(activePointerIndex);
                    int totalDeltaY = this.mGestureInitialScrollY - this.getScrollY();
                    velocity = (int)velocityTracker.getYVelocity(this.mActivePointerId);
                    int currentPageY = ii.positionY;
                    int distanceY = this.getScrollY() - this.computePageTop(ii.positionY);
                    float pageOffsetY = this.getYIndex((float)distanceY);
                    if(pageOffsetY == 0.0F) {
                        View child = this.getChildForInfo(this.infoForCurrentScrollPosition());
                        int scrollable = this.getScrollableDistance(child, -velocity);
                        if(scrollable != 0) {
                            this.mScrollingContent = child;
                            if(Math.abs(velocity) >= Math.abs(this.mMinFlingVelocity)) {
                                this.flingContent(0, scrollable, 0, -velocity);
                                this.endDrag();
                            }
                        }
                    } else {
                        targetPageY = this.determineTargetPage(this.mCurItem.y, currentPageY, pageOffsetY, this.mPopulatedPages.top, this.mPopulatedPages.bottom, velocity, totalDeltaY);
                    }
            }

            if(this.mScrollState != 3) {
                this.mDelayPopulate = true;
                if(targetPageY != this.mCurItem.y) {
                    targetPageX = this.mAdapter.getCurrentColumnForRow(targetPageY, this.mCurItem.x);
                }

                this.setCurrentItemInternal(targetPageY, targetPageX, true, true, velocity);
            }

            this.mActivePointerId = -1;
            this.endDrag();
            return false;
        } else {
            this.mActivePointerId = -1;
            this.endDrag();
            return false;
        }
    }

    private float getXIndex(float distanceX) {
        int width = this.getContentWidth() + this.mColMargin;
        if(width == 0) {
            Log.e("GridViewPager", "getXIndex() called with zero width.");
            return 0.0F;
        } else {
            return distanceX / (float)width;
        }
    }

    private float getYIndex(float distanceY) {
        int height = this.getContentHeight() + this.mRowMargin;
        if(height == 0) {
            Log.e("GridViewPager", "getYIndex() called with zero height.");
            return 0.0F;
        } else {
            return distanceY / (float)height;
        }
    }

    private int determineTargetPage(int previousPage, int currentPage, float pageOffset, int firstPage, int lastPage, int velocity, int totalDragDistance) {
        if(Math.abs(velocity) < this.mMinUsableVelocity) {
            velocity = (int)Math.copySign((float)velocity, (float)totalDragDistance);
        }

        float flingBoost = 0.5F / Math.max(Math.abs(0.5F - pageOffset), 0.001F) * 100.0F;
        int targetPage;
        if(Math.abs(totalDragDistance) > this.mMinFlingDistance && (float)Math.abs(velocity) + flingBoost > (float)this.mMinFlingVelocity) {
            targetPage = velocity > 0?currentPage:currentPage + 1;
        } else {
            targetPage = Math.round((float)currentPage + pageOffset);
        }

        targetPage = limit(targetPage, firstPage, lastPage);
        return targetPage;
    }

    private static int limit(int val, int min, int max) {
        return val < min?min:(val > max?max:val);
    }

    private static float limit(float val, float min, float max) {
        return val < min?min:(val > max?max:val);
    }

    protected android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new GridViewPager.LayoutParams();
    }

    protected android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return this.generateDefaultLayoutParams();
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof GridViewPager.LayoutParams && super.checkLayoutParams(p);
    }

    public android.view.ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new GridViewPager.LayoutParams(this.getContext(), attrs);
    }

    public void debug() {
        this.debug(0);
    }

    protected void debug(int depth) {
        super.debug(depth);
        String output = debugIndent(depth);
        output = output + "mCurItem={" + this.mCurItem + "}";
        Log.d("View", output);
        output = debugIndent(depth);
        output = output + "mAdapter={" + this.mAdapter + "}";
        Log.d("View", output);
        output = debugIndent(depth);
        output = output + "mRowCount=" + this.mExpectedRowCount;
        Log.d("View", output);
        output = debugIndent(depth);
        output = output + "mCurrentColumnCount=" + this.mExpectedCurrentColumnCount;
        Log.d("View", output);
        int count = this.mItems.size();
        if(count != 0) {
            output = debugIndent(depth);
            output = output + "mItems={";
            Log.d("View", output);
        }

        for(int i = 0; i < count; ++i) {
            output = debugIndent(depth + 1);
            output = output + this.mItems.keyAt(i) + " => " + this.mItems.valueAt(i);
            Log.d("View", output);
        }

        if(count != 0) {
            output = debugIndent(depth);
            output = output + "}";
            Log.d("View", output);
        }

    }

    private static String debugIndent(int depth) {
        StringBuilder spaces = new StringBuilder((depth * 2 + 3) * 2);

        for(int i = 0; i < depth * 2 + 3; ++i) {
            spaces.append(' ').append(' ');
        }

        return spaces.toString();
    }

    private static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private boolean pointInRange(Point p) {
        return this.pointInRange(p.x, p.y);
    }

    private boolean pointInRange(int x, int y) {
        return inRange(y, 0, this.mExpectedRowCount - 1) && inRange(x, 0, this.mAdapter.getColumnCount(y) - 1);
    }

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity;
        public boolean needsMeasure;

        public LayoutParams() {
            super(-1, -1);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, GridViewPager.LAYOUT_ATTRS);
            this.gravity = a.getInteger(0, 48);
            a.recycle();
        }
    }

    private class PagerObserver extends DataSetObserver {
        private PagerObserver() {
        }

        public void onChanged() {
            GridViewPager.this.dataSetChanged();
        }

        public void onInvalidated() {
            GridViewPager.this.dataSetChanged();
        }
    }

    private static final class DragFrictionInterpolator implements Interpolator {
        private static final float DEFAULT_FALLOFF = 4.0F;
        private final float falloffRate;

        public DragFrictionInterpolator() {
            this(4.0F);
        }

        public DragFrictionInterpolator(float falloffRate) {
            this.falloffRate = falloffRate;
        }

        public float getInterpolation(float input) {
            double e = Math.exp((double)(2.0F * input * this.falloffRate));
            return (float)((e - 1.0D) / (e + 1.0D)) * (1.0F / this.falloffRate);
        }
    }

    private static class SavedState extends BaseSavedState {
        int currentX;
        int currentY;
        public static final Creator<GridViewPager.SavedState> CREATOR = new Creator() {
            public GridViewPager.SavedState createFromParcel(Parcel in) {
                return new GridViewPager.SavedState(in);
            }

            public GridViewPager.SavedState[] newArray(int size) {
                return new GridViewPager.SavedState[size];
            }
        };

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.currentX);
            out.writeInt(this.currentY);
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentX = in.readInt();
            this.currentY = in.readInt();
        }
    }

    public interface OnAdapterChangeListener {
        void onAdapterChanged(GridPagerAdapter var1, GridPagerAdapter var2);

        void onDataSetChanged();
    }

    public interface OnPageChangeListener {
        void onPageScrolled(int var1, int var2, float var3, float var4, int var5, int var6);

        void onPageSelected(int var1, int var2);

        void onPageScrollStateChanged(int var1);
    }

    static class ItemInfo {
        Object object;
        int positionX;
        int positionY;

        ItemInfo() {
        }

        public String toString() {
            return this.positionX + "," + this.positionY + " => " + this.object;
        }
    }
}
