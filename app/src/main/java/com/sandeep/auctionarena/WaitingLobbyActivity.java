package com.sandeep.auctionarena;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaitingLobbyActivity extends AppCompatActivity {

    // ==================================================
    // CONFIG
    // ==================================================

    private static final String DATABASE_URL =
            "https://auctionarena-c777d-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Money stored in millions
    // 1000 = $1000M
    private static final long STARTING_BUDGET = 1000L;


    // ==================================================
    // UI
    // ==================================================

    private TextView txtRoomCode;

    private LinearLayout playersContainer;
    private LinearLayout btnShareRoom;
    private LinearLayout btnStartAuction;


    // ==================================================
    // ROOM / PLAYER
    // ==================================================

    private String roomCode;
    private String playerId;
    private String playerName;

    private boolean isHost;


    // ==================================================
    // FIREBASE
    // ==================================================

    private DatabaseReference roomRef;

    private ValueEventListener playersListener;
    private ValueEventListener statusListener;


    // ==================================================
    // LOCAL STATE
    // ==================================================

    private boolean auctionOpened = false;


    // ==================================================
    // ON CREATE
    // ==================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        // ==================================================
        // FULL SCREEN
        // ==================================================

        Window window = getWindow();

        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );


        // ==================================================
        // LOAD LAYOUT
        // ==================================================

        setContentView(
                R.layout.activity_waiting_lobby
        );


        // ==================================================
        // CONNECT XML
        // ==================================================

        txtRoomCode =
                findViewById(
                        R.id.txtRoomCode
                );


        playersContainer =
                findViewById(
                        R.id.playersContainer
                );


        btnShareRoom =
                findViewById(
                        R.id.btnShareRoom
                );


        btnStartAuction =
                findViewById(
                        R.id.btnStartAuction
                );


        // ==================================================
        // GET INTENT DATA
        // ==================================================

        Intent intent =
                getIntent();


        roomCode =
                intent.getStringExtra(
                        "ROOM_CODE"
                );


        playerId =
                intent.getStringExtra(
                        "PLAYER_ID"
                );


        // NEW:
        // Get player name from previous screen

        playerName =
                intent.getStringExtra(
                        "PLAYER_NAME"
                );


        isHost =
                intent.getBooleanExtra(
                        "IS_HOST",
                        false
                );


        // ==================================================
        // VALIDATE ROOM
        // ==================================================

        if (roomCode == null ||
                roomCode.trim().isEmpty()) {

            finish();

            return;
        }


        // ==================================================
        // SHOW ROOM CODE
        // ==================================================

        txtRoomCode.setText(
                roomCode.toUpperCase()
        );


        // ==================================================
        // FIREBASE
        // ==================================================

        roomRef =
                FirebaseDatabase
                        .getInstance(
                                DATABASE_URL
                        )
                        .getReference()
                        .child("rooms")
                        .child(roomCode);


        // ==================================================
        // START BUTTON
        // ==================================================

        // Hidden until at least 2 players join
        // Only host will see it

        btnStartAuction.setVisibility(
                View.GONE
        );


        // ==================================================
        // START FIREBASE LISTENERS
        // ==================================================

        listenForPlayers();

        listenForAuctionStatus();


        // ==================================================
        // SHARE ROOM
        // ==================================================

        btnShareRoom.setOnClickListener(
                v -> shareRoom()
        );


        // ==================================================
        // START AUCTION
        // ==================================================

        btnStartAuction.setOnClickListener(v -> {

            if (!isHost) {

                return;
            }


            startAuction();

        });
    }


    // ==================================================
    // LISTEN FOR PLAYERS
    // ==================================================

    private void listenForPlayers() {

        if (roomRef == null) {

            return;
        }


        playersListener =
                roomRef
                        .child("players")
                        .addValueEventListener(

                                new ValueEventListener() {

                                    @Override
                                    public void onDataChange(
                                            @NonNull DataSnapshot snapshot
                                    ) {

                                        // Remove old cards

                                        playersContainer
                                                .removeAllViews();


                                        int playerCount =
                                                0;


                                        // ==================================================
                                        // READ PLAYERS
                                        // ==================================================

                                        for (DataSnapshot playerSnapshot :
                                                snapshot.getChildren()) {


                                            String name =
                                                    playerSnapshot
                                                            .child("name")
                                                            .getValue(
                                                                    String.class
                                                            );


                                            Boolean hostValue =
                                                    playerSnapshot
                                                            .child("isHost")
                                                            .getValue(
                                                                    Boolean.class
                                                            );


                                            Long budgetValue =
                                                    playerSnapshot
                                                            .child("budget")
                                                            .getValue(
                                                                    Long.class
                                                            );


                                            // Invalid player

                                            if (name == null ||
                                                    name.trim().isEmpty()) {

                                                continue;
                                            }


                                            // ==================================================
                                            // HOST STATUS
                                            // ==================================================

                                            boolean playerIsHost =

                                                    hostValue != null

                                                            &&

                                                            hostValue;


                                            // ==================================================
                                            // BUDGET
                                            // ==================================================

                                            long budget =

                                                    budgetValue != null

                                                            ? budgetValue

                                                            : STARTING_BUDGET;


                                            // ==================================================
                                            // ADD PLAYER CARD
                                            // ==================================================

                                            addPlayerToUI(
                                                    name,
                                                    playerIsHost,
                                                    budget
                                            );


                                            playerCount++;
                                        }


                                        // ==================================================
                                        // START BUTTON LOGIC
                                        // ==================================================

                                        if (isHost &&
                                                playerCount >= 2) {

                                            btnStartAuction
                                                    .setVisibility(
                                                            View.VISIBLE
                                                    );

                                        } else {

                                            btnStartAuction
                                                    .setVisibility(
                                                            View.GONE
                                                    );
                                        }
                                    }


                                    @Override
                                    public void onCancelled(
                                            @NonNull DatabaseError error
                                    ) {

                                        Log.e(
                                                "WaitingLobby",
                                                "Players listener error: "
                                                        + error.getMessage()
                                        );
                                    }
                                }
                        );
    }


    // ==================================================
    // ADD PLAYER CARD
    // ==================================================

    private void addPlayerToUI(
            String name,
            boolean playerIsHost,
            long budget
    ) {

        TextView playerView =
                new TextView(this);


        // ==================================================
        // BEBAS NEUE FONT
        // ==================================================

        playerView.setTypeface(

                ResourcesCompat.getFont(
                        this,
                        R.font.bebas_neue
                )

        );


        // ==================================================
        // PLAYER TEXT
        // ==================================================

        String displayText;


        if (playerIsHost) {

            displayText =
                    name.toUpperCase()
                            + "  •  HOST"
                            + "     $"
                            + budget
                            + "M";

        } else {

            displayText =
                    name.toUpperCase()
                            + "     $"
                            + budget
                            + "M";
        }


        playerView.setText(
                displayText
        );


        // ==================================================
        // ROYAL COLORS
        // ==================================================

        if (playerIsHost) {

            // Host = Royal Gold

            playerView.setTextColor(
                    Color.parseColor(
                            "#D4AF37"
                    )
            );

        } else {

            // Other players = Soft White

            playerView.setTextColor(
                    Color.parseColor(
                            "#F5F5F5"
                    )
            );
        }


        // ==================================================
        // FONT SIZE
        // ==================================================

        playerView.setTextSize(
                16
        );


        // ==================================================
        // ALIGNMENT
        // ==================================================

        playerView.setGravity(
                Gravity.CENTER_VERTICAL
        );


        playerView.setPadding(
                dpToPx(18),
                0,
                dpToPx(18),
                0
        );


        // ==================================================
        // CARD BACKGROUND
        // ==================================================

        playerView.setBackgroundResource(
                R.drawable.menu_card
        );


        // ==================================================
        // CARD SIZE
        // ==================================================

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(55)
                );


        // Space between player cards

        params.setMargins(
                0,
                0,
                0,
                dpToPx(10)
        );


        playerView.setLayoutParams(
                params
        );


        // ==================================================
        // ADD PLAYER
        // ==================================================

        playersContainer.addView(
                playerView
        );
    }


    // ==================================================
    // LISTEN FOR AUCTION STATUS
    // ==================================================

    private void listenForAuctionStatus() {

        if (roomRef == null) {

            return;
        }


        statusListener =
                roomRef
                        .child("status")
                        .addValueEventListener(

                                new ValueEventListener() {

                                    @Override
                                    public void onDataChange(
                                            @NonNull DataSnapshot snapshot
                                    ) {

                                        String status =
                                                snapshot.getValue(
                                                        String.class
                                                );


                                        // Auction started

                                        if ("started".equals(
                                                status
                                        )) {

                                            openAuctionScreen();

                                        }
                                    }


                                    @Override
                                    public void onCancelled(
                                            @NonNull DatabaseError error
                                    ) {

                                        Log.e(
                                                "WaitingLobby",
                                                "Status listener error: "
                                                        + error.getMessage()
                                        );
                                    }
                                }
                        );
    }


    // ==================================================
    // START AUCTION
    // ==================================================

    private void startAuction() {

        if (!isHost ||
                roomRef == null) {

            return;
        }


        roomRef
                .child("status")
                .setValue(
                        "started"
                )
                .addOnFailureListener(e -> {

                    Log.e(
                            "WaitingLobby",
                            "Failed to start auction",
                            e
                    );

                });
    }


    // ==================================================
    // OPEN AUCTION SCREEN
    // ==================================================

    private void openAuctionScreen() {

        // Prevent opening twice

        if (auctionOpened) {

            return;
        }


        auctionOpened =
                true;


        Intent intent =
                new Intent(
                        WaitingLobbyActivity.this,
                        AuctionActivity.class
                );


        // ==================================================
        // PASS ROOM CODE
        // ==================================================

        intent.putExtra(
                "ROOM_CODE",
                roomCode
        );


        // ==================================================
        // PASS PLAYER ID
        // ==================================================

        intent.putExtra(
                "PLAYER_ID",
                playerId
        );


        // ==================================================
        // PASS PLAYER NAME
        // ==================================================

        intent.putExtra(
                "PLAYER_NAME",
                playerName
        );


        // ==================================================
        // PASS HOST STATUS
        // ==================================================

        intent.putExtra(
                "IS_HOST",
                isHost
        );


        // ==================================================
        // OPEN AUCTION
        // ==================================================

        startActivity(
                intent
        );


        // ==================================================
        // REMOVE LOBBY FROM BACK STACK
        // ==================================================

        finish();
    }


    // ==================================================
    // SHARE ROOM
    // ==================================================

    private void shareRoom() {

        Intent shareIntent =
                new Intent(
                        Intent.ACTION_SEND
                );


        shareIntent.setType(
                "text/plain"
        );


        shareIntent.putExtra(
                Intent.EXTRA_TEXT,

                "Join my Auction Arena room!"
                        + "\nRoom Code: "
                        + roomCode
        );


        startActivity(

                Intent.createChooser(
                        shareIntent,
                        "Share Room Code"
                )

        );
    }


    // ==================================================
    // DP TO PIXELS
    // ==================================================

    private int dpToPx(
            int dp
    ) {

        return (int) (

                dp

                        *

                        getResources()
                                .getDisplayMetrics()
                                .density

        );
    }


    // ==================================================
    // REMOVE FIREBASE LISTENERS
    // ==================================================

    @Override
    protected void onDestroy() {


        if (roomRef != null) {


            // Remove players listener

            if (playersListener != null) {

                roomRef
                        .child("players")
                        .removeEventListener(
                                playersListener
                        );
            }


            // Remove status listener

            if (statusListener != null) {

                roomRef
                        .child("status")
                        .removeEventListener(
                                statusListener
                        );
            }
        }


        super.onDestroy();
    }
}