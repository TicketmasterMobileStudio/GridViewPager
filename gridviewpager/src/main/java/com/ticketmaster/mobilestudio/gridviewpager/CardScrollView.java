package com.ticketmaster.mobilestudio.gridviewpager;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

@TargetApi(20)
public class CardScrollView extends FrameLayout {
    private static final String TAG = "CardScrollView";
    private static final boolean DEBUG = false;
    private static final int CARD_SHADOW_WIDTH_DP = 8;
    private CardFrame mCardFrame;
    private boolean mRoundDisplay;
    private final int mCardShadowWidth;

    public CardScrollView(Context context) {
        this(context, (AttributeSet)null);
    }

    public CardScrollView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        this.mCardShadowWidth = (int)(8.0F * this.getResources().getDisplayMetrics().density);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.requestApplyInsets();
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        boolean round = insets.isRound();
        if(this.mRoundDisplay != round) {
            this.mRoundDisplay = round;
            LayoutParams bottomInset = (LayoutParams)this.mCardFrame.getLayoutParams();
            bottomInset.leftMargin = -this.mCardShadowWidth;
            bottomInset.rightMargin = -this.mCardShadowWidth;
            bottomInset.bottomMargin = -this.mCardShadowWidth;
            this.mCardFrame.setLayoutParams(bottomInset);
        }

        if(insets.getSystemWindowInsetBottom() > 0) {
            int bottomInset1 = insets.getSystemWindowInsetBottom();
            android.view.ViewGroup.LayoutParams lp = this.getLayoutParams();
            if(lp instanceof MarginLayoutParams) {
                ((MarginLayoutParams)lp).bottomMargin = bottomInset1;
            }
        }

        if(this.mRoundDisplay && this.mCardFrame != null) {
            this.mCardFrame.onApplyWindowInsets(insets);
        }

        this.requestLayout();
        return insets;
    }

    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        if(this.getChildCount() <= 0 && child instanceof CardFrame) {
            super.addView(child, index, params);
            this.mCardFrame = (CardFrame)child;
        } else {
            throw new IllegalStateException("CardScrollView may contain only a single CardFrame.");
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        if(this.getChildCount() == 0 || !(this.getChildAt(0) instanceof CardFrame)) {
            Log.w("CardScrollView", "No CardFrame has been added!");
        }

    }

    private boolean hasCardFrame() {
        if(this.mCardFrame == null) {
            Log.w("CardScrollView", "No CardFrame has been added.");
            return false;
        } else {
            return true;
        }
    }

    public void setExpansionEnabled(boolean enableExpansion) {
        if(Log.isLoggable("CardScrollView", 3)) {
            Log.d("CardScrollView", "setExpansionEnabled: " + enableExpansion);
        }

        if(this.hasCardFrame()) {
            boolean wasEnabled = this.mCardFrame.isExpansionEnabled();
            if(enableExpansion != wasEnabled) {
                this.mCardFrame.setExpansionEnabled(enableExpansion);
                if(!enableExpansion) {
                    this.scrollTo(0, 0);
                }
            }
        }

    }

    public boolean isExpansionEnabled() {
        return this.hasCardFrame()?this.mCardFrame.isExpansionEnabled():false;
    }

    public void setExpansionDirection(int direction) {
        if(Log.isLoggable("CardScrollView", 3)) {
            Log.d("CardScrollView", "setExpansionDirection: " + direction);
        }

        if(this.hasCardFrame()) {
            int curDirection = this.mCardFrame.getExpansionDirection();
            if(direction != curDirection) {
                this.mCardFrame.setExpansionDirection(direction);
                if(direction == 1 && this.getScrollY() < 0) {
                    this.scrollTo(0, 0);
                } else if(direction == -1 && this.getScrollY() > 0) {
                    this.scrollTo(0, 0);
                }

                this.requestLayout();
            }
        }

    }

    public float getExpansionFactor() {
        return this.hasCardFrame()?this.mCardFrame.getExpansionFactor():0.0F;
    }

    public void setExpansionFactor(float expansionFactor) {
        if(Log.isLoggable("CardScrollView", 3)) {
            Log.d("CardScrollView", "setExpansionFactor: " + expansionFactor);
        }

        if(this.hasCardFrame()) {
            this.mCardFrame.setExpansionFactor(expansionFactor);
        }

    }

    public int getExpansionDirection() {
        return this.hasCardFrame()?this.mCardFrame.getExpansionDirection():0;
    }

    public void setCardGravity(int gravity) {
        if(Log.isLoggable("CardScrollView", 3)) {
            Log.d("CardScrollView", "setCardGravity: " + gravity);
        }

        if(this.hasCardFrame()) {
            gravity &= 112;
            LayoutParams existing = (LayoutParams)this.mCardFrame.getLayoutParams();
            if(existing.gravity != gravity) {
                this.mCardFrame.setLayoutParams(new LayoutParams(-1, -2, gravity));
                this.requestLayout();
            }
        }

    }

    public int getCardGravity() {
        if(this.hasCardFrame()) {
            LayoutParams existing = (LayoutParams)this.mCardFrame.getLayoutParams();
            return existing.gravity;
        } else {
            return 0;
        }
    }

    public boolean canScrollHorizontally(int direction) {
        return false;
    }

    public int getAvailableScrollDelta(int direction) {
        if(!this.hasCardFrame()) {
            return 0;
        } else {
            int paddingHeight = this.getPaddingTop() + this.getPaddingBottom();
            LayoutParams lp = (LayoutParams)this.mCardFrame.getLayoutParams();
            int marginHeight = lp.topMargin + lp.bottomMargin;
            int cardVerticalSpan = this.mCardFrame.getMeasuredHeight() + paddingHeight + marginHeight;
            if(cardVerticalSpan <= this.getMeasuredHeight()) {
                return 0;
            } else {
                int extra = cardVerticalSpan - this.getMeasuredHeight();
                int avail = 0;
                int sy = this.getScrollY();
                if(this.mCardFrame.getExpansionDirection() == 1) {
                    if(sy >= 0) {
                        if(direction < 0) {
                            avail = -sy;
                        } else if(direction > 0) {
                            avail = Math.max(0, extra - sy);
                        }
                    }
                } else if(this.mCardFrame.getExpansionDirection() == -1 && sy <= 0) {
                    if(direction > 0) {
                        avail = -sy;
                    } else if(direction < 0) {
                        avail = -(extra + sy);
                    }
                }

                if(Log.isLoggable("CardScrollView", 3)) {
                    Log.d("CardScrollView", "getVerticalScrollableDistance: " + Math.max(0, avail));
                }

                return avail;
            }
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(this.mCardFrame != null) {
            MarginLayoutParams lp = (MarginLayoutParams)this.mCardFrame.getLayoutParams();
            int paddingWidth = this.getPaddingLeft() + this.getPaddingRight();
            int paddingHeight = this.getPaddingTop() + this.getPaddingBottom();
            int availableHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingHeight;
            int availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingWidth;
            availableWidth -= lp.leftMargin + lp.rightMargin;
            availableHeight -= lp.topMargin + lp.bottomMargin;
            int widthSpec = MeasureSpec.makeMeasureSpec(availableWidth, 1073741824);
            int heightSpec = MeasureSpec.makeMeasureSpec(availableHeight, -2147483648);
            this.mCardFrame.measure(widthSpec, heightSpec);
        }

        this.setMeasuredDimension(getDefaultSize(this.getSuggestedMinimumWidth(), widthMeasureSpec), getDefaultSize(this.getSuggestedMinimumWidth(), heightMeasureSpec));
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(this.mCardFrame != null) {
            LayoutParams lp = (LayoutParams)this.mCardFrame.getLayoutParams();
            int cardHeight = this.mCardFrame.getMeasuredHeight();
            int cardWidth = this.mCardFrame.getMeasuredWidth();
            int parentHeight = bottom - top;
            boolean alignBottom = false;
            int l;
            if(cardHeight <= parentHeight) {
                alignBottom = (lp.gravity & 112) == 80;
            } else {
                l = this.mCardFrame.getExpansionDirection();
                alignBottom = l == -1;
            }

            l = this.getPaddingLeft() + lp.leftMargin;
            int t = this.getPaddingTop() + lp.topMargin;
            int r = l + cardWidth;
            int b = t + cardHeight;
            if(alignBottom) {
                b = parentHeight - (this.getPaddingBottom() + lp.bottomMargin);
                t = b - cardHeight;
            }

            this.mCardFrame.layout(l, t, r, b);
        }

    }

    int roundAwayFromZero(float v) {
        return (int)(v < 0.0F?Math.floor((double)v):Math.ceil((double)v));
    }

    int roundTowardZero(float v) {
        return (int)(v > 0.0F?Math.floor((double)v):Math.ceil((double)v));
    }
}

