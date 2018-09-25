package ru.mediasoft.datamanager;

import android.content.Context;
import android.content.SharedPreferences;

class SharedData {
    static String NAME = "DataManagerSharedPreferences";
    static String FLOATING_X = "FLOATING_X";
    static String FLOATING_Y = "FLOATING_Y";

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
}
