package com.ticketmaster.mobilestudio.gridviewpager;


import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.view.View;

import com.ticketmaster.mobilestudio.gridviewpager.GridPagerAdapter.OnBackgroundChangeListener;
import com.ticketmaster.mobilestudio.gridviewpager.GridViewPager.OnAdapterChangeListener;
import com.ticketmaster.mobilestudio.gridviewpager.GridViewPager.OnPageChangeListener;

class BackgroundController implements OnPageChangeListener, OnAdapterChangeListener, OnBackgroundChangeListener {
    private GridPagerAdapter mAdapter;
    private Direction mDirection;
    private final Point mCurrentPage;
    private final Point mLastSelectedPage;
    private final LruCache<Integer, Drawable> mRowBackgrounds;
    private final LruCache<Integer, Drawable> mPageBackgrounds;
    private final ViewportDrawable mBaseLayer;
    private final ViewportDrawable mCrossfadeLayer;
    private final CrossfadeDrawable mBackground;
    private final Point mLastPageScrolled;
    private final Point mFadeSourcePage;
    private final Point mBaseSourcePage;
    private float mScrollRelativeY;
    private float mScrollRelativeX;
    private float mCrossfadeXPos;
    private float mCrossfadeYPos;
    private int mFadeXSteps;
    private int mFadeYSteps;
    private float mBaseXPos;
    private float mBaseYPos;
    private int mBaseXSteps;
    private int mBaseYSteps;
    private boolean mUsingCrossfadeLayer;

    private static int pack(int x, int y) {
        return y << 16 | x & '\uffff';
    }

    private static int pack(Point p) {
        return pack(p.x, p.y);
    }

    private static int unpackX(int key) {
        return key & '\uffff';
    }

    private static int unpackY(int key) {
        return key >>> 16;
    }

    public BackgroundController() {
        this.mDirection = Direction.NONE;
        this.mCurrentPage = new Point();
        this.mLastSelectedPage = new Point();
        this.mRowBackgrounds = new LruCache(3) {
            protected Drawable create(Integer key) {
                return BackgroundController.this.mAdapter.getBackgroundForRow(key.intValue()).mutate();
            }
        };
        this.mPageBackgrounds = new LruCache(5) {
            protected Drawable create(Integer key) {
                int col = BackgroundController.unpackX(key.intValue());
                int row = BackgroundController.unpackY(key.intValue());
                return BackgroundController.this.mAdapter.getBackgroundForPage(row, col).mutate();
            }
        };
        this.mBaseLayer = new ViewportDrawable();
        this.mCrossfadeLayer = new ViewportDrawable();
        this.mBackground = new CrossfadeDrawable();
        this.mLastPageScrolled = new Point();
        this.mFadeSourcePage = new Point();
        this.mBaseSourcePage = new Point();
        this.mBackground.setFilterBitmap(true);
        this.mCrossfadeLayer.setFilterBitmap(true);
        this.mBaseLayer.setFilterBitmap(true);
    }

    public Drawable getBackground() {
        return this.mBackground;
    }

    public void attachTo(View v) {
        v.setBackground(this.mBackground);
    }

    public void onPageScrollStateChanged(int state) {
        if(state == GridViewPager.SCROLL_STATE_IDLE) {
            this.mDirection = Direction.NONE;
        }

    }

    public void onPageScrolled(int row, int column, float rowOffset, float colOffset, int rowOffsetPx, int colOffsetPx) {
        float relX;
        float relY;
        if(this.mDirection != Direction.NONE && this.mCurrentPage.equals(this.mLastSelectedPage) && this.mLastPageScrolled.equals(column, row)) {
            if(this.mDirection.isVertical()) {
                relX = 0.0F;
                relY = (float)Func.clamp(row - this.mCurrentPage.y, -1, 0) + rowOffset;
            } else {
                relX = (float)Func.clamp(column - this.mCurrentPage.x, -1, 0) + colOffset;
                relY = 0.0F;
            }
        } else {
            this.mLastPageScrolled.set(column, row);
            this.mCurrentPage.set(this.mLastSelectedPage.x, this.mLastSelectedPage.y);
            relX = 0.0F;
            relY = (float)Func.clamp(row - this.mCurrentPage.y, -1, 0) + rowOffset;
            if(relY == 0.0F) {
                relX = (float)Func.clamp(column - this.mCurrentPage.x, -1, 0) + colOffset;
            }

            this.mDirection = Direction.fromOffset(relX, relY);
            this.updateBackgrounds(this.mCurrentPage, this.mLastPageScrolled, this.mDirection, relX, relY);
        }

        this.mScrollRelativeX = relX;
        this.mScrollRelativeY = relY;
        this.mBaseLayer.setPosition(this.mBaseXPos + relX, this.mBaseYPos + relY);
        if(this.mUsingCrossfadeLayer) {
            float progress = this.mDirection.isVertical()?Math.abs(relY):Math.abs(relX);
            this.mBackground.setProgress(progress);
            this.mCrossfadeLayer.setPosition(this.mCrossfadeXPos + relX, this.mCrossfadeYPos + relY);
        }

    }

    private void updateBackgrounds(Point current, Point scrolling, Direction dir, float relX, float relY) {
        if(this.mAdapter != null && this.mAdapter.getRowCount() > 0) {
            Drawable base = this.updateBaseLayer(current, relX, relY);
            boolean overScrolling = (float)current.x + relX < 0.0F || (float)current.y + relY < 0.0F || (float)scrolling.x + relX > (float)(this.mAdapter.getColumnCount(current.y) - 1) || (float)scrolling.y + relY > (float)(this.mAdapter.getRowCount() - 1);
            if(this.mDirection != Direction.NONE && !overScrolling) {
                this.updateFadingLayer(current, scrolling, dir, relX, relY, base);
            } else {
                this.mUsingCrossfadeLayer = false;
                this.mCrossfadeLayer.setDrawable((Drawable)null);
                this.mBackground.setProgress(0.0F);
            }
        } else {
            this.mUsingCrossfadeLayer = false;
            this.mBaseLayer.setDrawable((Drawable)null);
            this.mCrossfadeLayer.setDrawable((Drawable)null);
        }

    }

    private Drawable updateBaseLayer(Point current, float relX, float relY) {
        Drawable base = (Drawable)this.mPageBackgrounds.get(Integer.valueOf(pack(current)));
        this.mBaseSourcePage.set(current.x, current.y);
        if(base == GridPagerAdapter.BACKGROUND_NONE) {
            base = (Drawable)this.mRowBackgrounds.get(Integer.valueOf(current.y));
            this.mBaseXSteps = this.mAdapter.getColumnCount(current.y) + 2;
            this.mBaseXPos = (float)(current.x + 1);
        } else {
            this.mBaseXSteps = 3;
            this.mBaseXPos = 1.0F;
        }

        this.mBaseYSteps = 3;
        this.mBaseYPos = 1.0F;
        this.mBaseLayer.setDrawable(base);
        this.mBaseLayer.setStops(this.mBaseXSteps, this.mBaseYSteps);
        this.mBaseLayer.setPosition(this.mBaseXPos + relX, this.mBaseYPos + relY);
        this.mBackground.setBase(this.mBaseLayer);
        return base;
    }

    private void updateFadingLayer(Point current, Point scrolling, Direction dir, float relX, float relY, Drawable base) {
        int crossfadeY = scrolling.y + (dir == Direction.DOWN?1:0);
        int crossfadeX = scrolling.x + (dir == Direction.RIGHT?1:0);
        if(crossfadeY != this.mCurrentPage.y) {
            crossfadeX = this.mAdapter.getCurrentColumnForRow(crossfadeY, current.x);
        }

        Drawable fade = (Drawable)this.mPageBackgrounds.get(Integer.valueOf(pack(crossfadeX, crossfadeY)));
        this.mFadeSourcePage.set(crossfadeX, crossfadeY);
        boolean fadeIsRowBg = false;
        if(fade == GridPagerAdapter.BACKGROUND_NONE) {
            fade = (Drawable)this.mRowBackgrounds.get(Integer.valueOf(crossfadeY));
            fadeIsRowBg = true;
        }

        if(base == fade) {
            this.mUsingCrossfadeLayer = false;
            this.mCrossfadeLayer.setDrawable((Drawable)null);
            this.mBackground.setFading((Drawable)null);
            this.mBackground.setProgress(0.0F);
        } else {
            if(fadeIsRowBg) {
                int physRow = Func.clamp(crossfadeY, 0, this.mAdapter.getRowCount() - 1);
                this.mFadeXSteps = this.mAdapter.getColumnCount(physRow) + 2;
                if(dir.isHorizontal()) {
                    this.mCrossfadeXPos = (float)(current.x + 1);
                } else {
                    this.mCrossfadeXPos = (float)(crossfadeX + 1);
                }
            } else {
                this.mFadeXSteps = 3;
                this.mCrossfadeXPos = (float)(1 - dir.x);
            }

            this.mFadeYSteps = 3;
            this.mCrossfadeYPos = (float)(1 - dir.y);
            this.mUsingCrossfadeLayer = true;
            this.mCrossfadeLayer.setDrawable(fade);
            this.mCrossfadeLayer.setStops(this.mFadeXSteps, this.mFadeYSteps);
            this.mCrossfadeLayer.setPosition(this.mCrossfadeXPos + relX, this.mCrossfadeYPos + relY);
            this.mBackground.setFading(this.mCrossfadeLayer);
        }

    }

    public void onPageSelected(int row, int column) {
        this.mLastSelectedPage.set(column, row);
    }

    public void onPageBackgroundChanged(int row, int column) {
        this.mPageBackgrounds.remove(Integer.valueOf(pack(column, row)));
        if(this.mAdapter != null && this.mAdapter.getRowCount() > 0) {
            this.updateBackgrounds(this.mCurrentPage, this.mCurrentPage, Direction.NONE, this.mScrollRelativeX, this.mScrollRelativeY);
        }

    }

    public void onRowBackgroundChanged(int row) {
        this.mRowBackgrounds.remove(Integer.valueOf(row));
        if(this.mAdapter != null && this.mAdapter.getRowCount() > 0) {
            this.updateBackgrounds(this.mCurrentPage, this.mCurrentPage, Direction.NONE, this.mScrollRelativeX, this.mScrollRelativeY);
        }

    }

    public void onAdapterChanged(GridPagerAdapter oldAdapter, GridPagerAdapter newAdapter) {
        this.reset();
        this.mLastSelectedPage.set(0, 0);
        this.mCurrentPage.set(0, 0);
        this.mAdapter = newAdapter;
    }

    public void onDataSetChanged() {
        this.reset();
    }

    private void reset() {
        this.mDirection = Direction.NONE;
        this.mPageBackgrounds.evictAll();
        this.mRowBackgrounds.evictAll();
        this.mCrossfadeLayer.setDrawable((Drawable)null);
        this.mBaseLayer.setDrawable((Drawable)null);
    }

    private static enum Direction {
        LEFT(-1, 0),
        UP(0, -1),
        RIGHT(1, 0),
        DOWN(0, 1),
        NONE(0, 0);

        private final int x;
        private final int y;

        private Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }

        boolean isVertical() {
            return this.y != 0;
        }

        boolean isHorizontal() {
            return this.x != 0;
        }

        static Direction fromOffset(float x, float y) {
            return y != 0.0F?(y > 0.0F?DOWN:UP):(x != 0.0F?(x > 0.0F?RIGHT:LEFT):NONE);
        }
    }
}
