package com.sandeep.auctionarena;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // ==================================================
    // UI
    // ==================================================

    private LinearLayout btnCreateRoom;
    private LinearLayout btnJoinRoom;


    // ==================================================
    // SOUNDS
    // ==================================================

    private MediaPlayer clickSound;
    private MediaPlayer introSound;


    // ==================================================
    // ON CREATE
    // ==================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        // Full screen layout
        Window window = getWindow();

        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );


        // Load main screen
        setContentView(R.layout.activity_main);


        // ==================================================
        // CONNECT UI
        // ==================================================

        btnCreateRoom =
                findViewById(
                        R.id.btnCreateRoom
                );

        btnJoinRoom =
                findViewById(
                        R.id.btnJoinRoom
                );


        // ==================================================
        // LOAD CLICK SOUND
        // ==================================================

        clickSound =
                MediaPlayer.create(
                        this,
                        R.raw.click
                );


        // ==================================================
        // PLAY INTRO SOUND
        // ==================================================

        introSound =
                MediaPlayer.create(
                        this,
                        R.raw.intro
                );


        if (introSound != null) {

            // Intro plays only once
            introSound.setLooping(false);

            // Volume
            introSound.setVolume(
                    1.0f,
                    1.0f
            );

            // Start intro
            introSound.start();


            // Release MediaPlayer after intro finishes
            introSound.setOnCompletionListener(mp -> {

                mp.release();

                introSound = null;

            });

        }


        // ==================================================
        // CREATE ROOM
        // ==================================================

        btnCreateRoom.setOnClickListener(v -> {

            playClickSound();


            Intent intent =
                    new Intent(
                            MainActivity.this,
                            CreateRoomActivity.class
                    );

            startActivity(intent);

        });


        // ==================================================
        // JOIN ROOM
        // ==================================================

        btnJoinRoom.setOnClickListener(v -> {

            playClickSound();


            Intent intent =
                    new Intent(
                            MainActivity.this,
                            JoinRoomActivity.class
                    );

            startActivity(intent);

        });

    }


    // ==================================================
    // PLAY CLICK SOUND
    // ==================================================

    private void playClickSound() {

        if (clickSound == null) {

            return;

        }


        /*
         * If user clicks quickly multiple times,
         * restart click sound from beginning.
         */

        if (clickSound.isPlaying()) {

            clickSound.seekTo(0);

        }


        clickSound.start();

    }


    // ==================================================
    // ON DESTROY
    // ==================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();


        // Release click sound
        if (clickSound != null) {

            clickSound.release();

            clickSound = null;

        }


        // Release intro sound
        if (introSound != null) {

            introSound.release();

            introSound = null;

        }

    }

}