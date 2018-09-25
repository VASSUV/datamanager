package ru.mediasoft.datamanager;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;


public class DbHelper {
    private SQLiteDatabase db;

    public void init(String dbPath) {
        db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public String[] getTableNames() {
        final Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        final String[] tables = new String[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            tables[i++] = cursor.getString(0);
        }
        cursor.close();
        return tables;
    }

    public int[] getRowCountsForEachTable(String[] tables) {
        Cursor cursor;
        final int[] rowCounts = new int[tables.length];
        for (int i = 0; i < rowCounts.length; i++) {
            cursor = db.rawQuery("SELECT count(*) FROM " + tables[i], null);
            if (cursor.moveToNext()) {
                rowCounts[i] = cursor.getInt(0);
            }
            cursor.close();
        }
        return rowCounts;
    }

    public Cursor getTableCursor(String table, int limitRows, int offset) {
        return db.rawQuery("SELECT rowid, * FROM " + table + " LIMIT " + limitRows + " OFFSET " + offset, null);
    }

    public void execSQL(String query) throws Throwable {
        db.execSQL(query);
    }

    public Cursor rawQuery(String query) throws Throwable {
        return db.rawQuery(query, null);
    }

    public void deleteAllRows(String table) {
        db.delete(table, null, null);
    }

    public ArrayList<String> getRow(String tableName, String rowId, ArrayList<Column> columns) {
        final ArrayList<String> list = new ArrayList<>();
        StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append("SELECT ");

        for (int i = 0; i < columns.size(); i++) {
            sqlQuery.append(columns.get(i).name);
            if (i != columns.size() - 1) {
                sqlQuery.append(",");
            }
        }
        final Column primaryKeyForTable = getPrimaryKeyForTable(tableName);
        final String columnNameRowId = primaryKeyForTable == null || primaryKeyForTable.name == null ? "rowid" : primaryKeyForTable.name;
        sqlQuery.append(" FROM ").append(tableName).append(" WHERE ").append(columnNameRowId).append("=").append(rowId);

        final Cursor cursor = db.rawQuery(sqlQuery.toString(), null);

        if (cursor.moveToFirst()) {
            for (int i = 0; i < columns.size(); i++) {
                list.add(cursor.getString(i));
            }
        } else {
            for (int i = 0; i < columns.size(); i++) {
                list.add(null);
            }
        }
        cursor.close();
        return list;
    }

    public ArrayList<Column> getColumns(String tableName) {
        final Cursor cursor = db.rawQuery("SELECT `name`, `type`, `notnull`, `dflt_value`, `pk`  from pragma_table_info('" + tableName + "')", null);
        final ArrayList<Column> list = new ArrayList<Column>();
        while (cursor.moveToNext()) {
            final Column column = new Column(cursor.getString(0), cursor.getString(1), cursor.getInt(2) == 1, cursor.getString(3), cursor.getInt(4) == 1);
            if (column.isPrimaryKey)
                list.add(0, column);
            else
                list.add(column);
        }
        if (!list.get(0).isPrimaryKey) {
            final Column column = new Column("rowid", "INTEGER", true, "", true);
            list.add(0, column);
        }
        cursor.close();
        return list;
    }


    public Column getPrimaryKeyForTable(String tableName) {
        final Cursor cursor = db.rawQuery("SELECT `name`, `type`, `notnull`, `dflt_value`, `pk` FROM pragma_table_info('" + tableName + "') WHERE pk=1", null);
        final Column column = cursor.moveToFirst() ? (new Column(cursor.getString(0), cursor.getString(1), cursor.getInt(2) == 1, cursor.getString(3), cursor.getInt(4) == 1)) : null;
        cursor.close();
        return column;
    }

    public void addRow(String tableName, ArrayList<Column> columns, ArrayList<String> values) {
        final StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append("INSERT INTO ").append(tableName).append(" (");
        for (int i = 1; i < columns.size(); i++) {
            if (i != 1)
                sqlQuery.append(",");
            sqlQuery.append(columns.get(i).name);
        }
        sqlQuery.append(") VALUES (");
        for (int i = 1; i < values.size(); i++) {
            if (i != 1)
                sqlQuery.append(",");
            sqlQuery.append(getValue(values.get(i), columns.get(i)));
        }
        sqlQuery.append(");");
        db.execSQL(sqlQuery.toString());
    }

    private String getValue(String value, Column column) {
        return (column.isNotNull && value == null) ? column.defaultValue
                : (value == null ? "NULL"
                : (column.type.startsWith("TEXT") || column.type.startsWith("VARCHAR")) ? ("'" + value + "'")
                : value);
    }

    public void updateRow(String tableName, ArrayList<Column> columns, ArrayList<String> values) {
        final StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append("UPDATE OR REPLACE ").append(tableName).append(" SET ");
        for (int i = 1; i < columns.size(); i++) {
            if (i != 1)
                sqlQuery.append(",");
            sqlQuery.append(columns.get(i).name).append("=").append(getValue(values.get(i), columns.get(i)));
        }
        sqlQuery.append(" WHERE ").append(columns.get(0).name).append("=").append(getValue(values.get(0), columns.get(0)));
        db.execSQL(sqlQuery.toString());
    }

    public void dublicateRow(String table, String rowId) {
        addRow(table, getColumns(table), getRow(table, rowId, getColumns(table)));
    }

    public void deleteRow(String table, String rowId) {
        final StringBuilder sqlQuery = new StringBuilder();
        final Column column = getPrimaryKeyForTable(table);
        sqlQuery.append("DELETE FROM ").append(table).append(" WHERE ").append(column.name).append("=").append(rowId);
        db.execSQL(sqlQuery.toString());
    }
}
