package com.sandeep.auctionarena;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CreateRoomActivity extends AppCompatActivity {

    private EditText etRoomName;
    private EditText etHostName;
    private LinearLayout btnCreate;

    private MediaPlayer clickSound;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private static final String DATABASE_URL =
            "https://auctionarena-c777d-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final long DEFAULT_BUDGET = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_create_room);

        // Connect XML
        etRoomName = findViewById(R.id.etRoomName);
        etHostName = findViewById(R.id.etHostName);
        btnCreate = findViewById(R.id.btnCreate);

        // Default room name
        if (etRoomName.getText().toString().trim().isEmpty()) {
            etRoomName.setText("Auction Arena");
        }

        // Firebase - IMPORTANT: same URL everywhere
        firebaseDatabase =
                FirebaseDatabase.getInstance(DATABASE_URL);

        databaseReference =
                firebaseDatabase.getReference();

        // Click sound
        clickSound =
                MediaPlayer.create(this, R.raw.click);

        btnCreate.setOnClickListener(v -> {

            playClickSound();

            String roomName =
                    etRoomName.getText()
                            .toString()
                            .trim();

            String hostName =
                    etHostName.getText()
                            .toString()
                            .trim();

            // Default room name
            if (roomName.isEmpty()) {
                roomName = "Auction Arena";
            }

            // Validate host
            if (hostName.isEmpty()) {

                etHostName.setError("Enter Host Name");

                etHostName.requestFocus();

                return;
            }

            String roomCode =
                    generateRoomCode();



            createRoom(
                    roomCode,
                    roomName,
                    hostName
            );
        });
    }

    private void createRoom(
            String roomCode,
            String roomName,
            String hostName
    ) {

        DatabaseReference roomRef =
                databaseReference
                        .child("rooms")
                        .child(roomCode);

        // Generate unique host player ID
        String hostPlayerId =
                roomRef
                        .child("players")
                        .push()
                        .getKey();

        if (hostPlayerId == null) {
            return;
        }

        // Room information
        Map<String, Object> roomData =
                new HashMap<>();

        roomData.put(
                "roomName",
                roomName
        );

        roomData.put(
                "hostName",
                hostName
        );

        roomData.put(
                "hostPlayerId",
                hostPlayerId
        );

        roomData.put(
                "status",
                "waiting"
        );

        // Host information
        Map<String, Object> hostData =
                new HashMap<>();

        hostData.put(
                "name",
                hostName
        );

        hostData.put(
                "isHost",
                true
        );

        hostData.put(
                "budget",
                DEFAULT_BUDGET
        );

        // First create room
        roomRef
                .setValue(roomData)
                .addOnSuccessListener(unused -> {

                    // Then add host
                    roomRef
                            .child("players")
                            .child(hostPlayerId)
                            .setValue(hostData)
                            .addOnSuccessListener(unused2 -> {

                                // Open Lobby
                                Intent intent =
                                        new Intent(
                                                CreateRoomActivity.this,
                                                WaitingLobbyActivity.class
                                        );

                                intent.putExtra(
                                        "ROOM_NAME",
                                        roomName
                                );

                                intent.putExtra(
                                        "ROOM_CODE",
                                        roomCode
                                );

                                intent.putExtra(
                                        "PLAYER_NAME",
                                        hostName
                                );

                                intent.putExtra(
                                        "PLAYER_ID",
                                        hostPlayerId
                                );

                                intent.putExtra(
                                        "IS_HOST",
                                        true
                                );

                                startActivity(intent);

                            })
                            .addOnFailureListener(e -> {
                                return;
                            });

                })
                .addOnFailureListener(e -> {

                });
    }

    private String generateRoomCode() {

        String characters =
                "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        Random random =
                new Random();

        StringBuilder code =
                new StringBuilder();

        for (int i = 0; i < 6; i++) {

            int index =
                    random.nextInt(
                            characters.length()
                    );

            code.append(
                    characters.charAt(index)
            );
        }

        return code.toString();
    }

    private void playClickSound() {

        if (clickSound != null) {

            try {

                clickSound.seekTo(0);
                clickSound.start();

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
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