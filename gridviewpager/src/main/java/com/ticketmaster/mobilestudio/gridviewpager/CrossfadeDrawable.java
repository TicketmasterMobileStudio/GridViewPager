package com.ticketmaster.mobilestudio.gridviewpager;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;

public class CrossfadeDrawable extends Drawable implements Callback {
    private Drawable mFading;
    private Drawable mBase;
    private float mProgress;
    private int mAlpha;
    private int mChangingConfigs;
    private ColorFilter mColorFilter;
    private boolean mFilterBitmap;
    private boolean mDither;
    private int mColorFilterColor;
    private Mode mColorFilterMode;

    public CrossfadeDrawable() {
    }

    public void setFading(Drawable d) {
        if(this.mFading != d) {
            if(this.mFading != null) {
                this.mFading.setCallback((Callback)null);
            }

            this.mFading = d;
            if(d != null) {
                this.initDrawable(d);
            }

            this.invalidateSelf();
        }

    }

    public void setBase(Drawable d) {
        if(this.mBase != d) {
            if(this.mBase != null) {
                this.mBase.setCallback((Callback)null);
            }

            this.mBase = d;
            this.initDrawable(d);
            this.invalidateSelf();
        }

    }

    public void setProgress(float progress) {
        float updated = Func.clamp(progress, 0, 1);
        if(updated != this.mProgress) {
            this.mProgress = updated;
            this.invalidateSelf();
        }

    }

    private void initDrawable(Drawable d) {
        d.setCallback(this);
        d.setState(this.getState());
        if(this.mColorFilter != null) {
            d.setColorFilter(this.mColorFilter);
        }

        if(this.mColorFilterMode != null) {
            d.setColorFilter(this.mColorFilterColor, this.mColorFilterMode);
        }

        d.setDither(this.mDither);
        d.setFilterBitmap(this.mFilterBitmap);
        d.setBounds(this.getBounds());
    }

    public void draw(Canvas canvas) {
        if(this.mBase != null && (this.mProgress < 1.0F || this.mFading == null)) {
            this.mBase.setAlpha(255);
            this.mBase.draw(canvas);
        }

        if(this.mFading != null && this.mProgress > 0.0F) {
            this.mFading.setAlpha((int)(255.0F * this.mProgress));
            this.mFading.draw(canvas);
        }

    }

    public int getIntrinsicWidth() {
        int fading = this.mFading == null?-1:this.mFading.getIntrinsicWidth();
        int base = this.mBase == null?-1:this.mBase.getIntrinsicHeight();
        return Math.max(fading, base);
    }

    public int getIntrinsicHeight() {
        int fading = this.mFading == null?-1:this.mFading.getIntrinsicHeight();
        int base = this.mBase == null?-1:this.mBase.getIntrinsicHeight();
        return Math.max(fading, base);
    }

    protected void onBoundsChange(Rect bounds) {
        if(this.mBase != null) {
            this.mBase.setBounds(bounds);
        }

        if(this.mFading != null) {
            this.mFading.setBounds(bounds);
        }

        this.invalidateSelf();
    }

    public void jumpToCurrentState() {
        if(this.mFading != null) {
            this.mFading.jumpToCurrentState();
        }

        if(this.mBase != null) {
            this.mBase.jumpToCurrentState();
        }

    }

    public void setChangingConfigurations(int configs) {
        if(this.mChangingConfigs != configs) {
            this.mChangingConfigs = configs;
            if(this.mFading != null) {
                this.mFading.setChangingConfigurations(configs);
            }

            if(this.mBase != null) {
                this.mBase.setChangingConfigurations(configs);
            }
        }

    }

    public void setFilterBitmap(boolean filter) {
        if(this.mFilterBitmap != filter) {
            this.mFilterBitmap = filter;
            if(this.mFading != null) {
                this.mFading.setFilterBitmap(filter);
            }

            if(this.mBase != null) {
                this.mBase.setFilterBitmap(filter);
            }
        }

    }

    public void setDither(boolean dither) {
        if(this.mDither != dither) {
            this.mDither = dither;
            if(this.mFading != null) {
                this.mFading.setDither(dither);
            }

            if(this.mBase != null) {
                this.mBase.setDither(dither);
            }
        }

    }

    public void setColorFilter(ColorFilter cf) {
        if(this.mColorFilter != cf) {
            this.mColorFilter = cf;
            if(this.mFading != null) {
                this.mFading.setColorFilter(cf);
            }

            if(this.mBase != null) {
                this.mBase.setColorFilter(cf);
            }
        }

    }

    public void setColorFilter(int color, Mode mode) {
        if(this.mColorFilterColor != color || this.mColorFilterMode != mode) {
            this.mColorFilterColor = color;
            this.mColorFilterMode = mode;
            if(this.mFading != null) {
                this.mFading.setColorFilter(color, mode);
            }

            if(this.mBase != null) {
                this.mBase.setColorFilter(color, mode);
            }
        }

    }

    public void clearColorFilter() {
        if(this.mColorFilterMode != null) {
            this.mColorFilterMode = null;
            if(this.mFading != null) {
                this.mFading.clearColorFilter();
            }

            if(this.mBase != null) {
                this.mBase.clearColorFilter();
            }
        }

    }

    public int getChangingConfigurations() {
        return this.mChangingConfigs;
    }

    protected boolean onStateChange(int[] state) {
        boolean changed = false;
        if(this.mFading != null) {
            changed |= this.mFading.setState(state);
        }

        if(this.mBase != null) {
            changed |= this.mBase.setState(state);
        }

        return changed;
    }

    protected boolean onLevelChange(int level) {
        boolean changed = false;
        if(this.mFading != null) {
            changed |= this.mFading.setLevel(level);
        }

        if(this.mBase != null) {
            changed |= this.mBase.setLevel(level);
        }

        return changed;
    }

    public boolean isStateful() {
        return this.mFading != null && this.mFading.isStateful() || this.mBase != null && this.mBase.isStateful();
    }

    public int getAlpha() {
        return this.mAlpha;
    }

    public void setAlpha(int alpha) {
        if(alpha != this.mAlpha) {
            this.mAlpha = alpha;
            this.invalidateSelf();
        }

    }

    public Drawable getBase() {
        return this.mBase;
    }

    public Drawable getFading() {
        return this.mFading;
    }

    public int getOpacity() {
        return resolveOpacity(this.mFading == null?0:this.mFading.getOpacity(), this.mBase == null?0:this.mBase.getOpacity());
    }

    public void invalidateDrawable(Drawable who) {
        if((who == this.mFading || who == this.mBase) && this.getCallback() != null) {
            this.getCallback().invalidateDrawable(this);
        }

    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if((who == this.mFading || who == this.mBase) && this.getCallback() != null) {
            this.getCallback().scheduleDrawable(this, what, when);
        }

    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        if((who == this.mFading || who == this.mBase) && this.getCallback() != null) {
            this.getCallback().unscheduleDrawable(this, what);
        }

    }
}
