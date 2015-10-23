package com.ticketmaster.mobilestudio.gridviewpager;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;

@TargetApi(20)
public interface GridPageOptions {
    Drawable getBackground();

    void setBackgroundListener(GridPageOptions.BackgroundListener var1);

    interface BackgroundListener {
        void notifyBackgroundChanged();
    }
}
