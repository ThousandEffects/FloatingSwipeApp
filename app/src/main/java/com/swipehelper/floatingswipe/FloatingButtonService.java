package com.swipehelper.floatingswipe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.core.app.NotificationCompat;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private ImageButton btnSwipeUp, btnSwipeDown;
    
    private static final String CHANNEL_ID = "FloatingButtonService";
    private static final int NOTIFICATION_ID = 1;
    
    // Posición de los botones
    private WindowManager.LayoutParams params;
    private boolean isDragging = false;
    private float lastX, lastY;
    private float initialX, initialY;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createFloatingView() {
        // Crear el layout flotante
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_buttons, null);
        
        // Configurar parámetros de la ventana
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // Posicionar en la esquina inferior derecha
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = getScreenWidth() - 200; // Cerca del borde derecho
        params.y = getScreenHeight() - 300; // Cerca del borde inferior

        // Encontrar los botones
        btnSwipeUp = floatingView.findViewById(R.id.btnSwipeUp);
        btnSwipeDown = floatingView.findViewById(R.id.btnSwipeDown);

        // Configurar listeners
        setupButtonListeners();
        setupTouchListener();

        // Agregar la vista flotante
        windowManager.addView(floatingView, params);
    }

    private void setupButtonListeners() {
        btnSwipeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Enviar comando de swipe up al servicio de accesibilidad
                Intent intent = new Intent("SWIPE_COMMAND");
                intent.putExtra("direction", "up");
                sendBroadcast(intent);
                
                // Feedback visual
                v.setAlpha(0.5f);
                v.animate().alpha(1.0f).setDuration(100);
            }
        });

        btnSwipeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Enviar comando de swipe down al servicio de accesibilidad
                Intent intent = new Intent("SWIPE_COMMAND");
                intent.putExtra("direction", "down");
                sendBroadcast(intent);
                
                // Feedback visual
                v.setAlpha(0.5f);
                v.animate().alpha(1.0f).setDuration(100);
            }
        });
    }

    private void setupTouchListener() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isDragging = false;
                        initialX = params.x;
                        initialY = params.y;
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - lastX;
                        float deltaY = event.getRawY() - lastY;
                        
                        // Si se mueve más de un umbral, considerarlo como dragging
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                        }
                        
                        if (isDragging) {
                            params.x = (int) (initialX + (event.getRawX() - lastX));
                            params.y = (int) (initialY + (event.getRawY() - lastY));
                            
                            // Mantener dentro de los límites de la pantalla
                            params.x = Math.max(0, Math.min(params.x, getScreenWidth() - floatingView.getWidth()));
                            params.y = Math.max(0, Math.min(params.y, getScreenHeight() - floatingView.getHeight()));
                            
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (isDragging) {
                            // Opcional: hacer que se pegue a los bordes
                            snapToEdge();
                            isDragging = false;
                            return true;
                        }
                        return false;
                }
                return false;
            }
        });
    }

    private void snapToEdge() {
        // Mover el botón al borde más cercano (izquierdo o derecho)
        int screenWidth = getScreenWidth();
        int middle = screenWidth / 2;
        
        if (params.x < middle) {
            // Mover al borde izquierdo
            params.x = 0;
        } else {
            // Mover al borde derecho
            params.x = screenWidth - floatingView.getWidth();
        }
        
        windowManager.updateViewLayout(floatingView, params);
    }

    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Floating Button Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Swipe Helper")
                .setContentText("Botones flotantes activos")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
