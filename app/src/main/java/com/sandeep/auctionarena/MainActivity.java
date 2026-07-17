package com.sandeep.auctionarena;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private LinearLayout btnCreateRoom, btnJoinRoom;
    private MediaPlayer clickSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_main);

        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);

        clickSound = MediaPlayer.create(this, R.raw.click);

        btnCreateRoom.setOnClickListener(v -> {

            if (clickSound != null)
                clickSound.start();

            Toast.makeText(this, "Create Room", Toast.LENGTH_SHORT).show();

        });

        btnJoinRoom.setOnClickListener(v -> {

            if (clickSound != null)
                clickSound.start();

            Toast.makeText(this, "Join Room", Toast.LENGTH_SHORT).show();

        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
    }
}