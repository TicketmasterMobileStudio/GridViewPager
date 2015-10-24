package com.ticketmaster.mobilestudio.gridviewpager;

import android.view.View;

public interface GridPageTransformer {
    void transformPage(View page, int column, int row, float positionX, float positionY);
}
