package ru.mediasoft.datamanager;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedData {
    static String NAME = "DataManagerSharedPreferences";
    static String FLOATING_X = "FLOATING_X";
    static String FLOATING_Y = "FLOATING_Y";
    static String DB_PATH = "DB_PATH";

    SharedPreferences sharedPreferences;

    public SharedData(Context applicationContext) {
        sharedPreferences = applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void saveFloatingButtonPosition(int x, int y) {
        sharedPreferences.edit().putInt(FLOATING_X, x).apply();
        sharedPreferences.edit().putInt(FLOATING_Y, y).apply();
    }

    public int getFloatingButtonX() {
        return sharedPreferences.getInt(FLOATING_X, -1);
    }

    public int getFloatingButtonY() {
        return sharedPreferences.getInt(FLOATING_Y, -1);
    }

    public void saveDbPath(String dbPath) {
        sharedPreferences.edit().putString(DB_PATH, dbPath).apply();

    }
    public String getDbPath() {
        return sharedPreferences.getString(DB_PATH, "");
    }
}
