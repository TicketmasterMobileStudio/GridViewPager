package com.ticketmaster.mobilestudio.gridviewpager;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

public abstract class GridPagerAdapter {
    public static final int DEFAULT_OFFSET = 1;

    public static final Drawable BACKGROUND_NONE = new NoOpDrawable();
    public static final int OPTION_DISABLE_PARALLAX = 1;
    public static final int PAGE_DEFAULT_OPTIONS = 0;
    public static final Point POSITION_NONE = new Point(-1, -1);
    public static final Point POSITION_UNCHANGED = new Point(-2, -2);
    private DataSetObservable mObservable = new DataSetObservable();
    private OnBackgroundChangeListener mOnBackgroundChangeListener;

    public GridPagerAdapter() {
    }

    public abstract int getRowCount();

    public abstract int getColumnCount(int row);

    /**
     * Default is GridPagerAdapter.DEFAULT_OFFSET.
     * This can also be thought of as the number of horizontal pages in a row given a position.
     * Note, if this returns 0 swiping left and right will be disabled.
     */
    public int getColumnOffscreenPageCount(int row, int column) {
        return DEFAULT_OFFSET;
    }

    /**
     * Default is GridPagerAdapter.DEFAULT_OFFSET.
     * This can also be thought of as the number of vertical pages in a column given a position.
     * Note, if this returns 0 swiping up and down will be disabled.
     */
    public int getRowOffscreenPageCount(int row, int column) {
        return DEFAULT_OFFSET;
    }

    /**
     * True by default. Can prevent swiping left at an exact position.
     */
    public boolean isLeftSwipingAllowed(int row, int column) {
        return true;
    }

    /**
     * True by default. Can prevent swiping up at an exact position.
     */
    public boolean isUpSwipingAllowed(int row, int column) {
        return true;
    }

    /**
     * True by default. Can prevent swiping right at an exact position.
     */
    public boolean isRightSwipingAllowed(int row, int column) {
        return true;
    }

    /**
     * True by default. Can prevent swiping down at an exact position.
     */
    public boolean isDownSwipingAllowed(int row, int column) {
        return true;
    }

    public int getCurrentColumnForRow(int row, int currentColumn) {
        return 0;
    }

    public void setCurrentColumnForRow(int row, int currentColumn) {
    }

    public void startUpdate(ViewGroup container) {
    }

    public abstract Object instantiateItem(ViewGroup var1, int var2, int var3);

    public abstract void destroyItem(ViewGroup var1, int var2, int var3, Object var4);

    public void finishUpdate(ViewGroup container) {
    }

    public abstract boolean isViewFromObject(View var1, Object var2);

    public Drawable getBackgroundForRow(int row) {
        return BACKGROUND_NONE;
    }

    public Drawable getBackgroundForPage(int row, int column) {
        return BACKGROUND_NONE;
    }

    public int getOptionsForPage(int row, int column) {
        return 0;
    }

    public void notifyPageBackgroundChanged(int row, int column) {
        if(this.mOnBackgroundChangeListener != null) {
            this.mOnBackgroundChangeListener.onPageBackgroundChanged(row, column);
        }

    }

    public void notifyRowBackgroundChanged(int row) {
        if(this.mOnBackgroundChangeListener != null) {
            this.mOnBackgroundChangeListener.onRowBackgroundChanged(row);
        }

    }

    void setOnBackgroundChangeListener(OnBackgroundChangeListener listener) {
        this.mOnBackgroundChangeListener = listener;
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        this.mObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        this.mObservable.unregisterObserver(observer);
    }

    public void notifyDataSetChanged() {
        this.mObservable.notifyChanged();
    }

    public Point getItemPosition(Object object) {
        return POSITION_NONE;
    }

    protected void applyItemPosition(Object object, Point position) {
    }

    public Parcelable saveState() {
        return null;
    }

    public void restoreState(Parcelable savedState, ClassLoader classLoader) {
    }

    private static final class NoOpDrawable extends Drawable {
        private NoOpDrawable() {
        }

        public void draw(Canvas canvas) {
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter cf) {
        }

        public int getOpacity() {
            return 0;
        }
    }

    public interface OnBackgroundChangeListener {
        void onPageBackgroundChanged(int var1, int var2);

        void onRowBackgroundChanged(int var1);
    }
}

