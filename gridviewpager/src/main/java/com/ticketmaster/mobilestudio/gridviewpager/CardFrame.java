package com.ticketmaster.mobilestudio.gridviewpager;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

@TargetApi(20)
public class CardFrame extends ViewGroup {
    private static final String TAG = "CardFrame";
    private static final boolean DEBUG = false;
    private static float BOX_FACTOR = 0.146467F;
    public static final float NO_EXPANSION = 1.0F;
    public static final int EXPAND_UP = -1;
    public static final int EXPAND_DOWN = 1;
    private static final int EDGE_FADE_DISTANCE_DP = 40;
    private static final int DEFAULT_CONTENT_PADDING_DP = 12;
    private static final int DEFAULT_CONTENT_PADDING_TOP_DP = 8;
    private boolean mCanExpand;
    private boolean mExpansionEnabled;
    private float mExpansionFactor;
    private int mExpansionDirection;
    private final int mEdgeFadeDistance;
    private final Rect mChildClipBounds;
    private int mCardBaseHeight;
    private boolean mRoundDisplay;
    private int mBoxInset;
    private final Rect mInsetPadding;
    private final Rect mContentPadding;
    private boolean mHasBottomInset;
    private final CardFrame.EdgeFade mEdgeFade;

    public CardFrame(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    public CardFrame(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardFrame(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mExpansionEnabled = true;
        this.mExpansionFactor = 1.0F;
        this.mExpansionDirection = 1;
        this.mChildClipBounds = new Rect();
        this.mInsetPadding = new Rect();
        this.mContentPadding = new Rect();
        this.mEdgeFade = new CardFrame.EdgeFade();
        Resources res = context.getResources();
        float density = res.getDisplayMetrics().density;
        this.mEdgeFadeDistance = (int)(40.0F * density);
        this.setBackgroundResource(R.drawable.card_background);
        int defaultContentPadding = (int)(12.0F * density);
        int defaultContentPaddingTop = (int)(8.0F * density);
        this.setContentPadding(defaultContentPadding, defaultContentPaddingTop, defaultContentPadding, defaultContentPadding);
    }

    public void setContentPadding(int left, int top, int right, int bottom) {
        this.mContentPadding.set(left, top, right, bottom);
        this.requestLayout();
    }

    public int getContentPaddingLeft() {
        return this.mContentPadding.left;
    }

    public int getContentPaddingRight() {
        return this.mContentPadding.right;
    }

    public int getContentPaddingTop() {
        return this.mContentPadding.top;
    }

    public int getContentPaddingBottom() {
        return this.mContentPadding.bottom;
    }

    public void setExpansionEnabled(boolean enabled) {
        this.mExpansionEnabled = enabled;
        this.requestLayout();
        this.invalidate();
    }

    public void setExpansionDirection(int direction) {
        this.mExpansionDirection = direction;
        this.requestLayout();
        this.invalidate();
    }

    public void setExpansionFactor(float expansionFactor) {
        this.mExpansionFactor = expansionFactor;
        this.requestLayout();
        this.invalidate();
    }

    public int getExpansionDirection() {
        return this.mExpansionDirection;
    }

    public boolean isExpansionEnabled() {
        return this.mExpansionEnabled;
    }

    public float getExpansionFactor() {
        return this.mExpansionFactor;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.requestApplyInsets();
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        boolean round = insets.isRound();
        if(round != this.mRoundDisplay) {
            this.mRoundDisplay = round;
            this.requestLayout();
        }

        boolean inset = insets.getSystemWindowInsetBottom() > 0;
        if(inset != this.mHasBottomInset) {
            this.mHasBottomInset = inset;
            this.requestLayout();
        }

        return insets.consumeSystemWindowInsets();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int logicalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int logicalHeight = MeasureSpec.getSize(heightMeasureSpec);
        int cardMeasuredHeight;
        int parentHeightSize;
        if(this.mRoundDisplay) {
            MarginLayoutParams cardMeasuredWidth = (MarginLayoutParams)this.getLayoutParams();
            this.mInsetPadding.setEmpty();
            cardMeasuredHeight = 0;
            int content = 0;
            parentHeightSize = 0;
            if(cardMeasuredWidth.leftMargin < 0) {
                cardMeasuredHeight = -cardMeasuredWidth.leftMargin;
                logicalWidth -= cardMeasuredHeight;
            }

            if(cardMeasuredWidth.rightMargin < 0) {
                parentHeightSize = -cardMeasuredWidth.rightMargin;
                logicalWidth -= parentHeightSize;
            }

            if(cardMeasuredWidth.bottomMargin < 0) {
                content = -cardMeasuredWidth.bottomMargin;
                logicalHeight -= content;
            }

            this.mBoxInset = (int)(BOX_FACTOR * (float)Math.max(logicalWidth, logicalHeight));
            this.mInsetPadding.left = this.mBoxInset - (this.getPaddingLeft() - cardMeasuredHeight);
            this.mInsetPadding.right = this.mBoxInset - (this.getPaddingRight() - parentHeightSize);
            if(!this.mHasBottomInset) {
                this.mInsetPadding.bottom = this.mBoxInset - (this.getPaddingBottom() - content);
            }
        }

        int cardMeasuredWidth1 = getDefaultSize(this.getSuggestedMinimumWidth(), widthMeasureSpec, true);
        cardMeasuredHeight = getDefaultSize(this.getSuggestedMinimumHeight(), heightMeasureSpec, false);
        if(this.getChildCount() == 0) {
            this.setMeasuredDimension(cardMeasuredWidth1, cardMeasuredHeight);
        } else {
            View content1 = this.getChildAt(0);
            parentHeightSize = MeasureSpec.getSize(heightMeasureSpec);
            int parentHeightMode = MeasureSpec.getMode(heightMeasureSpec);
            int childWidthMeasureSpecMode = MeasureSpec.EXACTLY;
            boolean cardHeightMatchContent = false;
            this.mCanExpand = this.mExpansionEnabled;
            int childHeightMeasureSpecSize;
            int childHeightMeasureSpecMode;
            if(parentHeightMode != 0 && parentHeightSize != 0) {
                if(parentHeightMode == MeasureSpec.EXACTLY) {
                    Log.w("CardFrame", "height measure spec passed with mode EXACT");
                    this.mCanExpand = false;
                    this.mCardBaseHeight = parentHeightSize;
                    cardMeasuredHeight = this.mCardBaseHeight;
                    childHeightMeasureSpecMode = MeasureSpec.EXACTLY;
                    childHeightMeasureSpecSize = cardMeasuredHeight;
                } else {
                    this.mCardBaseHeight = parentHeightSize;
                    cardMeasuredHeight = this.mCardBaseHeight;
                    if(this.mCanExpand) {
                        cardMeasuredHeight = (int)((float)cardMeasuredHeight * this.mExpansionFactor);
                    }

                    if(this.mExpansionDirection == -1) {
                        childHeightMeasureSpecMode = MeasureSpec.UNSPECIFIED;
                        childHeightMeasureSpecSize = MeasureSpec.UNSPECIFIED;
                    } else {
                        childHeightMeasureSpecMode = MeasureSpec.AT_MOST;
                        childHeightMeasureSpecSize = cardMeasuredHeight + this.getPaddingBottom();
                    }
                }
            } else {
                Log.w("CardFrame", "height measure spec passed with mode UNSPECIFIED, or zero height.");
                this.mCanExpand = false;
                this.mCardBaseHeight = 0;
                cardMeasuredHeight = 0;
                cardHeightMatchContent = true;
                childHeightMeasureSpecMode = MeasureSpec.UNSPECIFIED;
                childHeightMeasureSpecSize = MeasureSpec.UNSPECIFIED;
            }

            int paddingWidth = this.getPaddingLeft() + this.getPaddingRight() + this.mContentPadding.left + this.mContentPadding.right + this.mInsetPadding.left + this.mInsetPadding.right;
            int paddingHeight = this.getPaddingTop() + this.getPaddingBottom() + this.mContentPadding.top + this.mContentPadding.bottom + this.mInsetPadding.top + this.mInsetPadding.bottom;
            int childWidthSpec = MeasureSpec.makeMeasureSpec(cardMeasuredWidth1 - paddingWidth, childWidthMeasureSpecMode);
            int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeightMeasureSpecSize - paddingHeight, childHeightMeasureSpecMode);
            LayoutParams lp = content1.getLayoutParams();
            childWidthSpec = getChildMeasureSpec(childWidthSpec, 0, lp.width);
            content1.measure(childWidthSpec, childHeightSpec);
            if(cardHeightMatchContent) {
                cardMeasuredHeight = content1.getMeasuredHeight() + paddingHeight;
            } else {
                cardMeasuredHeight = Math.min(cardMeasuredHeight, content1.getMeasuredHeight() + paddingHeight);
                this.mCanExpand &= content1.getMeasuredHeight() > cardMeasuredHeight - paddingHeight;
            }

            this.setMeasuredDimension(cardMeasuredWidth1, cardMeasuredHeight);
        }
    }

    public static int getDefaultSize(int size, int measureSpec, boolean greedy) {

        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch(specMode) {
            case MeasureSpec.AT_MOST:
                result = greedy?specSize:size;
                break;
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
        }

        return result;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(this.getChildCount() != 0) {
            View content = this.getChildAt(0);
            int parentHeight = bottom - top;
            int l = this.getPaddingLeft() + this.mInsetPadding.left + this.mContentPadding.left;
            int r = l + content.getMeasuredWidth();
            int t;
            int b;
            if(this.mExpansionDirection == -1) {
                b = parentHeight;
                t = parentHeight - (content.getMeasuredHeight() + this.getPaddingBottom() + this.mInsetPadding.bottom + this.mContentPadding.bottom);
            } else {
                t = this.getPaddingTop() + this.mInsetPadding.top + this.mContentPadding.top;
                b = t + content.getMeasuredHeight();
            }

            content.layout(l, t, r, b);
        }
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int fadeDistance = this.mEdgeFadeDistance;
        boolean more = false;
        boolean bottomFade = false;
        boolean topFade = false;
        this.mChildClipBounds.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        int paddingHeight = this.getPaddingTop() + this.getPaddingBottom();
        int contentHeight = child.getHeight();
        if(this.mCanExpand) {
            if(this.mExpansionDirection == -1 && contentHeight + paddingHeight > this.getHeight()) {
                topFade = true;
                this.mChildClipBounds.top = this.getPaddingTop();
            } else if(this.mExpansionDirection == 1 && contentHeight + paddingHeight > this.getHeight()) {
                bottomFade = true;
                this.mChildClipBounds.bottom = this.getHeight() - this.getPaddingBottom();
            }
        }

        int saveCount = canvas.getSaveCount();
        canvas.clipRect(this.mChildClipBounds);
        boolean flags = true;
        if(topFade) {
            canvas.saveLayer((float)this.mChildClipBounds.left, (float)this.mChildClipBounds.top, (float)this.mChildClipBounds.right, (float)(this.mChildClipBounds.top + fadeDistance), (Paint)null, 4);
        }

        if(bottomFade) {
            canvas.saveLayer((float)this.mChildClipBounds.left, (float)(this.mChildClipBounds.bottom - fadeDistance), (float)this.mChildClipBounds.right, (float)this.mChildClipBounds.bottom, (Paint)null, 4);
        }

        more = super.drawChild(canvas, child, drawingTime);
        if(topFade) {
            this.mEdgeFade.matrix.reset();
            this.mEdgeFade.matrix.setScale(1.0F, (float)fadeDistance);
            this.mEdgeFade.matrix.postTranslate((float)this.mChildClipBounds.left, (float)this.mChildClipBounds.top);
            this.mEdgeFade.shader.setLocalMatrix(this.mEdgeFade.matrix);
            this.mEdgeFade.paint.setShader(this.mEdgeFade.shader);
            canvas.drawRect((float)this.mChildClipBounds.left, (float)this.mChildClipBounds.top, (float)this.mChildClipBounds.right, (float)(this.mChildClipBounds.top + fadeDistance), this.mEdgeFade.paint);
        }

        if(bottomFade) {
            this.mEdgeFade.matrix.reset();
            this.mEdgeFade.matrix.setScale(1.0F, (float)fadeDistance);
            this.mEdgeFade.matrix.postRotate(180.0F);
            this.mEdgeFade.matrix.postTranslate((float)this.mChildClipBounds.left, (float)this.mChildClipBounds.bottom);
            this.mEdgeFade.shader.setLocalMatrix(this.mEdgeFade.matrix);
            this.mEdgeFade.paint.setShader(this.mEdgeFade.shader);
            canvas.drawRect((float)this.mChildClipBounds.left, (float)(this.mChildClipBounds.bottom - fadeDistance), (float)this.mChildClipBounds.right, (float)this.mChildClipBounds.bottom, this.mEdgeFade.paint);
        }

        canvas.restoreToCount(saveCount);
        return more;
    }

    public void addView(View child) {
        if(this.getChildCount() > 0) {
            throw new IllegalStateException("CardFrame can host only one direct child");
        } else {
            super.addView(child);
        }
    }

    public void addView(View child, int index) {
        if(this.getChildCount() > 0) {
            throw new IllegalStateException("CardFrame can host only one direct child");
        } else {
            super.addView(child, index);
        }
    }

    public void addView(View child, LayoutParams params) {
        if(this.getChildCount() > 0) {
            throw new IllegalStateException("CardFrame can host only one direct child");
        } else {
            super.addView(child, params);
        }
    }

    public void addView(View child, int index, LayoutParams params) {
        if(this.getChildCount() > 0) {
            throw new IllegalStateException("CardFrame can host only one direct child");
        } else {
            super.addView(child, index, params);
        }
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(CardFrame.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(CardFrame.class.getName());
    }

    private static class EdgeFade {
        private final Shader shader;
        private final Paint paint;
        private final Matrix matrix = new Matrix();

        public EdgeFade() {
            this.shader = new LinearGradient(0.0F, 0.0F, 0.0F, 1.0F, -16777216, 0, TileMode.CLAMP);
            this.paint = new Paint();
            this.paint.setShader(this.shader);
            this.paint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        }
    }
}
