package ru.mediasoft.datamanager.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;

import ru.mediasoft.datamanager.DataManager;
import ru.mediasoft.datamanager.DbHelper;
import ru.mediasoft.datamanager.R;
import ru.mediasoft.datamanager.SharedData;
import ru.mediasoft.datamanager.Utils;

import static android.app.Activity.RESULT_OK;

class DataManagerPresenter {
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 999;
    public static final int CHOOSE_FILE_REQUEST_CODE = 123;
    private DataManagerView view;
    private Context context;
    private String dbPath;

    DbHelper dbHelper = new DbHelper();
    private String[] tables = new String[0];
    private int[] rowCounts = new int[0];
    private Dialog dialogTableList;
    private int limitRows = 200;
    private int selectedPage = 0;
    int selectedTable = -1;

    private AsyncTask<Void, Void, TableRow[]> asyncTask;

    private Utils.Row selectedRow = null;
    private View selectedView = null;
    private int measuredWidth;
    private SharedData sharedData;
    private Dialog fileListDialog;

    void onCreate(DataManagerView dataManagerView, Context context) {
        view = dataManagerView;
        this.context = context;

        sharedData = new SharedData(context);

        dbPath = sharedData.getDbPath();
        if (dbPath.isEmpty()) {
            fileListDialog = getFileListDialog();
            fileListDialog.show();
        } else {
            loadTables();
        }
    }

    void onStart(int measuredWidth) {
        this.measuredWidth = measuredWidth;
    }

    private void loadTables() {
        if (dbPath == null || dbPath.isEmpty()) {
            showError("Не передан путь до файла");
            return;
        }

        dbPath = Utils.pathToFullPath(context, dbPath);

        connectDbFile();
    }

    private void connectDbFile() {

        if (!isPermissionsGrantedOrRequest()) {
            return;
        }

        try {
            dbHelper.destroy();
            dbHelper.init(dbPath);
            sharedData.saveDbPath(dbPath);
        } catch (Throwable throwable) {
            showError("Не удалось подключиться к \n\n" + dbPath);
            throwable.printStackTrace();
            return;
        }

        view.hideProgress();

        updateTables();

        view.showRange(0, 0, 0);

        if (dialogTableList != null) {
            if (dialogTableList.isShowing()) {
                dialogTableList.dismiss();
            }
            dialogTableList = null;
        }

        showTables();

    }

    private boolean isPermissionsGrantedOrRequest() {
        if (ContextCompat.checkSelfPermission((Activity) view, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions((Activity) view, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                ActivityCompat.requestPermissions((Activity) view, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else return true;
        return false;
    }

    void updateTables() {
        tables = dbHelper.getTableNames();
        rowCounts = dbHelper.getRowCountsForEachTable(tables);
    }

    void showTables() {
        if (tables.length == 0) {
            showError("Отсутствуют таблицы в БД");
        } else {
            getTableListDialog().show();
        }
    }

    @NonNull
    private Dialog getFileListDialog() {
        if (fileListDialog != null) {
            if (fileListDialog.isShowing()){
                fileListDialog.dismiss();
            }
            fileListDialog = null;
        }
        fileListDialog = new Dialog(context);
        fileListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        fileListDialog.setContentView(R.layout.dialog_list);
        fileListDialog.setCancelable(true);
        final TextView selectExternalFileButton = fileListDialog.findViewById(R.id.select_file);
        ((TextView) fileListDialog.findViewById(R.id.title)).setText("Выберите файл Базы данных");
        selectExternalFileButton.setText("Выбрать файл из\nвнешней памяти");

        selectExternalFileButton.setOnClickListener(fileView -> {
            chooseExternalFileDb();
        });
        final ListView fileListView = fileListDialog.findViewById(R.id.list_view);
        File directory = new File(Utils.getDataBaseDirectoryPath(context));
        File[] files;

        if (!directory.exists() || !directory.isDirectory())  {
            chooseExternalFileDb();
            files = new File[0];
        } else {
            files = directory.listFiles();
        }

        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }
        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            final String path = files[position].getPath();
            if (checkDataBase(path)) {
                dbPath = path;
                dialogsDismiss();
                connectDbFile();
            }
        });
        final ArrayAdapter<String> fileAdapter = new ArrayAdapter<>(context, R.layout.simple_list_item_1, fileNames);
        fileListView.setAdapter(fileAdapter);
        fileListDialog.show();
        return fileListDialog;
    }

    private void chooseExternalFileDb() {
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);
        view.startActivityForResult(Intent.createChooser(intent, "Select a file"), CHOOSE_FILE_REQUEST_CODE);
    }

    @NonNull
    private Dialog getTableListDialog() {
        if (dialogTableList != null) return dialogTableList;

        dialogTableList = new Dialog(context);
        dialogTableList.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogTableList.setContentView(R.layout.dialog_list);
        dialogTableList.setCancelable(true);
        final ListView listView = dialogTableList.findViewById(R.id.list_view);
        final View selectFileButton = dialogTableList.findViewById(R.id.select_file);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.simple_list_item_1, tables);
        listView.setAdapter(adapter);

        fileListDialog = getFileListDialog();
        selectFileButton.setOnClickListener(v -> {
            fileListDialog.show();
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedTable = position;
            selectedPage = 0;
            this.view.resetScroll();
            openTable(tables[selectedTable], 0);
            dialogTableList.dismiss();
            normalShowRange();
        });
        dialogTableList.setOnDismissListener(dialogInterface -> {
            if (fileListDialog.isShowing()) {
                fileListDialog.dismiss();
            }
        });
        return dialogTableList;
    }

    private boolean checkDataBase(String dbPath) {
        SQLiteDatabase checkDB = null;
        try {
            checkDB = SQLiteDatabase.openDatabase(dbPath, null,
                    SQLiteDatabase.OPEN_READONLY);
            checkDB.close();
        } catch (SQLiteException e) {
            showError("Файл не является файлом SQLite DataBase");
        }

        if (dbPath.endsWith("-journal")) {
            showError("Вероятно, что выбранный файл временный, и нам не нужно его открывать");
            return false;
        }

        return checkDB != null;
    }

    private void showError(String текстОшибки) {
        dialogsDismiss();
        view.showError(текстОшибки);
    }

    private void dialogsDismiss() {
        if (dialogTableList != null)
            dialogTableList.dismiss();
        if (fileListDialog != null)
            fileListDialog.dismiss();
    }

    void normalShowRange() {
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
            selectedRow = (Utils.Row) view.getTag();
            editRow();
            return true;
        }
        return false;
    };

    private void execSqlQuery(String query) {
        try {
            dbHelper.execSQL(query);
        } catch (Throwable throwable) {
            showError(Utils.parceSqlException(throwable));
            throwable.printStackTrace();
        }
    }

    void applyTextEdit(String query) {
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
                return dbHelper.rawQuery(query);
            } catch (final Throwable throwable) {
                view.runOnUiThread(() -> view.showError(Utils.parceSqlException(throwable)));
                throwable.printStackTrace();
                return null;
            }
        });

        selectedTable = -1;

        view.showRange(0, -1, 0);
    }

    boolean onBackPressed() {
        if (tables.length > 0 && view.errorIsVisible()) {
            view.hideError();
            return true;
        }
        return false;
    }

    void onPrevPage() {
        if (selectedTable != -1 && selectedPage > 0) {
            selectedPage--;
            openTable(tables[selectedTable], selectedPage * limitRows);
            normalShowRange();
        }
    }

    void onNextPage() {
        if (selectedTable != -1 && rowCounts[selectedTable] >= (selectedPage + 1) * limitRows) {
            selectedPage++;
            openTable(tables[selectedTable], selectedPage * limitRows);
            normalShowRange();
        }
    }

    void clearAllRows() {
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

    void closeError() {
        if (dbHelper.isInit()) {
            view.hideError();
        } else {
            view.backToApp();
        }
    }

    void addRow() {
        view.openEditRowFragment(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId, true);
        view.hideDbHelper();
    }

    void updateTable() {
        openTable(tables[selectedTable], selectedPage * limitRows);
    }

    void editRow() {
        view.openEditRowFragment(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId, false);
        view.hideDbHelper();
    }

    void dublicateRow() {
        dbHelper.dublicateRow(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId);
        updateTable();
        view.hideDbHelper();
        updateTables();
        normalShowRange();
    }

    void deleteRow() {
        dbHelper.deleteRow(tables[selectedTable], selectedRow == null ? null : selectedRow.rowId);
        updateTable();
        view.hideDbHelper();
        updateTables();
        normalShowRange();
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData();

            if (selectedfile != null) {
                dbPath = selectedfile.getPath();
//                dbPath = getPath(context, selectedfile);

                File file = new File(dbPath);
                if (!file.canWrite()) {
                    showError("Не удалось подключиться к файлу - \n\n" + dbPath + "\n\nФайл защищен от записи\nПопробуйте переместить файл в другое место или воспользуйтесь другим файловым менеджером");
                } else {
                    connectDbFile();
                } // /document/raw:/storage/emulated/0/Download/domru.db
            } // /storage/emulated/0/Download/domru.db
        } // /document/3037-6431:domru.db
    } // /storage/3037-6431/domru.db

    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                connectDbFile();
            }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    return "/storage/" + split[0] + "/" + split[1];
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
