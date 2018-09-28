package ru.mediasoft.example;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Random;

import ru.mediasoft.datamanager.DataManager;

public class MainActivity extends AppCompatActivity {

    private static final String DB_PATH = "database";

    final Random random = new Random();
    final String[] names = new String[] {"Arnold", "Mary", "Mike", "Stephany", "Mark", "Silvester", "Putin", "Donald", "Margaret", "Elizabet"};
    final String[] hints = new String[] {"Student", "Teacher", "President", "Actor", "Minister", "Workman", "Programmer", "Manager", "Director", "Tester"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView logsTxt = findViewById(R.id.logs);
        logsTxt.setText( "" );
    }

    public void runManager(View view) {
        DataManager.init(this);
    }

    public void stopManager(View view) {
        DataManager.destroy(this);
    }

    public void createTable(View view) {
        execSql("CREATE TABLE IF NOT EXISTS user ( id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(20), hint TEXT , age INTEGER)", "Не удалось создать таблицу");
    }

    public void fillTable(View view) {
        final StringBuilder builder = new StringBuilder("INSERT INTO user (name, hint, age) VALUES ").append(getRandomUser());
        for (int i = 0; i < 5; i++) {
            builder.append(",").append(getRandomUser());
        }
        execSql(builder.append(";").toString(), "Не удалось заполнить таблицу");
    }

    public void clearTable(View view) {
        execSql("DELETE FROM user;", "Не удалось очистить таблицу");
    }

    public void deleteTable(View view) {
        execSql("DROP TABLE user;", "Не удалось удалить таблицу");
    }

    private void execSql(String sqlQuery, String errorMessage) {
        try {
            SQLiteDatabase db = getApplicationContext().openOrCreateDatabase(DB_PATH, SQLiteDatabase.OPEN_READWRITE, null);
            db.execSQL(sqlQuery);
        } catch (Throwable throwable) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private String getRandomUser() {
        return String.format("('%s', '%s', %d)", names[random.nextInt(10)], hints[random.nextInt(10)], random.nextInt(70));
    }
}
