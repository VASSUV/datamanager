package ru.mediasoft.datamanager;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Utils {

    private static Drawable selectbaleDrawable;

    public static String pathToFullPath(Context context, String dbPath) {
        return !dbPath.contains("/") ? getDataBaseDirectoryPath(context) + dbPath : dbPath;
    }

    @NonNull
    public static String getDataBaseDirectoryPath(Context context) {
        return Build.VERSION.SDK_INT >= 17 ? context.getApplicationInfo().dataDir + "/databases/"  : "/data/data/" + context.getPackageName() + "/databases/";
    }

    public static TableRow[] reViewTable(ViewGroup tableLayout,
                                         Cursor cursor,
                                         View.OnClickListener rowClickListener,
                                         View.OnLongClickListener rowLongClickListener,
                                         int screenWidth) {
        if (cursor == null) return new TableRow[0];
        final TableLayout.LayoutParams lp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT);
        final List<String> names = new ArrayList<>(new HashSet<>(Arrays.asList(cursor.getColumnNames())));
        final int countColumns = names.size();
        final int countRows = cursor.getCount();
        final String rowIdName = cursor.getColumnNames()[0];
        final int[] indexies = new int[countColumns];
        TableRow[] rows = new TableRow[countRows + 1];

        rowIdMoveToFirst(cursor, names, rowIdName, indexies);

        rows[0] = getHeaderRow(names, tableLayout.getContext());
        rows[0].setLayoutParams(lp);
        for (int i = 0; i < countRows; i++) {
            cursor.moveToPosition(i);
            rows[i + 1] = getTableRow(tableLayout.getContext(), cursor, rowClickListener, rowLongClickListener, screenWidth, countColumns, indexies, i);
            rows[i + 1].setLayoutParams(lp);
        }
        return rows;
    }

    private static void rowIdMoveToFirst(Cursor cursor, List<String> names, String rowIdName, int[] indexies) {
        int i = 0;
        for (String name : names) {
            indexies[i] = cursor.getColumnIndex(name);
            if (name.equals(rowIdName)) {
                swap(indexies, i, 0);
                Collections.swap(names, i, 0);
            }
            i++;
        }
    }

    private static TableRow getHeaderRow(List<String> names, Context context) {
        TableRow row = new TableRow(context);
        ViewCompat.setZ(row, 1f);

        TableLayout.LayoutParams lp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT);
        row.setLayoutParams(lp);
        row.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        for (String name : names) {
            TextView button = new TextView(row.getContext());
            TableRow.LayoutParams lpb = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
            button.setPadding(32, 15, 32, 15);
            button.setLayoutParams(lpb);
            button.setText(name);
            button.setBackgroundResource(R.drawable.header_table_background);
            row.addView(button);
        }
        row.setClickable(true);
        row.setFocusable(true);
        return row;
    }

    @NonNull
    private static TableRow getTableRow(Context context,
                                        Cursor cursor,
                                        View.OnClickListener rowClickListener,
                                        View.OnLongClickListener rowLongClickListener,
                                        int screenWidth, int countColumns, int[] indexies, int i) {
        TableRow row = new TableRow(context);
        row.setGravity(Gravity.CENTER | Gravity.BOTTOM);

        final Row tableRow = new Row();
        tableRow.position = i;
        tableRow.rowId = cursor.getString(indexies[0]);

        row.setTag(tableRow);

        row.setOnClickListener(rowClickListener);
        row.setOnLongClickListener(rowLongClickListener);
        setSelectableBackground(row);

        for (int j = 0; j < countColumns; j++) {
            row.addView(getCell(cursor.getString(indexies[j]), screenWidth, row));
        }
        return row;
    }

    @NonNull
    private static TextView getCell(String cellText, int screenWidth, TableRow row) {
        TextView button = new TextView(row.getContext());
        button.setMaxWidth(screenWidth);
        button.setSingleLine(false);

        TableRow.LayoutParams lpb = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        lpb.leftMargin = 32;
        lpb.rightMargin = 32;
        button.setLayoutParams(lpb);
        button.setText(cellText);
        return button;
    }

    private static void setSelectableBackground(TableRow row) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            row.setBackgroundResource(R.drawable.selectable_backround_ripple);//getSelectableDrawable(row.getContext()));
//        }
    }

//    private static Drawable getSelectableDrawable(Context context) {
//        if (selectbaleDrawable != null) return selectbaleDrawable;
//
//        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
//        TypedArray typedArray = context.obtainStyledAttributes(attrs);
//        selectbaleDrawable = typedArray.getDrawable(0);
//        return selectbaleDrawable;
//    }

    private static void swap(int[] indexies, int positionA, int positionB) {
        int temp = indexies[positionA];
        indexies[positionA] = indexies[positionB];
        indexies[positionB] = temp;
    }

    public static String parceSqlException(Throwable sqlExeption) {
        Log.e("DataManager", "parceSqlException: ", sqlExeption);
        return sqlExeption.getMessage();
    }

    public static class Row {
        public int position;
        public String rowId;
    }

    public interface CallBack<T> {
        T call();
    }
}
