package com.benny.openlauncher.core.widget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.benny.openlauncher.core.R;
import com.benny.openlauncher.core.activity.Home;
import com.benny.openlauncher.core.interfaces.Item;
import com.benny.openlauncher.core.manager.Setup;
import com.benny.openlauncher.core.util.DragAction;
import com.benny.openlauncher.core.util.DragDropHandler;
import com.benny.openlauncher.core.util.Tool;
import com.benny.openlauncher.core.viewutil.DesktopCallBack;
import com.benny.openlauncher.core.viewutil.DesktopGestureListener;
import com.benny.openlauncher.core.viewutil.SmoothPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import in.championswimmer.sfg.lib.SimpleFingerGestures;

public class Desktop extends SmoothViewPager implements OnDragListener, DesktopCallBack {
    public List<CellContainer> pages = new ArrayList<>();
    public OnDesktopEditListener desktopEditListener;
    public View previousItemView;
    public Item previousItem;
    public boolean inEditMode;
    public int previousPage = -1;
    public int pageCount;

    private float startPosX;
    private float startPosY;
    private Home home;
    private PagerIndicator pageIndicator;

    public static int topInset;
    public static int bottomInset;

    public Desktop(Context c) {
        this(c, null);
    }

    public Desktop(Context c, AttributeSet attr) {
        super(c, attr);
    }

    public void setDesktopEditListener(OnDesktopEditListener desktopEditListener) {
        this.desktopEditListener = desktopEditListener;
    }

    public void setPageIndicator(PagerIndicator pageIndicator) {
        this.pageIndicator = pageIndicator;
    }

    public void init() {
        if (isInEditMode()) {
            return;
        }

        pageCount = Home.launcher.db.getDesktop().size();
        if (pageCount == 0) {
            pageCount = 1;
        }

        setOnDragListener(this);
        setCurrentItem(Setup.appSettings().getDesktopPageCurrent());
    }

    public void initDesktopNormal(Home home) {
        setAdapter(new DesktopAdapter(this));
        if (Setup.appSettings().isDesktopShowIndicator() && pageIndicator != null) {
            pageIndicator.setViewPager(this);
        }
        this.home = home;

        int columns = Setup.appSettings().getDesktopColumnCount();
        int rows = Setup.appSettings().getDesktopRowCount();
        List<List<Item>> desktopItems = Home.launcher.db.getDesktop();
        for (int pageCount = 0; pageCount < desktopItems.size(); pageCount++) {
            if (pages.size() <= pageCount) break;
            pages.get(pageCount).removeAllViews();
            List<Item> items = desktopItems.get(pageCount);
            for (int j = 0; j < items.size(); j++) {
                Item item = items.get(j);
                if (((item.getX() + item.getSpanX()) <= columns) && ((item.getY() + item.getSpanY()) <= rows)) {
                    addItemToPage(item, pageCount);
                }
            }
        }
    }

    public void initDesktopShowAll(Context c, Home home) {
        List<Item> apps = Setup.get().createAllAppItems(c);
//        List<AppManager.App> apps = AppManager.getInstance(c).getApps();
        int appsSize = apps.size();

        // reset page count
        pageCount = 0;
        int columns = Setup.appSettings().getDesktopColumnCount();
        int rows = Setup.appSettings().getDesktopRowCount();
        while ((appsSize = appsSize - (columns * rows)) >= (columns * rows) || (appsSize > -(columns * rows))) {
            pageCount++;
        }

        setAdapter(new DesktopAdapter(this));
        if (Setup.appSettings().isDesktopShowIndicator() && pageIndicator != null) {
            pageIndicator.setViewPager(this);
        }
        this.home = home;

        // fill the desktop adapter
        for (int i = 0; i < pageCount; i++) {
            for (int x = 0; x < columns; x++) {
                for (int y = 0; y < rows; y++) {
                    int pagePos = y * rows + x;
                    int pos = columns * rows * i + pagePos;
                    if (!(pos >= apps.size())) {
                        Item appItem = apps.get(pos);
                        appItem.setX(x);
                        appItem.setY(y);
                        addItemToPage(appItem, i);
                    }
                }
            }
        }
    }

    public void addPageRight() {
        pageCount++;

        final int previousPage = getCurrentItem();
        ((DesktopAdapter) getAdapter()).addPageRight();
        setCurrentItem(previousPage + 1);

        for (CellContainer cellContainer : pages) {
            cellContainer.setHideGrid(false);
        }
        pageIndicator.invalidate();
    }

    public void addPageLeft() {
        pageCount++;

        final int previousPage = getCurrentItem();
        ((DesktopAdapter) getAdapter()).addPageLeft();
        setCurrentItem(previousPage + 1, false);
        setCurrentItem(previousPage - 1);

        for (CellContainer cellContainer : pages) {
            cellContainer.setHideGrid(false);
        }
        pageIndicator.invalidate();
    }

    public void removeCurrentPage() {
        if (pageCount == 1) return;
        if (Setup.appSettings().getDesktopStyle() == DesktopMode.SHOW_ALL_APPS)
            return;
        pageCount--;

        int previousPage = getCurrentItem();
        ((DesktopAdapter) getAdapter()).removePage(getCurrentItem());

        for (CellContainer v : pages) {
            v.setAlpha(0);
            v.animate().alpha(1);
            v.setScaleX(0.85f);
            v.setScaleY(0.85f);
            v.animateBackgroundShow();
        }

        setCurrentItem(previousPage);
        pageIndicator.invalidate();
    }

    @Override
    public boolean onDrag(View p1, DragEvent p2) {
        switch (p2.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                Tool.print("ACTION_DRAG_STARTED");
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                Tool.print("ACTION_DRAG_ENTERED");
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                Tool.print("ACTION_DRAG_EXITED");
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                Tool.print("ACTION_DRAG_LOCATION");
                getCurrentPage().peekItemAndSwap(p2);
                return true;
            case DragEvent.ACTION_DROP:
                Tool.print("ACTION_DROP");
                Item item = DragDropHandler.getDraggedObject(p2);

                // this statement makes sure that adding an app multiple times from the app drawer works
                // the app will get a new id every time
                if (((DragAction) p2.getLocalState()).action == DragAction.Action.APP_DRAWER) {
                    item.reset();
                }

                if (addItemToPoint(item, (int) p2.getX(), (int) p2.getY())) {
                    home.desktop.consumeRevert();
                    home.dock.consumeRevert();

                    // add the item to the database
                    home.db.setItem(item, getCurrentItem(), 1);
                } else {
                    Point pos = getCurrentPage().touchPosToCoordinate((int) p2.getX(), (int) p2.getY(), item.getSpanX(), item.getSpanY(), false);
                    View itemView = getCurrentPage().coordinateToChildView(pos);
                    if (itemView != null && Desktop.handleOnDropOver(home, item, (Item) itemView.getTag(), itemView, getCurrentPage(), getCurrentItem(), 1, this)) {
                        home.desktop.consumeRevert();
                        home.dock.consumeRevert();
                    } else {
                        Toast.makeText(getContext(), R.string.toast_not_enough_space, Toast.LENGTH_SHORT).show();
                        home.dock.revertLastItem();
                        home.desktop.revertLastItem();
                    }
                }
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                Tool.print("ACTION_DRAG_ENDED");
                return true;
        }
        return false;
    }

    public CellContainer getCurrentPage() {
        return pages.get(getCurrentItem());
    }

    @Override
    public void setLastItem(Object... args) {
        // args stores the item in [0] and the view reference in [1]
        Item item = (Item) args[0];
        View v = (View) args[1];

        previousPage = getCurrentItem();
        previousItemView = v;
        previousItem = item;
        getCurrentPage().removeView(v);
    }

    @Override
    public void revertLastItem() {
        if (previousItemView != null && getAdapter().getCount() >= previousPage && previousPage > -1) {
            getCurrentPage().addViewToGrid(previousItemView);
            previousItem = null;
            previousItemView = null;
            previousPage = -1;
        }
    }

    @Override
    public void consumeRevert() {
        previousItem = null;
        previousItemView = null;
        previousPage = -1;
    }

    @Override
    public boolean addItemToPage(final Item item, int page) {
        View itemView = Setup.get().getItemView(getContext(), item, Setup.appSettings().isDesktopShowLabel(), this);

        if (itemView == null) {
            home.db.deleteItem(item);
            return false;
        } else {
            pages.get(page).addViewToGrid(itemView, item.getX(), item.getY(), item.getSpanX(), item.getSpanY());
            return true;
        }
    }

    @Override
    public boolean addItemToPoint(final Item item, int x, int y) {
        CellContainer.LayoutParams positionToLayoutPrams = getCurrentPage().coordinateToLayoutParams(x, y, item.getSpanX(), item.getSpanY());
        if (positionToLayoutPrams != null) {
            item.setX(positionToLayoutPrams.x);
            item.setY(positionToLayoutPrams.y);

            View itemView = Setup.get().getItemView(getContext(), item, Setup.appSettings().isDesktopShowLabel(), this);

            if (itemView != null) {
                itemView.setLayoutParams(positionToLayoutPrams);
                getCurrentPage().addView(itemView);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addItemToCell(final Item item, int x, int y) {
        item.setX(x);
        item.setY(y);

        View itemView = Setup.get().getItemView(getContext(), item, Setup.appSettings().isDesktopShowLabel(), this);

        if (itemView != null) {
            getCurrentPage().addViewToGrid(itemView, item.getX(), item.getY(), item.getSpanX(), item.getSpanY());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeItem(View view) {
        getCurrentPage().removeViewInLayout(view);
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        if (isInEditMode()) {
            return;
        }
        WallpaperManager.getInstance(getContext()).setWallpaperOffsets(getWindowToken(), (position + offset) / (pageCount - 1), 0);
        super.onPageScrolled(position, offset, offsetPixels);
    }

    public class DesktopAdapter extends SmoothPagerAdapter {
        private MotionEvent currentEvent;
        private Desktop desktop;
        float scaleFactor = 1f;

        public DesktopAdapter(Desktop desktop) {
            this.desktop = desktop;
            desktop.pages = new ArrayList<>();
            for (int i = 0; i < getCount(); i++) {
                desktop.pages.add(getItemLayout());
            }
        }

        public void addPageLeft() {
            desktop.pages.add(0, getItemLayout());
            notifyDataSetChanged();
        }

        public void addPageRight() {
            desktop.pages.add(getItemLayout());
            notifyDataSetChanged();
        }

        public void removePage(int position) {
            desktop.pages.remove(position);
            notifyDataSetChanged();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return desktop.pageCount;
        }

        @Override
        public boolean isViewFromObject(View p1, Object p2) {
            return p1 == p2;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int pos) {
            ViewGroup layout = desktop.pages.get(pos);
            container.addView(layout);
            return layout;
        }

        private SimpleFingerGestures.OnFingerGestureListener getGestureListener() {
            return new DesktopGestureListener(desktop, Setup.get().getDrawerGestureCallback());
        }

        private CellContainer getItemLayout() {
            CellContainer layout = new CellContainer(desktop.getContext());
            layout.setSoundEffectsEnabled(false);

            SimpleFingerGestures mySfg = new SimpleFingerGestures();
            mySfg.setOnFingerGestureListener(getGestureListener());

            layout.gestures = mySfg;
            layout.onItemRearrangeListener = new CellContainer.OnItemRearrangeListener() {
                @Override
                public void onItemRearrange(Point from, Point to) {
                    Item itemFromCoordinate = Desktop.getItemFromCoordinate(from, getCurrentItem());
                    if (itemFromCoordinate == null) return;
                    itemFromCoordinate.setX(to.x);
                    itemFromCoordinate.setY(to.y);
                }
            };
            layout.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    currentEvent = event;
                    return false;
                }
            });
            layout.setGridSize(Setup.appSettings().getDesktopColumnCount(), Setup.appSettings().getDesktopRowCount());
            layout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    scaleFactor = 1f;
                    for (final CellContainer v : desktop.pages) {
                        v.blockTouch = false;
                        v.animateBackgroundHide();
                        v.animate().scaleX(scaleFactor).scaleY(scaleFactor).setInterpolator(new AccelerateDecelerateInterpolator());
                    }
                    if (!desktop.inEditMode && currentEvent != null) {
                        WallpaperManager.getInstance(view.getContext()).sendWallpaperCommand(view.getWindowToken(), WallpaperManager.COMMAND_TAP, (int) currentEvent.getX(), (int) currentEvent.getY(), 0, null);
                    }
                    desktop.inEditMode = false;
                    if (desktop.desktopEditListener != null) {
                        desktop.desktopEditListener.onFinishDesktopEdit();
                    }
                }
            });
            layout.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    scaleFactor = 0.75f;
                    for (CellContainer v : desktop.pages) {
                        v.blockTouch = true;
                        v.animateBackgroundShow();
                        v.animate().scaleX(scaleFactor).scaleY(scaleFactor).setInterpolator(new AccelerateDecelerateInterpolator());
                    }
                    desktop.inEditMode = true;
                    if (desktop.desktopEditListener != null) {
                        desktop.desktopEditListener.onDesktopEdit();
                    }
                    return true;
                }
            });
            return layout;
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            topInset = insets.getSystemWindowInsetTop();
            bottomInset = insets.getSystemWindowInsetBottom();
            Home.launcher.updateHomeLayout();
        }
        return insets;
    }

    public static Item getItemFromCoordinate(Point point, int page) {
        List<List<Item>> desktopItems = Home.launcher.db.getDesktop();
        List<Item> pageData = desktopItems.get(page);
        for (int i = 0; i < pageData.size(); i++) {
            Item item = pageData.get(i);
            if (item.getX() == point.x && item.getY() == point.y && item.getSpanX() == 1 && item.getSpanY() == 1) {
                return pageData.get(i);
            }
        }
        return null;
    }

    public static boolean handleOnDropOver(Home home, Item dropItem, Item item, View itemView, ViewGroup parent, int page, int desktop, DesktopCallBack callback) {
        if (item == null || dropItem == null) {
            return false;
        }

        switch (item.getType()) {
            case APP:
            case SHORTCUT:
                if (dropItem.getType() == Item.Type.APP || dropItem.getType() == Item.Type.SHORTCUT) {
                    parent.removeView(itemView);

                    // create a new group item
                    Item group = Setup.get().newGroupItem();
                    group.getGroupItems().add(item);
                    group.getGroupItems().add(dropItem);
                    group.setX(item.getX());
                    group.setY(item.getY());

                    // add the drop item just in case it is coming from the app drawer
                    home.db.setItem(dropItem, page, desktop);

                    // hide the apps added to the group
                    home.db.updateItem(item, 0);
                    home.db.updateItem(dropItem, 0);

                    // add the item to the database
                    home.db.setItem(group, page, desktop);

                    callback.addItemToPage(group, page);

                    home.desktop.consumeRevert();
                    home.dock.consumeRevert();
                    return true;
                }
                break;
            case GROUP:
                if ((dropItem.getType() == Item.Type.APP || dropItem.getType() == Item.Type.SHORTCUT) && item.getGroupItems().size() < GroupPopupView.GroupDef.maxItem) {
                    parent.removeView(itemView);

                    item.getGroupItems().add(dropItem);

                    // add the drop item just in case it is coming from the app drawer
                    home.db.setItem(dropItem, page, desktop);

                    // hide the new app in the group
                    home.db.updateItem(dropItem, 0);

                    // add the item to the database
                    home.db.setItem(item, page, desktop);

                    callback.addItemToPage(item, page);

                    home.desktop.consumeRevert();
                    home.dock.consumeRevert();
                    return true;
                }
                break;
        }
        return false;
    }

    public static class DesktopMode {
        public static final int NORMAL = 0;
        public static final int SHOW_ALL_APPS = 1;
    }

    public interface OnDesktopEditListener {
        void onDesktopEdit();

        void onFinishDesktopEdit();
    }
}
