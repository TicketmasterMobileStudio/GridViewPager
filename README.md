# GridViewPager
Android Wear's GridViewPager rebuilt for phone and tablet use. Backwards compatibility was added to API-16. Then *beefed* up.

## Where's the _beef_:

- GridPagerTransformer has been added
- Duration of programmatic page changing can be set.
- Any Nested ScrollView works correctly
- Offsets controlled by individual pages in the adapter. 
- Corner offsets controlled by the GridViewpager
- In the adapter individual pages can decide the swiping directions they support; left, up, right and down.
- Added SupportFragmentGridPagerAdapter.
- Supports Android starting at API 16. Forking could bring it back further but why?

### Adapter Power

The adapter is a more powerful class in GridViewPager then it is in the traditional ViewPager.

- `isXSwipingAllow` can turn on and off any direction for an individual position. This can allow you to control the flow that the user has to take.
- Offsets are controlled by individual pages. Each page can override its own row and column offset.

```java
 
    @Override
    public int getColumnOffscreenPageCount(int row, int column) {
        return row == 0 ? 2 : 0;
    }

    @Override
    public int getRowOffscreenPageCount(int row, int column) {
        return 1;
    }
   
```
 
 - `getCurrentColumnForRow` comes from the wear support lib. It controls which column swiping vertical will take you to. The following will give you a standard grid.
 
```java
 
    @Override
    public int getCurrentColumnForRow(int row, int currentColumn) {
        return currentColumn;
    }
 
```


### GridViewPager Power

- GridPagerTransformer, works just like PagerTransformer but gives x and y offsets instead of just a page offset.
- `setSlideAnimationDuration` Will set the duration of programmatic page changing. 
- Corner Offsets will allow creation of the page in the upper left, upper right, lower left and lower right. This will always respect the offsets described by the adapter for the surrounding individual positions but beyond that there is no additional control. This is useful when animating side pages.
- All scrollable views have full support. This includes ScrollView, ListView, RecyclerView and any view that responses correctly to `isScrollable` methods. The GridViewPager will look at the root view and its direct children. Any deeper then that is not currently supported.


### What isn't supported

- Edge effect. This is purely because we didn't need it. Pull requests are welcome with individual directional and color control.