package ru.mediasoft.datamanager.activity;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableRow;

import ru.mediasoft.datamanager.DataManager;
import ru.mediasoft.datamanager.DbHelper;
import ru.mediasoft.datamanager.R;
import ru.mediasoft.datamanager.Utils;

public class DataManagerPresenter {
    private DataManagerView view;
    private Context context;
    private String dbPath;

    public DbHelper dbHelper = new DbHelper();
    private String[] tables;
    private int[] rowCounts;
    private Dialog dialogTableList;
    private int limitRows = 200;
    private int selectedPage = 0;
    public int selectedTable = -1;

    private AsyncTask<Void, Void, TableRow[]> asyncTask;

    private Utils.Row selectedRow = null;
    private View selectedView = null;
    private int measuredWidth;
    public void onCreate(DataManagerView dataManagerView, Context context) {
        view = dataManagerView;
        this.context = context;

        dbPath = DataManager.dbPath;

        loadTables();
    }

    public void onStart(int measuredWidth) {
        this.measuredWidth = measuredWidth;
    }

    private void loadTables() {
        if (dbPath == null || dbPath.isEmpty()) {
            view.showError("Не передан путь до файла");
            return;
        }

        dbPath = Utils.pathToFullPath(context, dbPath);

        try {
            dbHelper.init(dbPath);
        } catch (Throwable throwable) {
            view.showError("Не удалось подключиться к \n\n" + dbPath);
            return;
        }

        tables = dbHelper.getTableNames();
        rowCounts = dbHelper.getRowCountsForEachTable(tables);

        view.hideProgress();

        showTables();

        view.showRange(0, 0, 0);
    }

    public void showTables() {
        if (tables.length == 0) {
            view.showError("Отсутствуют таблицы в БД");
        } else {
            getTableListDialog().show();
        }
    }

    @NonNull
    private Dialog getTableListDialog() {
        if (dialogTableList != null) return dialogTableList;

        dialogTableList = new Dialog(context);
        dialogTableList.setContentView(R.layout.dialog_list);

        ListView listView = dialogTableList.findViewById(R.id.list_view);
        dialogTableList.setCancelable(true);
        dialogTableList.setTitle("Выберите таблицу");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, tables);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedTable = position;
            selectedPage = 0;
            this.view.resetScroll();
            openTable(tables[selectedTable], 0);
            dialogTableList.dismiss();
            normalShowRange();
        });
        return dialogTableList;
    }

    private void normalShowRange() {
        this.view.showRange(selectedPage * limitRows,
                selectedTable < 0 ? 0 : (rowCounts[selectedTable] >= (selectedPage + 1) * limitRows ? limitRows : (rowCounts[selectedTable] - selectedPage * limitRows)),
                selectedTable < 0 ? -1 : rowCounts[selectedTable]);
    }

    private void openTable(final String table, final int offset) {
        view.showTableSettings();
        processOpenTable(table, () -> dbHelper.getTableCursor(table, limitRows, offset));
    }

    private void processOpenTable(String table, Utils.CallBack<Cursor> callBack) {
        view.showProgress();
        view.showTableName(table);

        if (asyncTask != null) asyncTask.cancel(true);

        asyncTask = getAsyncTask(callBack);
        asyncTask.execute();
    }

    @NonNull
    private AsyncTask<Void, Void, TableRow[]> getAsyncTask(final Utils.CallBack<Cursor> callBack) {
        return new AsyncTask<Void, Void, TableRow[]>() {
            @Override
            protected TableRow[] doInBackground(Void... params) {
                final Cursor cursor = callBack.call();
                final TableRow[] rows = Utils.reViewTable(view.getTableLayout(), cursor, rowClickListener, rowLongClickListener, measuredWidth);
                if (cursor != null)
                    cursor.close();
                return rows;
            }

            @Override
            protected void onPostExecute(TableRow[] rows) {
                view.tableUpdate(rows);
                view.resetScroll();
                view.hideProgress();
            }
        };
    }


    private View.OnClickListener rowClickListener = v -> {
        if (v.getTag() instanceof Utils.Row) {
            final Utils.Row row = (Utils.Row) v.getTag();

            if (selectedRow == null || selectedRow.position != row.position) {
                if (selectedRow != null && selectedView != null) {
                    selectedView.setBackgroundResource(R.drawable.unselected_row_background);
                }
                selectedRow = row;
                selectedView = v;

                selectedView.setBackgroundResource(R.drawable.selected_row_background);

                if (selectedTable >= 0) view.showDbHelper();
            } else {
                v.setBackgroundResource(R.drawable.unselected_row_background);
                selectedRow = null;
                selectedView = null;

                if (selectedTable >= 0) view.hideDbHelper();
            }
        }
    };

    private View.OnLongClickListener rowLongClickListener = view -> {
        if (view.getTag() instanceof Utils.Row) {
            final Utils.Row row = (Utils.Row) view.getTag();
            selectedRow = row;
            editRow();
            return true;
        }
        return false;
    };

    private void execSqlQuery(String query) {
        try {
            dbHelper.execSQL(query);
        } catch (Throwable throwable) {
            view.showError(Utils.parceSqlException(throwable));
        }
    }

    public void applyTextEdit(String query) {
        if (query.toLowerCase().startsWith("select")) {
            openTempTable(query);
        } else {
            execSqlQuery(query);
        }

        view.showTextEditContainer(false);
    }

    private void openTempTable(final String query) {

        view.hideTableSettings();
        processOpenTable("CUSTOM", () -> {
            try {
                final Cursor cursor = dbHelper.rawQuery(query);
                return cursor;
            } catch (final Throwable throwable) {
                view.runOnUiThread(() -> view.showError(Utils.parceSqlException(throwable)));
                return null;
            }
        });

        selectedTable = -1;

        view.showRange(0, -1, 0);
    }

    public boolean onBackPressed() {
        if (tables.length > 0 && view.errorIsVisible()) {
            view.hideError();
            return true;
        }
        return false;
    }

    public void onPrevPage() {
        if (selectedPage > 0) {
            selectedPage--;
            openTable(tables[selectedTable], selectedPage * limitRows);
            normalShowRange();
        }
    }

    public void onNextPage() {
        if (rowCounts[selectedTable] >= (selectedPage + 1) * limitRows) {
            selectedPage++;
            openTable(tables[selectedTable], selectedPage * limitRows);
            normalShowRange();
        }
    }

    public void clearAllRows() {
        new AlertDialog.Builder(context)
                .setTitle("Очистка таблицы")
                .setMessage("Вы действительно хотите удалить все строки из таблицы - \"" + tables[selectedTable] + "\"?")
                .setPositiveButton("Ok", (dialog, which) -> deleteAllRows(tables[selectedTable]))
                .create().show();
    }

    private void deleteAllRows(String table) {
        dbHelper.deleteAllRows(table);
        openTable(table, 0);
        view.showRange(0, 0, 0);
    }

    public void closeError() {
        view.hideError();
    }

    public void addRow() {
        view.openEditRowFragment(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId, true);
    }

    public void updateTable() {
        openTable(tables[selectedTable], selectedPage * limitRows);
    }

    public void editRow() {
        view.openEditRowFragment(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId, false);
    }

    public void dublicateRow() {
        dbHelper.dublicateRow(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId);
        updateTable();
    }

    public void deleteRow() {
        dbHelper.deleteRow(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId);
        updateTable();
    }
}
