package ru.mediasoft.datamanager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import ru.mediasoft.datamanager.activity.DataManagerActivity;

public class FloatingDBButtonService extends Service {
    //менеджер к которому цепляем кнопку что бы все время быть на верху
    private WindowManager windowManager;
    private ImageView iconView;
    private WindowManager.LayoutParams params;

    SharedData sharedData;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedData = new SharedData(getApplicationContext());
        requestOverlayPermission(this);

        if (isPermissionGranted(this))
            showButton();
    }

    public static void requestOverlayPermission(Context context) {
        if (!isPermissionGranted(context)) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
                return;
            }

            Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            myIntent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(myIntent);
        }
    }

    public static boolean isPermissionGranted(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context.getApplicationContext());
    }

    private void showButton() {
        //инициализируем его
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        //создаем нашу кнопку что бы отобразить
        iconView = new ImageView(this);
        iconView.setImageResource(R.mipmap.db_launcher);

        //задаем параметры для картинки, что бы была
        //своего размера, что бы можно было перемещать по экрану
        //что бы была прозрачной, и устанавливается ее стартовое полодение
        //на экране при создании
        params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_PHONE,
                 LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        params.x = sharedData.getFloatingButtonX();
        params.y = sharedData.getFloatingButtonY();
        if(params.x == -1 && params.x == params.y ) {
            params.x = 0;
            params.y = 0;
        }

        //кол перемещения тоста по экрану при помощи touch
        iconView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean shouldClick;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        shouldClick = true;
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (shouldClick) {
//                            Toast.makeText(getApplicationContext(), "Клик по тосту случился!", Toast.LENGTH_LONG).show();
                            final Intent intent = new Intent(getApplicationContext(), DataManagerActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        shouldClick = abs(event.getRawX() - initialTouchX) < 20 && abs(event.getRawY() - initialTouchY) < 20;
                        params.x = initialX
                                + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY
                                + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(iconView, params);
                        sharedData.saveFloatingButtonPosition(params.x, params.y);
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(iconView, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    private float abs(float value) {
        return value > 0 ? value : -value;
    }

    //удалем тост если была команда выключить сервис
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (iconView != null)
            windowManager.removeView(iconView);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
