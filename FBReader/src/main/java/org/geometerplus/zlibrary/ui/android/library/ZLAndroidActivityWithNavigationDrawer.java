package org.geometerplus.zlibrary.ui.android.library;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.internal.NavigationMenuPresenter;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.benetech.android.R;
import org.geometerplus.android.fbreader.BookNavigationBookmarkTab;
import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.library.SQLiteBooksDatabase;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.ColorProfile;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Bookmark;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.ui.android.util.BookmarkUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.lang.reflect.Field;

/**
 * Created by animal@martus.org on 10/27/15.
 */
public class ZLAndroidActivityWithNavigationDrawer extends AppCompatActivity implements DialogInterface.OnClickListener{

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationMenu;
    private AlertDialog orientationDialog;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle state) {
        doWorkBeforeCallingSuper();
        super.onCreate(state);

        setContentView(R.layout.main);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationMenu = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationMenu.setNavigationItemSelectedListener(new OnNavigationItemSelectedHandler());
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawerToggle = new ActionBarDrawerToggleHandler(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
		mDrawerToggle.syncState();
		mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_action_bar_menu, menu);

        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
        setupNavigationMenu();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event

        if (item.getItemId() == R.id.toc) {
            ZLApplication.Instance().doAction(ActionCode.SHOW_TOC);
        }
        else if (item.getItemId() == R.id.top_menu_bookmark) {
            addBookmark();
        }


        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupNavigationMenu(){
        FBReaderApp fbReader =  (FBReaderApp) FBReaderApp.Instance();
        if(fbReader != null && ColorProfile.DAY.equals(fbReader.getColorProfileName())){
            MenuItem item = mNavigationMenu.getMenu().findItem(R.id.drawer_item_day);
            item.setTitle(R.string.nav_draw_night);
            item.setIcon(R.drawable.ic_menu_night);
        }
    }

    private void doWorkBeforeCallingSuper() {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
    }

    private void showOrientationPopup() {
        LinearLayout viewGroup = (LinearLayout) findViewById(R.id.popup);
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.screen_orientation_layout, viewGroup);

        if (ZLibrary.Instance().supportsAllOrientations()) {
            layout.findViewById(R.id.reverseLandscape).setEnabled(true);
            layout.findViewById(R.id.reversePortrait).setEnabled(true);
        }

        RadioGroup radioGroup = (RadioGroup) layout.findViewById(R.id.orientationGroup);
        radioGroup.check(findCheckRadioButtonId());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setTitle(getString(R.string.dialog_title_screen_orientation));
        orientationDialog = builder.show();
    }

    private int findCheckRadioButtonId() {
        String value = ZLibrary.Instance().OrientationOption.getValue();
        if (value.equals(ZLibrary.SCREEN_ORIENTATION_SYSTEM))
            return R.id.systemOrientation;

        if (value.equals(ZLibrary.SCREEN_ORIENTATION_SENSOR))
            return R.id.deviceOrientationSensitive;

        if (value.equals(ZLibrary.SCREEN_ORIENTATION_LANDSCAPE))
            return R.id.landscape;

        if (value.equals(ZLibrary.SCREEN_ORIENTATION_PORTRAIT))
            return R.id.portrait;

        if (value.equals(ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT))
            return R.id.reversePortrait;

        if (value.equals(ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE))
            return R.id.reverseLandscape;

        return R.id.systemOrientation;
    }

    public void onSystemClick(View view) {
        handleOrientationChange(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM);
    }

    public void onDeviceOrientationSensetive(View view) {
        handleOrientationChange(ActionCode.SET_SCREEN_ORIENTATION_SENSOR);
    }

    public void onPortrait(View view) {
        handleOrientationChange(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT);
    }

    public void onLandscape(View view) {
        handleOrientationChange(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void onReversePortrait(View view) {
        handleOrientationChange(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT);
    }

    public void onReverseLandscape(View view) {
        handleOrientationChange(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    private void handleOrientationChange(String actionId) {
        ZLApplication.Instance().doAction(actionId);
        orientationDialog.dismiss();
    }

    private void addCurrentOpenBookToFavorites() {
        FBReaderApp fbReader =  (FBReaderApp) FBReaderApp.Instance();
        if (fbReader != null && fbReader.Model != null)
        {
            Book currentOpenBook = fbReader.Model.Book;
            Library libraryInstance = Library.Instance();
            libraryInstance.addBookToFavorites(currentOpenBook);
        }
    }

    private void deleteCurrentBook() {
        FBReaderApp fbReader =  (FBReaderApp) FBReaderApp.Instance();
        FBReaderApp.Instance().onWindowClosing();
        preDeleteBookWork();
        Book currentOpenBook = fbReader.Model.Book;
        if (currentOpenBook.File.getShortName().equals(FBReader.MINI_HELP_FILE_NAME)) {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.message_cannot_delete_guide),
                    Toast.LENGTH_SHORT
                    ).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(currentOpenBook.getTitle())
                .setMessage(getString(R.string.message_confirm_remove_book))
                .setIcon(0)
                .setPositiveButton(getString(R.string.button_label_delete_book), this)
                .setNegativeButton(getString(R.string.button_label_cancel), null)
                .create().show();
    }

    public void onClick(DialogInterface dialog, int which) {
        FBReaderApp fbReader =  (FBReaderApp) FBReaderApp.Instance();
        Book currentOpenBook = fbReader.Model.Book;
        Library.Instance().removeBook(currentOpenBook, Library.REMOVE_FROM_DISK);
        ((SQLiteBooksDatabase)SQLiteBooksDatabase.Instance()).clearBookStatus(currentOpenBook);
        postDeleteBook();
    }

    protected void postDeleteBook() {
        ZLApplication.Instance().doAction(ActionCode.SHOW_HELP);
        ZLApplication.Instance().doAction(ActionCode.SHOW_MY_BOOKS);
    }

    protected void preDeleteBookWork() {
    }

    private class ActionBarDrawerToggleHandler extends ActionBarDrawerToggle{

        public ActionBarDrawerToggleHandler(ZLAndroidActivityWithNavigationDrawer zlAndroidActivityWithActionBar, DrawerLayout mDrawerLayout, Toolbar toolbar, int drawer_open, int drawer_close) {
            super(zlAndroidActivityWithActionBar, mDrawerLayout, toolbar, drawer_open, drawer_close);
        }

        public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }

        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }
    }

    private class OnNavigationItemSelectedHandler implements NavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {

            int menuItemId = menuItem.getItemId();
            if (menuItemId == R.id.drawer_item_my_books_new)
                ZLApplication.Instance().doAction(ActionCode.SHOW_MY_BOOKS);

            if (menuItemId == R.id.drawer_item_search_bookshare)
                ZLApplication.Instance().doAction(ActionCode.BOOKSHARE);

            if (menuItemId == R.id.drawer_item_other_catalogs)
                ZLApplication.Instance().doAction(ActionCode.SHOW_NETWORK_LIBRARY);

            if (menuItemId == R.id.drawer_item_search_text)
                ZLApplication.Instance().doAction(ActionCode.SEARCH);

            if (menuItemId == R.id.drawer_item_day)
                toggleDayNight(menuItem);

            if (menuItemId == R.id.drawer_item_screen_orientation)
                showOrientationPopup();

            if (menuItemId == R.id.drawer_item_book_info)
                ZLApplication.Instance().doAction(ActionCode.SHOW_BOOK_INFO);

            if (menuItemId == R.id.drawer_item_add_to_favorites)
                addCurrentOpenBookToFavorites();

            if (menuItemId == R.id.drawer_item_delete_book)
                deleteCurrentBook();

            mDrawerLayout.closeDrawers();

            return true;
        }

        private void toggleDayNight(MenuItem item){
            String day = getString(R.string.nav_draw_day);
            final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();

            if(day.equals(item.getTitle())){
                item.setTitle(R.string.nav_draw_night);
                item.setIcon(R.drawable.ic_menu_night);
                fbReader.doAction(ActionCode.SWITCH_TO_DAY_PROFILE);
            }
            else {
                item.setTitle(R.string.nav_draw_day);
                item.setIcon(R.drawable.ic_menu_day);
                fbReader.doAction(ActionCode.SWITCH_TO_NIGHT_PROFILE);
            }
            updateNavigationView();
        }
    }



    /**
     * THIS METHOD IS A WORKAROUND THAT USES REFLECTION IN ORDER TO UPDATE THE NAVIGATION DRAWER'S MENU
     * IN THE FUTURE WHEN THE APP IS MOVED TO SDK VERSION 23 OR HIGHER THIS WORKAROUND SHOULD BE REMOVED
     */
    private void updateNavigationView() {
        try {
            Field presenterField = NavigationView.class.getDeclaredField("mPresenter");
            presenterField.setAccessible(true);
            ((NavigationMenuPresenter) presenterField.get(mNavigationMenu)).updateMenuView(false);
        } catch (NoSuchFieldException e) {
            Log.e("Hack crashed", "hack to refresh navigation drawer crashed", e);
        } catch (IllegalAccessException e) {
            Log.e("Hack crashed", "hack to refresh navigation drawer crashed", e);
        }
    }

    private void addBookmark(){

        List<Bookmark> allBooksBookmarks = Bookmark.bookmarks();
        List<Bookmark> bookBookmarks = new ArrayList<>();

        Collections.sort(allBooksBookmarks, new Bookmark.ByTimeComparator());
        final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();

        if (fbreader.Model != null) {
            final long bookId = fbreader.Model.Book.getId();
            for (Bookmark bookmark : allBooksBookmarks) {
                if (bookmark.getBookId() == bookId) {
                    bookBookmarks.add(bookmark);
                }
            }

        }
        BookmarkUtil.addBookmark(bookBookmarks, allBooksBookmarks, this);
    }
}
