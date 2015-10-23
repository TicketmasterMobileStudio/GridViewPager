package com.ticketmaster.mobilestudio.gridviewpager;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;

class ViewportDrawable extends Drawable implements Callback {
    private static final String TAG = "ViewportDrawable";
    private static final float STEP_SIZE_PCT = 0.2F;
    private static final boolean DEBUG = false;
    private Drawable mDrawable;
    private int mAlpha;
    private boolean mDither;
    private int mColorFilterColor;
    private Mode mColorFilterMode;
    private ColorFilter mColorFilter;
    private boolean mFilterBitmap;
    private int mChangingConfigs;
    private int mMaxPosX;
    private int mMaxPosY;
    private float mPositionX;
    private float mPositionY;
    private int mCenterOffsetX;
    private int mCenterOffsetY;
    private final Rect mDrawableBounds;
    private float mScale;
    private int mSrcWidth;
    private int mSrcHeight;
    private float mWidthStepSize;
    private float mHeightStepSize;

    public ViewportDrawable() {
        this((Drawable)null);
    }

    public ViewportDrawable(Drawable drawable) {
        this.mAlpha = 255;
        this.mDither = true;
        this.mFilterBitmap = true;
        this.mMaxPosX = 2;
        this.mMaxPosY = 2;
        this.mPositionX = 1.0F;
        this.mPositionY = 1.0F;
        this.mDrawableBounds = new Rect();
        this.mScale = 1.0F;
        this.setDrawable(drawable);
    }

    public void setDrawable(Drawable drawable) {
        if(this.mDrawable != drawable) {
            if(this.mDrawable != null) {
                this.mDrawable.setCallback((Callback)null);
            }

            this.mDrawable = drawable;
            if(this.mDrawable != null) {
                this.mDrawable.setAlpha(this.getAlpha());
                this.mDrawable.setBounds(this.getBounds());
                int w = this.mDrawable.getIntrinsicWidth();
                int h = this.mDrawable.getIntrinsicHeight();
                Rect bounds = this.getBounds();
                if(w != -1 && w != 1) {
                    this.mDrawable.setBounds(bounds.left, bounds.top, bounds.left + w, bounds.top + h);
                } else {
                    this.mDrawable.setBounds(bounds);
                }

                this.mDrawable.setCallback(this);
                if(this.mColorFilter != null) {
                    this.mDrawable.setColorFilter(this.mColorFilter);
                }

                if(this.mColorFilterMode != null) {
                    this.mDrawable.setColorFilter(this.mColorFilterColor, this.mColorFilterMode);
                }

                this.mDrawable.setDither(this.mDither);
                this.mDrawable.setFilterBitmap(this.mFilterBitmap);
                this.mDrawable.setState(this.getState());
                this.recomputeScale();
                this.invalidateSelf();
            }

        }
    }

    public void setPosition(float x, float y) {
        if(this.mPositionX != x || this.mPositionY != y) {
            this.mPositionX = limit(x, 0, this.mMaxPosX);
            this.mPositionY = limit(y, 0, this.mMaxPosY);
            this.invalidateSelf();
        }

    }

    public void setVerticalPosition(float y) {
        this.setPosition(this.mPositionX, y);
    }

    public void setHorizontalPosition(float x) {
        this.setPosition(x, this.mPositionY);
    }

    public void setStops(int xStops, int yStops) {
        int maxX = Math.max(0, xStops - 1);
        int maxY = Math.max(0, yStops - 1);
        if(maxX != this.mMaxPosX || maxY != this.mMaxPosY) {
            this.mMaxPosX = maxX;
            this.mMaxPosY = maxY;
            this.mPositionX = limit(this.mPositionX, 0, this.mMaxPosX);
            this.mPositionY = limit(this.mPositionY, 0, this.mMaxPosY);
            this.recomputeScale();
            this.invalidateSelf();
        }

    }

    public void setHorizontalStops(int stops) {
        this.setStops(stops, this.mMaxPosY + 1);
    }

    public void setVerticalStops(int stops) {
        this.setStops(this.mMaxPosX + 1, stops);
    }

    protected void onBoundsChange(Rect bounds) {
        this.mDrawableBounds.set(bounds);
        this.recomputeScale();
        this.invalidateSelf();
    }

    private void recomputeScale() {
        if(this.mDrawable != null) {
            if(this.mDrawableBounds.width() != 0 && this.mDrawableBounds.height() != 0) {
                this.mSrcWidth = this.mDrawable.getIntrinsicWidth();
                this.mSrcHeight = this.mDrawable.getIntrinsicHeight();
                if(this.mSrcWidth != -1 && this.mSrcHeight != -1) {
                    this.mWidthStepSize = 0.2F * (float)this.mDrawableBounds.width();
                    this.mHeightStepSize = 0.2F * (float)this.mDrawableBounds.height();
                    float minWidth = (float)this.mDrawableBounds.width() + (float)this.mMaxPosX * this.mWidthStepSize;
                    float minHeight = (float)this.mDrawableBounds.height() + (float)this.mMaxPosY * this.mHeightStepSize;
                    this.mScale = Math.max(minWidth / (float)this.mSrcWidth, minHeight / (float)this.mSrcHeight);
                    float scaledWidth = (float)this.mSrcWidth * this.mScale;
                    float scaledHeight = (float)this.mSrcHeight * this.mScale;
                    if(scaledWidth > minWidth) {
                        this.mCenterOffsetX = (int)((scaledWidth - minWidth) / 2.0F);
                        this.mCenterOffsetY = 0;
                    } else {
                        this.mCenterOffsetY = (int)((scaledHeight - minHeight) / 2.0F);
                        this.mCenterOffsetX = 0;
                    }

                } else {
                    this.mSrcWidth = this.mDrawableBounds.width();
                    this.mSrcHeight = this.mDrawableBounds.height();
                    this.mScale = 1.0F;
                    this.mWidthStepSize = 0.0F;
                    this.mHeightStepSize = 0.0F;
                    this.mCenterOffsetX = 0;
                    this.mCenterOffsetY = 0;
                }
            }
        }
    }

    public void draw(Canvas canvas) {
        if(this.mDrawable != null) {
            canvas.save();
            canvas.clipRect(this.getBounds());
            float tx = (float)this.mCenterOffsetX + this.mPositionX * this.mWidthStepSize;
            float ty = (float)this.mCenterOffsetY + this.mPositionY * this.mHeightStepSize;
            canvas.translate(-tx, -ty);
            canvas.scale(this.mScale, this.mScale);
            this.mDrawable.draw(canvas);
            canvas.restore();
        }

    }

    private static float limit(float value, int min, int max) {
        return value < (float)min?(float)min:(value > (float)max?(float)max:value);
    }

    public void setFilterBitmap(boolean filter) {
        if(this.mFilterBitmap != filter) {
            this.mFilterBitmap = filter;
            if(this.mDrawable != null) {
                this.mDrawable.setFilterBitmap(filter);
            }
        }

    }

    public void setDither(boolean dither) {
        if(this.mDither != dither) {
            this.mDither = dither;
            if(this.mDrawable != null) {
                this.mDrawable.setDither(dither);
            }
        }

    }

    public void setColorFilter(int color, Mode mode) {
        if(this.mColorFilterColor != color || this.mColorFilterMode != mode) {
            this.mColorFilterColor = color;
            this.mColorFilterMode = mode;
            if(this.mDrawable != null) {
                this.mDrawable.setColorFilter(color, mode);
            }
        }

    }

    public void clearColorFilter() {
        if(this.mColorFilterMode != null) {
            this.mColorFilterMode = null;
            if(this.mDrawable != null) {
                this.mDrawable.clearColorFilter();
            }
        }

    }

    public void jumpToCurrentState() {
        if(this.mDrawable != null) {
            this.mDrawable.jumpToCurrentState();
        }

    }

    public void setChangingConfigurations(int configs) {
        if(this.mChangingConfigs != configs) {
            this.mChangingConfigs = configs;
            if(this.mDrawable != null) {
                this.mDrawable.setChangingConfigurations(configs);
            }
        }

    }

    public int getChangingConfigurations() {
        return this.mChangingConfigs;
    }

    protected boolean onStateChange(int[] state) {
        return this.mDrawable != null?this.mDrawable.setState(state):false;
    }

    protected boolean onLevelChange(int level) {
        return this.mDrawable != null?this.mDrawable.setLevel(level):false;
    }

    public boolean isStateful() {
        return this.mDrawable != null?this.mDrawable.isStateful():false;
    }

    public int getAlpha() {
        return this.mAlpha;
    }

    public void setAlpha(int alpha) {
        if(this.mAlpha != alpha) {
            this.mAlpha = alpha;
            if(this.mDrawable != null) {
                this.mDrawable.setAlpha(alpha);
            }
        }

    }

    public void setColorFilter(ColorFilter cf) {
        if(this.mColorFilter != cf) {
            this.mColorFilter = cf;
            if(this.mDrawable != null) {
                this.mDrawable.setColorFilter(cf);
            }
        }

    }

    public int getOpacity() {
        return this.mDrawable != null?this.mDrawable.getOpacity():0;
    }

    public void invalidateDrawable(Drawable who) {
        if(who == this.mDrawable && this.getCallback() != null) {
            this.getCallback().invalidateDrawable(this);
        }

    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if(who == this.mDrawable && this.getCallback() != null) {
            this.getCallback().scheduleDrawable(this, what, when);
        }

    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        if(who == this.mDrawable && this.getCallback() != null) {
            this.getCallback().unscheduleDrawable(this, what);
        }

    }
}
