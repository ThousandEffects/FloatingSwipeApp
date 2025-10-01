package com.swipehelper.floatingswipe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class SwipeSimulatorService extends AccessibilityService {

    private static final String TAG = "SwipeSimulatorService";
    private BroadcastReceiver swipeCommandReceiver;
    
    // Coordenadas para el swipe (centro de la pantalla)
    private float screenCenterX;
    private float screenCenterY;
    
    // Distancia del swipe (pequeña para simular scroll de reels)
    private static final float SWIPE_DISTANCE = 200f;
    private static final long SWIPE_DURATION = 100; // ms

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Swipe Simulator Service Connected");
        
        // Obtener dimensiones de la pantalla
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenCenterX = displayMetrics.widthPixels / 2f;
        screenCenterY = displayMetrics.heightPixels / 2f;
        
        // Registrar el receiver para comandos de swipe
        registerSwipeCommandReceiver();
        
        Toast.makeText(this, "Swipe Simulator activado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (swipeCommandReceiver != null) {
            try {
                unregisterReceiver(swipeCommandReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Receiver not registered", e);
            }
        }
        Log.d(TAG, "Swipe Simulator Service Destroyed");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No necesitamos procesar eventos de accesibilidad para este caso de uso
        // Solo usamos este servicio para simular gestos
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Swipe Simulator Service Interrupted");
    }

    private void registerSwipeCommandReceiver() {
        swipeCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("SWIPE_COMMAND".equals(intent.getAction())) {
                    String direction = intent.getStringExtra("direction");
                    Log.d(TAG, "Received swipe command: " + direction);
                    
                    if ("up".equals(direction)) {
                        performSwipeUp();
                    } else if ("down".equals(direction)) {
                        performSwipeDown();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("SWIPE_COMMAND");
        registerReceiver(swipeCommandReceiver, filter);
    }

    private void performSwipeUp() {
        Log.d(TAG, "Performing swipe up");
        
        // Crear path para swipe hacia arriba
        Path swipePath = new Path();
        swipePath.moveTo(screenCenterX, screenCenterY + SWIPE_DISTANCE/2);
        swipePath.lineTo(screenCenterX, screenCenterY - SWIPE_DISTANCE/2);
        
        performGesture(swipePath);
    }

    private void performSwipeDown() {
        Log.d(TAG, "Performing swipe down");
        
        // Crear path para swipe hacia abajo
        Path swipePath = new Path();
        swipePath.moveTo(screenCenterX, screenCenterY - SWIPE_DISTANCE/2);
        swipePath.lineTo(screenCenterX, screenCenterY + SWIPE_DISTANCE/2);
        
        performGesture(swipePath);
    }

    private void performGesture(Path gesturePath) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            GestureDescription.StrokeDescription strokeDescription = 
                new GestureDescription.StrokeDescription(gesturePath, 0, SWIPE_DURATION);
            
            GestureDescription gestureDescription = new GestureDescription.Builder()
                .addStroke(strokeDescription)
                .build();

            boolean result = dispatchGesture(gestureDescription, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "Gesture completed successfully");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.w(TAG, "Gesture was cancelled");
                }
            }, null);

            if (!result) {
                Log.e(TAG, "Failed to dispatch gesture");
            }
        } else {
            Log.e(TAG, "Gesture simulation requires Android N (API 24) or higher");
            Toast.makeText(this, "Tu dispositivo no soporta simulación de gestos", 
                          Toast.LENGTH_SHORT).show();
        }
    }

    // Métodos para ajustar la sensibilidad y posición del swipe
    public void setSwipePosition(float x, float y) {
        screenCenterX = x;
        screenCenterY = y;
        Log.d(TAG, "Swipe position updated to: " + x + ", " + y);
    }

    // Método para swipe personalizado (si se necesita en el futuro)
    private void performCustomSwipe(float startX, float startY, float endX, float endY, long duration) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Path customPath = new Path();
            customPath.moveTo(startX, startY);
            customPath.lineTo(endX, endY);

            GestureDescription.StrokeDescription strokeDescription = 
                new GestureDescription.StrokeDescription(customPath, 0, duration);
            
            GestureDescription gestureDescription = new GestureDescription.Builder()
                .addStroke(strokeDescription)
                .build();

            boolean result = dispatchGesture(gestureDescription, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "Custom gesture completed");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.w(TAG, "Custom gesture cancelled");
                }
            }, null);

            if (!result) {
                Log.e(TAG, "Failed to dispatch custom gesture");
            }
        }
    }
}
