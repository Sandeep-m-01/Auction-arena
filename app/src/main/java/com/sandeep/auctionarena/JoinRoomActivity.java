package com.sandeep.auctionarena;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class JoinRoomActivity extends AppCompatActivity {

    private EditText etRoomCode;
    private EditText etPlayerName;
    private LinearLayout btnJoin;

    private MediaPlayer clickSound;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private static final String DATABASE_URL =
            "https://auctionarena-c777d-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final long DEFAULT_BUDGET = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_join_room);

        // Connect XML
        etRoomCode =
                findViewById(R.id.etRoomCode);

        etPlayerName =
                findViewById(R.id.etPlayerName);

        btnJoin =
                findViewById(R.id.btnJoin);

        // Same Firebase URL
        firebaseDatabase =
                FirebaseDatabase.getInstance(DATABASE_URL);

        databaseReference =
                firebaseDatabase.getReference();

        // Sound
        clickSound =
                MediaPlayer.create(this, R.raw.click);

        btnJoin.setOnClickListener(v -> {

            playClickSound();

            String roomCode = etRoomCode
                    .getText()
                    .toString()
                    .trim()
                    .toUpperCase(java.util.Locale.ROOT);

            String playerName =
                    etPlayerName
                            .getText()
                            .toString()
                            .trim();

            if (roomCode.isEmpty()) {

                etRoomCode.setError(
                        "Enter Room Code"
                );

                etRoomCode.requestFocus();

                return;
            }

            if (playerName.isEmpty()) {

                etPlayerName.setError(
                        "Enter Player Name"
                );

                etPlayerName.requestFocus();

                return;
            }


            checkAndJoinRoom(
                    roomCode,
                    playerName
            );
        });
    }

    private void checkAndJoinRoom(
            String roomCode,
            String playerName
    ) {

        DatabaseReference roomRef =
                databaseReference
                        .child("rooms")
                        .child(roomCode);

        roomRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {

                            etRoomCode.setError(
                                    "Room not found"
                            );

                            etRoomCode.requestFocus();

                            return;
                        }

                        String status =
                                snapshot
                                        .child("status")
                                        .getValue(String.class);

                        if (status != null
                                && !status.equals("waiting")) {


                            return;
                        }

                        String roomName =
                                snapshot
                                        .child("roomName")
                                        .getValue(String.class);

                        if (roomName == null
                                || roomName.isEmpty()) {

                            roomName =
                                    "Auction Arena";
                        }

                        addPlayerToRoom(
                                roomRef,
                                roomCode,
                                roomName,
                                playerName
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                    }
                }
        );
    }

    private void addPlayerToRoom(
            DatabaseReference roomRef,
            String roomCode,
            String roomName,
            String playerName
    ) {

        String playerId =
                roomRef
                        .child("players")
                        .push()
                        .getKey();

        if (playerId == null) {


            return;
        }

        Map<String, Object> playerData =
                new HashMap<>();

        playerData.put(
                "name",
                playerName
        );

        playerData.put(
                "isHost",
                false
        );

        playerData.put(
                "budget",
                DEFAULT_BUDGET
        );

        roomRef
                .child("players")
                .child(playerId)
                .setValue(playerData)
                .addOnSuccessListener(unused -> {



                    Intent intent =
                            new Intent(
                                    JoinRoomActivity.this,
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
                            playerName
                    );

                    intent.putExtra(
                            "PLAYER_ID",
                            playerId
                    );

                    intent.putExtra(
                            "IS_HOST",
                            false
                    );

                    startActivity(intent);

                })
                .addOnFailureListener(e -> {



                });
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