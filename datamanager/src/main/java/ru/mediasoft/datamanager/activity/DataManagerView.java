package ru.mediasoft.datamanager.activity;

import android.content.Intent;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;

public interface DataManagerView {
    void showRange(int offset, int countRowInPage, int countAllRows);

    void showError(String текстОшибки);
    void hideError();

    void hideProgress();
    void showProgress();

    void showTableSettings();
    void hideTableSettings();

    void hideDbHelper();
    void showDbHelper();


    void resetScroll();

    void showTableName(String table);

    void tableUpdate(TableRow[] rows);

    void showTextEditContainer(boolean visibility);

    TableLayout getTableLayout();

    void runOnUiThread(Runnable runnable);

    boolean errorIsVisible();

    void openEditRowFragment(String tableName, String rowId, boolean isAdd);

    void backToApp();

    void startActivityForResult(Intent intent, int requestCode);
}
