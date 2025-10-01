package com.swipehelper.floatingswipe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnGrantOverlayPermission;
    private Button btnGrantAccessibilityPermission;
    private Button btnToggleService;
    private TextView tvServiceStatus;
    
    private boolean isServiceRunning = false;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        updateUI();
    }

    private void initViews() {
        btnGrantOverlayPermission = findViewById(R.id.btnGrantOverlayPermission);
        btnGrantAccessibilityPermission = findViewById(R.id.btnGrantAccessibilityPermission);
        btnToggleService = findViewById(R.id.btnToggleService);
        tvServiceStatus = findViewById(R.id.tvServiceStatus);
    }

    private void setupClickListeners() {
        btnGrantOverlayPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOverlayPermission();
            }
        });

        btnGrantAccessibilityPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAccessibilityPermission();
            }
        });

        btnToggleService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFloatingButtonService();
            }
        });
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Permiso de overlay ya concedido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Por favor, activa 'Swipe Simulator' en Servicios de Accesibilidad", 
                      Toast.LENGTH_LONG).show();
    }

    private void toggleFloatingButtonService() {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Primero concede el permiso de overlay", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Primero activa el servicio de accesibilidad", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isServiceRunning) {
            stopFloatingButtonService();
        } else {
            startFloatingButtonService();
        }
    }

    private void startFloatingButtonService() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        isServiceRunning = true;
        updateUI();
        Toast.makeText(this, "Botones flotantes activados", Toast.LENGTH_SHORT).show();
    }

    private void stopFloatingButtonService() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
        updateUI();
        Toast.makeText(this, "Botones flotantes desactivados", Toast.LENGTH_SHORT).show();
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + SwipeSimulatorService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // Setting not found
        }

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                return settingValue.contains(service);
            }
        }

        return false;
    }

    private void updateUI() {
        // Actualizar estado de permisos
        btnGrantOverlayPermission.setEnabled(!hasOverlayPermission());
        btnGrantAccessibilityPermission.setEnabled(!isAccessibilityServiceEnabled());
        
        // Actualizar botón del servicio
        if (isServiceRunning) {
            btnToggleService.setText(R.string.disable_service);
            tvServiceStatus.setText(R.string.service_running);
        } else {
            btnToggleService.setText(R.string.enable_service);
            tvServiceStatus.setText(R.string.service_stopped);
        }
        
        // Habilitar botón solo si ambos permisos están concedidos
        boolean canToggleService = hasOverlayPermission() && isAccessibilityServiceEnabled();
        btnToggleService.setEnabled(canToggleService);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (hasOverlayPermission()) {
                Toast.makeText(this, "Permiso de overlay concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de overlay denegado", Toast.LENGTH_SHORT).show();
            }
            updateUI();
        }
    }
}
