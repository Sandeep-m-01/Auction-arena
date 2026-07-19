package com.sandeep.auctionarena;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class AuctionFragment extends Fragment {

    // ==================================================
    // CONFIG
    // ==================================================

    private static final String DATABASE_URL =
            "https://auctionarena-c777d-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Money stored in millions
    // 1000 = $1000M
    private static final long STARTING_BUDGET = 1000L;


    // ==================================================
    // ROOM / USER
    // ==================================================

    private String roomCode;
    private String playerId;
    private String playerName;

    private boolean isHost;


    // ==================================================
    // UI
    // ==================================================

    private View playerCard;
    private View liveDot;

    private ImageView imgAuctionPlayer;

    private TextView txtAuctionPlayerName;
    private TextView txtPlayerPosition;
    private TextView txtCurrentBid;
    private TextView txtHighestBidder;
    private TextView txtRemainingBudget;
    private TextView txtSoldOverlay;

    private EditText etBidAmount;

    private TextView btnBid;
    private TextView btnGiveUp;


    // ==================================================
    // FIREBASE
    // ==================================================

    private DatabaseReference roomRef;
    private DatabaseReference auctionRef;
    private DatabaseReference myPlayerRef;

    private ValueEventListener auctionListener;
    private ValueEventListener budgetListener;


    // ==================================================
    // LOCAL STATE
    // ==================================================

    private long myBudget = STARTING_BUDGET;

    private boolean hasGivenUp = false;
    private boolean amIHighestBidder = false;

    private boolean resultAnimationRunning = false;
    private boolean hostCheckingWinner = false;

    // Prevent host from skipping the same player twice
    private boolean hostSkippingPlayer = false;

    private String lastHandledResultId = "";

    // Used to detect when a new auction player appears
    private String lastShownPlayerId = "";


    // ==================================================
    // SOUND
    // ==================================================

    private MediaPlayer hammerSound;
    private MediaPlayer cardSound;


    // ==================================================
    // CONSTRUCTOR
    // ==================================================

    public AuctionFragment() {
        // Required empty constructor
    }


    // ==================================================
    // ON CREATE
    // ==================================================

    @Override
    public void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

            roomCode =
                    getArguments()
                            .getString(
                                    "ROOM_CODE"
                            );

            playerId =
                    getArguments()
                            .getString(
                                    "PLAYER_ID"
                            );

            playerName =
                    getArguments()
                            .getString(
                                    "PLAYER_NAME"
                            );

            isHost =
                    getArguments()
                            .getBoolean(
                                    "IS_HOST",
                                    false
                            );
        }


        if (playerName == null ||
                playerName.trim().isEmpty()) {

            playerName = "PLAYER";
        }
    }


    // ==================================================
    // CREATE VIEW
    // ==================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_auction,
                container,
                false
        );
    }


    // ==================================================
    // VIEW CREATED
    // ==================================================

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );


        // ==================================================
        // CONNECT XML
        // ==================================================

        playerCard =
                view.findViewById(
                        R.id.playerCard
                );

        liveDot =
                view.findViewById(
                        R.id.liveDot
                );

        imgAuctionPlayer =
                view.findViewById(
                        R.id.imgAuctionPlayer
                );

        txtAuctionPlayerName =
                view.findViewById(
                        R.id.txtAuctionPlayerName
                );

        txtPlayerPosition =
                view.findViewById(
                        R.id.txtPlayerPosition
                );

        txtCurrentBid =
                view.findViewById(
                        R.id.txtCurrentBid
                );

        txtHighestBidder =
                view.findViewById(
                        R.id.txtHighestBidder
                );

        txtRemainingBudget =
                view.findViewById(
                        R.id.txtRemainingBudget
                );

        txtSoldOverlay =
                view.findViewById(
                        R.id.txtSoldOverlay
                );

        etBidAmount =
                view.findViewById(
                        R.id.etBidAmount
                );

        btnBid =
                view.findViewById(
                        R.id.btnBid
                );

        btnGiveUp =
                view.findViewById(
                        R.id.btnGiveUp
                );


        // ==================================================
        // DEFAULT UI
        // ==================================================

        txtAuctionPlayerName.setText("");

        txtPlayerPosition.setText("");

        txtCurrentBid.setText(
                "$0M"
        );

        txtHighestBidder.setText(
                "NO BIDS YET"
        );

        txtRemainingBudget.setText(
                "$1000M"
        );

        txtSoldOverlay.setVisibility(
                View.GONE
        );


        // ==================================================
        // LIVE DOT ANIMATION
        // ==================================================

        startLiveDotAnimation();


        // ==================================================
        // CREATE SOUNDS
        // ==================================================

        hammerSound =
                MediaPlayer.create(
                        requireContext(),
                        R.raw.hammer
                );

        cardSound =
                MediaPlayer.create(
                        requireContext(),
                        R.raw.card
                );


        // ==================================================
        // VALIDATE ROOM
        // ==================================================

        if (roomCode == null ||
                roomCode.trim().isEmpty()) {

            return;
        }


        // ==================================================
        // VALIDATE PLAYER
        // ==================================================

        if (playerId == null ||
                playerId.trim().isEmpty()) {

            return;
        }


        // ==================================================
        // FIREBASE
        // ==================================================

        FirebaseDatabase database =
                FirebaseDatabase.getInstance(
                        DATABASE_URL
                );


        roomRef =
                database
                        .getReference()
                        .child("rooms")
                        .child(roomCode);


        auctionRef =
                roomRef
                        .child("auction");


        myPlayerRef =
                roomRef
                        .child("players")
                        .child(playerId);


        // ==================================================
        // PLAYER BUDGET
        // ==================================================

        initializePlayerBudget();

        listenToMyBudget();


        // ==================================================
        // AUCTION LISTENER
        // ==================================================

        listenToAuction();


        // ==================================================
        // HOST INITIALIZES FIRST PLAYER
        // ==================================================

        if (isHost) {

            initializeFirstPlayer();
        }


        // ==================================================
        // BID BUTTON
        // ==================================================

        btnBid.setOnClickListener(v -> {

            // Player already gave up
            if (hasGivenUp) {

                return;
            }


            // Current highest bidder cannot bid again
            if (amIHighestBidder) {

                etBidAmount.setError(
                        "Wait for another player"
                );

                return;
            }


            String bidText =
                    etBidAmount
                            .getText()
                            .toString()
                            .trim();


            // Empty bid
            if (bidText.isEmpty()) {

                etBidAmount.setError(
                        "Enter bid amount in millions"
                );

                return;
            }


            long bidAmount;


            // Convert entered text to number
            try {

                bidAmount =
                        Long.parseLong(
                                bidText
                        );

            } catch (NumberFormatException e) {

                etBidAmount.setError(
                        "Enter a valid amount"
                );

                return;
            }


            // Bid must be positive
            if (bidAmount <= 0) {

                etBidAmount.setError(
                        "Bid must be at least $1M"
                );

                return;
            }


            // Cannot bid more than remaining budget
            if (bidAmount > myBudget) {

                etBidAmount.setError(
                        "Your budget is $"
                                + myBudget
                                + "M"
                );

                return;
            }


            // Send bid to Firebase
            placeBid(
                    bidAmount
            );

        });


        // ==================================================
        // GIVE UP BUTTON
        // ==================================================

        btnGiveUp.setOnClickListener(v -> {

            giveUp();

        });
    }


    // ==================================================
    // LIVE DOT ANIMATION
    // ==================================================

    private void startLiveDotAnimation() {

        if (liveDot == null) {
            return;
        }

        liveDot.animate()
                .alpha(0.15f)
                .setDuration(500)
                .withEndAction(() -> {

                    if (liveDot == null) {
                        return;
                    }

                    liveDot.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .withEndAction(
                                    this::startLiveDotAnimation
                            )
                            .start();

                })
                .start();
    }


    // ==================================================
    // INITIALIZE PLAYER BUDGET
    // ==================================================

    private void initializePlayerBudget() {

        if (myPlayerRef == null) {
            return;
        }

        myPlayerRef
                .child("budget")
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!snapshot.exists()) {

                                    myPlayerRef
                                            .child("budget")
                                            .setValue(
                                                    STARTING_BUDGET
                                            );
                                }
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                            }
                        }
                );
    }


    // ==================================================
    // PLACE BID
    // ==================================================

    private void placeBid(long newBid) {

        if (auctionRef == null ||
                playerId == null) {

            return;
        }


        auctionRef.runTransaction(

                new Transaction.Handler() {

                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(
                            @NonNull MutableData currentData
                    ) {

                        // ==========================================
                        // CHECK IF AUCTION IS COMPLETED
                        // ==========================================

                        Boolean completed =
                                currentData
                                        .child("completed")
                                        .getValue(
                                                Boolean.class
                                        );


                        if (Boolean.TRUE.equals(
                                completed
                        )) {

                            return Transaction.abort();
                        }


                        // ==========================================
                        // CHECK IF AUCTION IS BEING SKIPPED
                        // ==========================================

                        Boolean skipped =
                                currentData
                                        .child("skipped")
                                        .getValue(
                                                Boolean.class
                                        );


                        if (Boolean.TRUE.equals(
                                skipped
                        )) {

                            return Transaction.abort();
                        }


                        // ==========================================
                        // CHECK IF PLAYER GAVE UP
                        // ==========================================

                        Boolean gaveUp =
                                currentData
                                        .child("givenUpPlayers")
                                        .child(playerId)
                                        .getValue(
                                                Boolean.class
                                        );


                        if (Boolean.TRUE.equals(
                                gaveUp
                        )) {

                            return Transaction.abort();
                        }


                        // ==========================================
                        // GET CURRENT BID
                        // ==========================================

                        Long currentBid =
                                currentData
                                        .child("currentBid")
                                        .getValue(
                                                Long.class
                                        );


                        if (currentBid == null) {

                            currentBid = 0L;
                        }


                        // ==========================================
                        // GET CURRENT HIGHEST BIDDER
                        // ==========================================

                        String highestBidderId =
                                currentData
                                        .child("highestBidderId")
                                        .getValue(
                                                String.class
                                        );


                        // ==========================================
                        // HIGHEST BIDDER CANNOT BID AGAIN
                        // ==========================================

                        if (playerId.equals(
                                highestBidderId
                        )) {

                            return Transaction.abort();
                        }


                        // ==========================================
                        // NEW BID MUST BE HIGHER
                        // ==========================================

                        if (newBid <= currentBid) {

                            return Transaction.abort();
                        }


                        // ==========================================
                        // BID CANNOT EXCEED BUDGET
                        // ==========================================

                        if (newBid > myBudget) {

                            return Transaction.abort();
                        }


                        // ==========================================
                        // UPDATE CURRENT BID
                        // ==========================================

                        currentData
                                .child("currentBid")
                                .setValue(
                                        newBid
                                );


                        // ==========================================
                        // UPDATE HIGHEST BIDDER ID
                        // ==========================================

                        currentData
                                .child("highestBidderId")
                                .setValue(
                                        playerId
                                );


                        // ==========================================
                        // UPDATE HIGHEST BIDDER NAME
                        // ==========================================

                        currentData
                                .child("highestBidderName")
                                .setValue(
                                        playerName
                                );


                        return Transaction.success(
                                currentData
                        );
                    }


                    @Override
                    public void onComplete(
                            @Nullable DatabaseError error,
                            boolean committed,
                            @Nullable DataSnapshot snapshot
                    ) {

                        if (!isAdded()) {
                            return;
                        }


                        if (committed) {

                            // Clear bid input after successful bid
                            etBidAmount.setText("");

                        } else {

                            if (!hasGivenUp &&
                                    !amIHighestBidder) {

                                etBidAmount.setError(
                                        "Bid rejected. Enter a higher valid bid."
                                );
                            }
                        }
                    }
                }
        );
    }


    // ==================================================
    // GIVE UP
    // ==================================================

    private void giveUp() {

        if (auctionRef == null ||
                playerId == null ||
                hasGivenUp) {

            return;
        }


        /*
         * The current highest bidder cannot give up.
         *
         * They are currently winning the auction,
         * so they must wait for another player
         * to place a higher bid.
         */

        if (amIHighestBidder) {

            return;
        }


        auctionRef
                .child("givenUpPlayers")
                .child(playerId)
                .setValue(true)
                .addOnSuccessListener(unused -> {

                    hasGivenUp = true;

                    updateBiddingControls();

                });
    }

    // ==================================================
    // LISTEN TO AUCTION
    // ==================================================

    private void listenToAuction() {

        if (auctionRef == null) {
            return;
        }


        auctionListener =
                auctionRef.addValueEventListener(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!isAdded()) {
                                    return;
                                }


                                // ==========================================
                                // CURRENT FOOTBALLER
                                // ==========================================

                                DataSnapshot currentPlayer =
                                        snapshot.child(
                                                "currentPlayer"
                                        );


                                Object footballerIdObject =
                                        currentPlayer
                                                .child("id")
                                                .getValue();


                                String currentFootballerId =
                                        footballerIdObject != null
                                                ? String.valueOf(
                                                footballerIdObject
                                        )
                                                : "";


                                String footballerName =
                                        currentPlayer
                                                .child("name")
                                                .getValue(
                                                        String.class
                                                );


                                String position =
                                        currentPlayer
                                                .child("position")
                                                .getValue(
                                                        String.class
                                                );


                                String type =
                                        currentPlayer
                                                .child("type")
                                                .getValue(
                                                        String.class
                                                );


                                String imageName =
                                        currentPlayer
                                                .child("image")
                                                .getValue(
                                                        String.class
                                                );


                                // ==========================================
                                // DETECT NEW PLAYER CARD
                                // ==========================================

                                if (!currentFootballerId.isEmpty() &&
                                        !currentFootballerId.equals(
                                                lastShownPlayerId
                                        )) {


                                    lastShownPlayerId =
                                            currentFootballerId;


                                    // Reset local state for new player
                                    hasGivenUp =
                                            false;

                                    amIHighestBidder =
                                            false;

                                    resultAnimationRunning =
                                            false;


                                    // Play card sound
                                    playCardSound();
                                }


                                // ==========================================
                                // LOAD PLAYER IMAGE
                                // ==========================================

                                if (imageName != null &&
                                        !imageName.trim().isEmpty()) {


                                    int imageResource =
                                            getResources()
                                                    .getIdentifier(
                                                            imageName,
                                                            "drawable",
                                                            requireContext()
                                                                    .getPackageName()
                                                    );


                                    if (imageResource != 0) {

                                        imgAuctionPlayer
                                                .setImageResource(
                                                        imageResource
                                                );

                                    } else {

                                        imgAuctionPlayer
                                                .setImageDrawable(
                                                        null
                                                );
                                    }

                                } else {

                                    imgAuctionPlayer
                                            .setImageDrawable(
                                                    null
                                            );
                                }


                                // ==========================================
                                // CURRENT BID
                                // ==========================================

                                Long currentBid =
                                        snapshot
                                                .child("currentBid")
                                                .getValue(
                                                        Long.class
                                                );


                                if (currentBid == null) {

                                    currentBid = 0L;
                                }


                                // ==========================================
                                // HIGHEST BIDDER
                                // ==========================================

                                String highestBidderId =
                                        snapshot
                                                .child("highestBidderId")
                                                .getValue(
                                                        String.class
                                                );


                                String highestBidderName =
                                        snapshot
                                                .child("highestBidderName")
                                                .getValue(
                                                        String.class
                                                );


                                // ==========================================
                                // AM I HIGHEST BIDDER?
                                // ==========================================

                                amIHighestBidder =

                                        playerId != null

                                                &&

                                                playerId.equals(
                                                        highestBidderId
                                                );


                                // ==========================================
                                // DID I GIVE UP?
                                // ==========================================

                                Boolean gaveUp =
                                        snapshot
                                                .child("givenUpPlayers")
                                                .child(playerId)
                                                .getValue(
                                                        Boolean.class
                                                );


                                hasGivenUp =
                                        Boolean.TRUE.equals(
                                                gaveUp
                                        );


                                // ==========================================
                                // UPDATE PLAYER NAME
                                // ==========================================

                                if (footballerName != null) {

                                    txtAuctionPlayerName
                                            .setText(
                                                    footballerName
                                                            .toUpperCase()
                                            );

                                } else {

                                    txtAuctionPlayerName
                                            .setText("");
                                }


                                // ==========================================
                                // POSITION + TYPE
                                // ==========================================

                                if (position != null) {

                                    if (type != null &&
                                            !type.isEmpty()) {

                                        txtPlayerPosition
                                                .setText(
                                                        position.toUpperCase()
                                                                + " • "
                                                                + type.toUpperCase()
                                                );

                                    } else {

                                        txtPlayerPosition
                                                .setText(
                                                        position.toUpperCase()
                                                );
                                    }

                                } else {

                                    txtPlayerPosition
                                            .setText("");
                                }


                                // ==========================================
                                // UPDATE CURRENT BID
                                // ==========================================

                                txtCurrentBid
                                        .setText(
                                                "$"
                                                        + currentBid
                                                        + "M"
                                        );


                                // ==========================================
                                // UPDATE HIGHEST BIDDER TEXT
                                // ==========================================

                                if (highestBidderName == null ||
                                        highestBidderName.isEmpty()) {

                                    txtHighestBidder
                                            .setText(
                                                    "NO BIDS YET"
                                            );

                                } else if (amIHighestBidder) {

                                    txtHighestBidder
                                            .setText(
                                                    "YOU ARE WINNING • WAIT"
                                            );

                                } else {

                                    txtHighestBidder
                                            .setText(
                                                    "HIGHEST • "
                                                            + highestBidderName
                                                            .toUpperCase()
                                            );
                                }


                                // ==========================================
                                // CHECK SOLD / COMPLETED
                                // ==========================================

                                Boolean completed =
                                        snapshot
                                                .child("completed")
                                                .getValue(
                                                        Boolean.class
                                                );


                                if (Boolean.TRUE.equals(
                                        completed
                                )) {

                                    handleCompletedAuction(
                                            snapshot.child(
                                                    "result"
                                            )
                                    );

                                    return;
                                }


                                // ==========================================
                                // CHECK SKIPPED PLAYER
                                // ==========================================

                                Boolean skipped =
                                        snapshot
                                                .child("skipped")
                                                .getValue(
                                                        Boolean.class
                                                );


                                String skipId =
                                        snapshot
                                                .child("skipId")
                                                .getValue(
                                                        String.class
                                                );


                                if (Boolean.TRUE.equals(
                                        skipped
                                )) {

                                    handleSkippedAuction(
                                            skipId
                                    );

                                    return;
                                }


                                // ==========================================
                                // UPDATE BID / GIVE UP BUTTONS
                                // ==========================================

                                updateBiddingControls();


                                // ==========================================
                                // ONLY HOST DECIDES WINNER OR SKIP
                                // ==========================================

                                if (isHost) {

                                    checkForAuctionWinner();

                                }
                            }


                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                            }
                        }
                );
    }


    // ==================================================
    // PLAY CARD SOUND
    // ==================================================

    private void playCardSound() {

        if (cardSound == null) {
            return;
        }


        try {

            cardSound.seekTo(0);

            cardSound.start();

        } catch (Exception ignored) {

        }
    }

    // ==================================================
    // CHECK FOR AUCTION WINNER / SKIP
    // ==================================================

    private void checkForAuctionWinner() {

        if (!isHost ||
                roomRef == null ||
                auctionRef == null ||
                hostCheckingWinner ||
                hostSkippingPlayer) {

            return;
        }


        hostCheckingWinner = true;


        roomRef.addListenerForSingleValueEvent(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot roomSnapshot
                    ) {

                        hostCheckingWinner = false;


                        // Fragment is no longer active
                        if (!isAdded()) {

                            return;
                        }


                        DataSnapshot auctionSnapshot =
                                roomSnapshot.child(
                                        "auction"
                                );


                        // ==========================================
                        // ALREADY COMPLETED?
                        // ==========================================

                        Boolean completed =
                                auctionSnapshot
                                        .child("completed")
                                        .getValue(
                                                Boolean.class
                                        );


                        if (Boolean.TRUE.equals(
                                completed
                        )) {

                            return;
                        }


                        // ==========================================
                        // ALREADY SKIPPING?
                        // ==========================================

                        Boolean skipped =
                                auctionSnapshot
                                        .child("skipped")
                                        .getValue(
                                                Boolean.class
                                        );


                        if (Boolean.TRUE.equals(
                                skipped
                        )) {

                            return;
                        }


                        // ==========================================
                        // CURRENT HIGHEST BIDDER
                        // ==========================================

                        String highestBidderId =
                                auctionSnapshot
                                        .child("highestBidderId")
                                        .getValue(
                                                String.class
                                        );


                        String highestBidderName =
                                auctionSnapshot
                                        .child("highestBidderName")
                                        .getValue(
                                                String.class
                                        );


                        // ==========================================
                        // CURRENT BID
                        // ==========================================

                        Long currentBid =
                                auctionSnapshot
                                        .child("currentBid")
                                        .getValue(
                                                Long.class
                                        );


                        if (currentBid == null) {

                            currentBid = 0L;
                        }


                        // ==========================================
                        // GET ALL ROOM PLAYERS
                        // ==========================================

                        DataSnapshot playersSnapshot =
                                roomSnapshot.child(
                                        "players"
                                );


                        int totalPlayers = 0;

                        int activePlayers = 0;

                        String remainingPlayerId =
                                null;


                        // ==========================================
                        // COUNT ACTIVE PLAYERS
                        // ==========================================

                        for (DataSnapshot participant :
                                playersSnapshot.getChildren()) {


                            String id =
                                    participant.getKey();


                            if (id == null) {

                                continue;
                            }


                            totalPlayers++;


                            Boolean participantGaveUp =
                                    auctionSnapshot
                                            .child("givenUpPlayers")
                                            .child(id)
                                            .getValue(
                                                    Boolean.class
                                            );


                            // Player is still active
                            if (!Boolean.TRUE.equals(
                                    participantGaveUp
                            )) {

                                activePlayers++;

                                remainingPlayerId =
                                        id;
                            }
                        }


                        // ==========================================
                        // NEED AT LEAST 2 PLAYERS
                        // ==========================================

                        if (totalPlayers < 2) {

                            return;
                        }


                        // ==========================================
                        // EVERYONE GAVE UP
                        // ==========================================

                        /*
                         * Example:
                         *
                         * Player A -> GIVE UP
                         * Player B -> GIVE UP
                         *
                         * activePlayers = 0
                         *
                         * No one wins the footballer.
                         * The footballer is skipped.
                         */

                        if (activePlayers == 0) {

                            skipCurrentPlayer();

                            return;
                        }


                        // ==========================================
                        // NO BID YET
                        // ==========================================

                        /*
                         * If players are still active but nobody
                         * has placed a bid, there is no winner yet.
                         */

                        if (highestBidderId == null ||
                                currentBid <= 0) {

                            return;
                        }


                        // ==========================================
                        // ONE ACTIVE PLAYER REMAINS
                        // ==========================================

                        /*
                         * If only one active player remains,
                         * they win only if they are also the
                         * current highest bidder.
                         */

                        if (activePlayers == 1 &&
                                remainingPlayerId != null &&
                                remainingPlayerId.equals(
                                        highestBidderId
                                )) {


                            finishAuction(
                                    highestBidderId,
                                    highestBidderName,
                                    currentBid,
                                    auctionSnapshot
                            );

                        }
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        hostCheckingWinner =
                                false;

                    }
                }
        );
    }


    // ==================================================
    // SKIP CURRENT PLAYER
    // ==================================================

    private void skipCurrentPlayer() {

        if (!isHost ||
                auctionRef == null ||
                hostSkippingPlayer) {

            return;
        }


        // Prevent duplicate skip calls
        hostSkippingPlayer =
                true;


        // Unique ID for this skip event
        String skipId =
                "skip_"
                        + System.currentTimeMillis();


        Map<String, Object> updates =
                new HashMap<>();


        updates.put(
                "skipped",
                true
        );


        updates.put(
                "skipId",
                skipId
        );


        auctionRef
                .updateChildren(
                        updates
                )
                .addOnFailureListener(e -> {

                    // Allow retry if Firebase update fails
                    hostSkippingPlayer =
                            false;

                });
    }


    // ==================================================
    // HANDLE SKIPPED AUCTION
    // ==================================================

    private void handleSkippedAuction(
            String skipId
    ) {

        if (skipId == null ||
                skipId.trim().isEmpty()) {

            return;
        }


        // Prevent the same skip animation
        // from running multiple times
        if (skipId.equals(
                lastHandledResultId
        )) {

            return;
        }


        lastHandledResultId =
                skipId;


        // ==========================================
        // DISABLE INPUT
        // ==========================================

        if (etBidAmount != null) {

            etBidAmount.setEnabled(
                    false
            );
        }


        if (btnBid != null) {

            btnBid.setEnabled(
                    false
            );

            btnBid.setAlpha(
                    0.4f
            );
        }


        if (btnGiveUp != null) {

            btnGiveUp.setEnabled(
                    false
            );

            btnGiveUp.setAlpha(
                    0.4f
            );
        }


        // ==========================================
        // SHOW SKIPPED STATUS
        // ==========================================

        if (txtHighestBidder != null) {

            txtHighestBidder.setText(
                    "PLAYER SKIPPED"
            );
        }


        // ==========================================
        // WAIT THEN ANIMATE CARD
        // ==========================================

        if (playerCard != null) {

            playerCard.postDelayed(
                    this::animateSkippedPlayer,
                    700
            );
        }
    }


    // ==================================================
    // ANIMATE SKIPPED PLAYER
    // ==================================================

    private void animateSkippedPlayer() {

        if (playerCard == null ||
                resultAnimationRunning) {

            return;
        }


        resultAnimationRunning =
                true;


        // ==========================================
        // OLD CARD MOVES RIGHT
        // ==========================================

        playerCard
                .animate()
                .translationX(
                        playerCard.getWidth()
                                + 500f
                )
                .alpha(0f)
                .setDuration(600)
                .withEndAction(() -> {


                    // ==========================================
                    // HOST LOADS NEXT PLAYER
                    // ==========================================

                    if (isHost) {

                        loadNextPlayer();
                    }


                    if (playerCard == null) {

                        resultAnimationRunning =
                                false;

                        return;
                    }


                    // ==========================================
                    // PLACE CARD OUTSIDE LEFT
                    // ==========================================

                    playerCard.setTranslationX(
                            -playerCard.getWidth()
                                    - 500f
                    );


                    playerCard.setAlpha(
                            0f
                    );


                    // ==========================================
                    // NEW CARD ENTERS FROM LEFT
                    // ==========================================

                    playerCard
                            .animate()
                            .translationX(
                                    0f
                            )
                            .alpha(
                                    1f
                            )
                            .setDuration(
                                    700
                            )
                            .withEndAction(() -> {

                                resultAnimationRunning =
                                        false;

                            })
                            .start();

                })
                .start();
    }

    // ==================================================
    // FINISH AUCTION
    // ==================================================

    private void finishAuction(
            String winnerId,
            String winnerName,
            long winningBid,
            DataSnapshot auctionSnapshot
    ) {

        if (!isHost ||
                roomRef == null) {

            return;
        }


        // ==========================================
        // GET CURRENT FOOTBALLER
        // ==========================================

        DataSnapshot footballerSnapshot =
                auctionSnapshot.child(
                        "currentPlayer"
                );


        if (!footballerSnapshot.exists()) {

            return;
        }


        // ==========================================
        // FOOTBALLER ID
        // ==========================================

        Object footballerIdObject =
                footballerSnapshot
                        .child("id")
                        .getValue();


        if (footballerIdObject == null) {

            return;
        }


        String footballerId =
                String.valueOf(
                        footballerIdObject
                );


        // ==========================================
        // FOOTBALLER NAME
        // ==========================================

        String footballerName =
                footballerSnapshot
                        .child("name")
                        .getValue(
                                String.class
                        );


        // ==========================================
        // POSITION
        // ==========================================

        String position =
                footballerSnapshot
                        .child("position")
                        .getValue(
                                String.class
                        );


        // ==========================================
        // TYPE
        // ==========================================

        String type =
                footballerSnapshot
                        .child("type")
                        .getValue(
                                String.class
                        );


        // ==========================================
        // IMAGE
        // ==========================================

        String image =
                footballerSnapshot
                        .child("image")
                        .getValue(
                                String.class
                        );


        // ==========================================
        // UNIQUE RESULT ID
        // ==========================================

        String resultId =
                footballerId
                        + "_"
                        + System.currentTimeMillis();


        // ==================================================
        // CREATE RESULT DATA
        // ==================================================

        Map<String, Object> result =
                new HashMap<>();


        result.put(
                "resultId",
                resultId
        );


        result.put(
                "winnerId",
                winnerId
        );


        result.put(
                "winnerName",
                winnerName
        );


        result.put(
                "winningBid",
                winningBid
        );


        result.put(
                "playerId",
                footballerId
        );


        result.put(
                "playerName",
                footballerName
        );


        result.put(
                "position",
                position
        );


        result.put(
                "type",
                type
        );


        result.put(
                "image",
                image
        );


        // ==================================================
        // COMPLETE AUCTION
        // ==================================================

        Map<String, Object> updates =
                new HashMap<>();


        updates.put(
                "auction/completed",
                true
        );


        updates.put(
                "auction/result",
                result
        );


        roomRef
                .updateChildren(
                        updates
                )
                .addOnSuccessListener(unused -> {


                    // ==========================================
                    // DEDUCT WINNER'S BUDGET
                    // ==========================================

                    deductWinnerBudget(
                            winnerId,
                            winningBid
                    );


                    // ==========================================
                    // ADD FOOTBALLER TO WINNER'S TEAM
                    // ==========================================

                    addFootballerToWinnerTeam(
                            winnerId,
                            footballerId,
                            footballerName,
                            position,
                            type,
                            image,
                            winningBid
                    );

                });
    }


    // ==================================================
    // DEDUCT WINNER BUDGET
    // ==================================================

    private void deductWinnerBudget(
            String winnerId,
            long winningBid
    ) {

        if (roomRef == null) {

            return;
        }


        DatabaseReference budgetRef =
                roomRef
                        .child("players")
                        .child(winnerId)
                        .child("budget");


        budgetRef.runTransaction(

                new Transaction.Handler() {

                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(
                            @NonNull MutableData currentData
                    ) {

                        Long budget =
                                currentData.getValue(
                                        Long.class
                                );


                        if (budget == null) {

                            budget =
                                    STARTING_BUDGET;
                        }


                        // ==========================================
                        // CALCULATE NEW BUDGET
                        // ==========================================

                        long newBudget =
                                Math.max(
                                        0,
                                        budget - winningBid
                                );


                        currentData.setValue(
                                newBudget
                        );


                        return Transaction.success(
                                currentData
                        );
                    }


                    @Override
                    public void onComplete(
                            @Nullable DatabaseError error,
                            boolean committed,
                            @Nullable DataSnapshot snapshot
                    ) {

                    }
                }
        );
    }


    // ==================================================
    // ADD FOOTBALLER TO WINNER TEAM
    // ==================================================

    private void addFootballerToWinnerTeam(
            String winnerId,
            String footballerId,
            String footballerName,
            String position,
            String type,
            String image,
            long price
    ) {

        if (roomRef == null) {

            return;
        }


        // ==========================================
        // CREATE PLAYER DATA
        // ==========================================

        Map<String, Object> playerData =
                new HashMap<>();


        playerData.put(
                "id",
                footballerId
        );


        playerData.put(
                "name",
                footballerName
        );


        playerData.put(
                "position",
                position
        );


        playerData.put(
                "type",
                type
        );


        playerData.put(
                "image",
                image
        );


        playerData.put(
                "price",
                price
        );


        // ==========================================
        // SAVE TO WINNER TEAM
        // ==========================================

        /*
         * Firebase:
         *
         * rooms
         *   ROOM_CODE
         *     players
         *       WINNER_ID
         *         team
         *           FOOTBALLER_ID
         *
         * Using footballerId as the key also prevents
         * the same footballer from being added twice
         * to the same player's team.
         */

        roomRef
                .child("players")
                .child(winnerId)
                .child("team")
                .child(footballerId)
                .setValue(
                        playerData
                );
    }
    // ==================================================
    // INITIALIZE FIRST PLAYER
    // ==================================================

    private void initializeFirstPlayer() {

        if (auctionRef == null) {
            return;
        }


        auctionRef
                .child("currentPlayer")
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                // If a player already exists,
                                // do not create another one.
                                if (snapshot.exists()) {
                                    return;
                                }


                                loadNextPlayer();
                            }


                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                            }
                        }
                );
    }


    // ==================================================
    // LOAD NEXT PLAYER - NO REPEATED PLAYERS
    // ==================================================

    private void loadNextPlayer() {

        if (!isHost ||
                roomRef == null ||
                auctionRef == null ||
                !isAdded()) {

            return;
        }


        // ==================================================
        // LOAD ALL PLAYERS FROM LOCAL JSON
        // ==================================================

        List<Player> allPlayers =
                PlayerLoader.loadPlayers(
                        requireContext()
                );


        if (allPlayers == null ||
                allPlayers.isEmpty()) {

            hostSkippingPlayer = false;

            return;
        }


        // ==================================================
        // READ USED PLAYERS FROM FIREBASE
        // ==================================================

        roomRef
                .child("usedPlayers")
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!isAdded() ||
                                        !isHost ||
                                        roomRef == null ||
                                        auctionRef == null) {

                                    hostSkippingPlayer = false;

                                    return;
                                }


                                // ==================================================
                                // CREATE SET OF USED PLAYER IDS
                                // ==================================================

                                Set<String> usedPlayerIds =
                                        new HashSet<>();


                                for (DataSnapshot usedPlayerSnapshot :
                                        snapshot.getChildren()) {


                                    Boolean used =
                                            usedPlayerSnapshot
                                                    .getValue(
                                                            Boolean.class
                                                    );


                                    String usedPlayerId =
                                            usedPlayerSnapshot
                                                    .getKey();


                                    if (Boolean.TRUE.equals(used) &&
                                            usedPlayerId != null) {

                                        usedPlayerIds.add(
                                                usedPlayerId
                                        );
                                    }
                                }


                                // ==================================================
                                // CREATE AVAILABLE PLAYER LIST
                                // ==================================================

                                List<Player> availablePlayers =
                                        new ArrayList<>();


                                for (Player player :
                                        allPlayers) {


                                    String footballerId =
                                            String.valueOf(
                                                    player.getId()
                                            );


                                    // Only add players who have
                                    // never appeared in this room.
                                    if (!usedPlayerIds.contains(
                                            footballerId
                                    )) {

                                        availablePlayers.add(
                                                player
                                        );
                                    }
                                }


                                // ==================================================
                                // ALL PLAYERS HAVE BEEN USED
                                // ==================================================

                                if (availablePlayers.isEmpty()) {

                                    hostSkippingPlayer = false;

                                    hostCheckingWinner = false;

                                    showAuctionFinished();

                                    return;
                                }


                                // ==================================================
                                // SELECT RANDOM UNUSED PLAYER
                                // ==================================================

                                Random random =
                                        new Random();


                                Player selectedPlayer =
                                        availablePlayers.get(

                                                random.nextInt(
                                                        availablePlayers.size()
                                                )

                                        );


                                String selectedPlayerId =
                                        String.valueOf(
                                                selectedPlayer.getId()
                                        );


                                // ==================================================
                                // CREATE PLAYER DATA
                                // ==================================================

                                Map<String, Object> playerData =
                                        new HashMap<>();


                                playerData.put(
                                        "id",
                                        selectedPlayer.getId()
                                );


                                playerData.put(
                                        "name",
                                        selectedPlayer.getName()
                                );


                                playerData.put(
                                        "position",
                                        selectedPlayer.getPosition()
                                );


                                playerData.put(
                                        "type",
                                        selectedPlayer.getType()
                                );


                                playerData.put(
                                        "image",
                                        selectedPlayer.getImage()
                                );


                                // ==================================================
                                // UPDATE FIREBASE
                                // ==================================================

                                Map<String, Object> updates =
                                        new HashMap<>();


                                /*
                                 * Mark the player as used immediately
                                 * when they enter the auction.
                                 *
                                 * This means:
                                 *
                                 * SOLD -> cannot appear again
                                 * SKIPPED -> cannot appear again
                                 */

                                updates.put(
                                        "usedPlayers/"
                                                + selectedPlayerId,
                                        true
                                );


                                // Set new auction player
                                updates.put(
                                        "auction/currentPlayer",
                                        playerData
                                );


                                // Reset current bid
                                updates.put(
                                        "auction/currentBid",
                                        0
                                );


                                // Reset highest bidder
                                updates.put(
                                        "auction/highestBidderId",
                                        null
                                );


                                updates.put(
                                        "auction/highestBidderName",
                                        null
                                );


                                // Reset give-up list
                                updates.put(
                                        "auction/givenUpPlayers",
                                        null
                                );


                                // Reset completed state
                                updates.put(
                                        "auction/completed",
                                        false
                                );


                                // Remove previous result
                                updates.put(
                                        "auction/result",
                                        null
                                );


                                // Reset skipped state
                                updates.put(
                                        "auction/skipped",
                                        false
                                );


                                updates.put(
                                        "auction/skipId",
                                        null
                                );


                                // ==================================================
                                // SEND UPDATE
                                // ==================================================

                                roomRef
                                        .updateChildren(
                                                updates
                                        )
                                        .addOnCompleteListener(task -> {

                                            // Allow winner/skip checks
                                            // for the new player.
                                            hostSkippingPlayer =
                                                    false;

                                            hostCheckingWinner =
                                                    false;

                                        });

                            }


                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                hostSkippingPlayer =
                                        false;

                                hostCheckingWinner =
                                        false;

                            }
                        }
                );
    }


    // ==================================================
    // AUCTION FINISHED - ALL PLAYERS USED
    // ==================================================

    private void showAuctionFinished() {

        if (!isAdded()) {
            return;
        }


        resultAnimationRunning =
                false;

        hostSkippingPlayer =
                false;

        hostCheckingWinner =
                false;


        // ==================================================
        // UPDATE UI
        // ==================================================

        if (txtAuctionPlayerName != null) {

            txtAuctionPlayerName.setText(
                    "AUCTION COMPLETE"
            );
        }


        if (txtPlayerPosition != null) {

            txtPlayerPosition.setText(
                    "ALL PLAYERS HAVE BEEN USED"
            );
        }


        if (txtCurrentBid != null) {

            txtCurrentBid.setText(
                    "$0M"
            );
        }


        if (txtHighestBidder != null) {

            txtHighestBidder.setText(
                    "AUCTION FINISHED"
            );
        }


        // ==================================================
        // REMOVE PLAYER IMAGE
        // ==================================================

        if (imgAuctionPlayer != null) {

            imgAuctionPlayer.setImageDrawable(
                    null
            );
        }


        // ==================================================
        // HIDE SOLD OVERLAY
        // ==================================================

        if (txtSoldOverlay != null) {

            txtSoldOverlay.setVisibility(
                    View.GONE
            );
        }


        // ==================================================
        // DISABLE BIDDING
        // ==================================================

        if (etBidAmount != null) {

            etBidAmount.setEnabled(
                    false
            );
        }


        if (btnBid != null) {

            btnBid.setEnabled(
                    false
            );

            btnBid.setAlpha(
                    0.4f
            );
        }


        if (btnGiveUp != null) {

            btnGiveUp.setEnabled(
                    false
            );

            btnGiveUp.setAlpha(
                    0.4f
            );
        }
    }

    // ==================================================
    // HANDLE COMPLETED AUCTION
    // ==================================================

    private void handleCompletedAuction(
            DataSnapshot resultSnapshot
    ) {

        if (!resultSnapshot.exists()) {
            return;
        }


        String resultId =
                resultSnapshot
                        .child("resultId")
                        .getValue(
                                String.class
                        );


        if (resultId == null) {
            return;
        }


        // ==================================================
        // PREVENT SAME RESULT TWICE
        // ==================================================

        if (resultId.equals(
                lastHandledResultId
        )) {

            return;
        }


        lastHandledResultId =
                resultId;


        // ==================================================
        // GET RESULT DATA
        // ==================================================

        String winnerName =
                resultSnapshot
                        .child("winnerName")
                        .getValue(
                                String.class
                        );


        String footballerName =
                resultSnapshot
                        .child("playerName")
                        .getValue(
                                String.class
                        );


        Long winningBid =
                resultSnapshot
                        .child("winningBid")
                        .getValue(
                                Long.class
                        );


        // ==================================================
        // DISABLE INPUT
        // ==================================================

        if (etBidAmount != null) {

            etBidAmount.setEnabled(
                    false
            );
        }


        if (btnBid != null) {

            btnBid.setEnabled(
                    false
            );

            btnBid.setAlpha(
                    0.4f
            );
        }


        if (btnGiveUp != null) {

            btnGiveUp.setEnabled(
                    false
            );

            btnGiveUp.setAlpha(
                    0.4f
            );
        }


        // ==================================================
        // PLAY HAMMER SOUND
        // ==================================================

        playHammerSound();


        // ==================================================
        // SHOW SOLD OVERLAY
        // ==================================================

        if (txtSoldOverlay != null) {

            txtSoldOverlay.setVisibility(
                    View.VISIBLE
            );
        }


        // ==================================================
        // SHOW RESULT TEXT
        // ==================================================

        if (footballerName != null &&
                winnerName != null &&
                txtHighestBidder != null) {


            String resultText =
                    footballerName.toUpperCase()
                            + " SOLD TO "
                            + winnerName.toUpperCase();


            if (winningBid != null) {

                resultText +=
                        " • $"
                                + winningBid
                                + "M";
            }


            txtHighestBidder.setText(
                    resultText
            );
        }


        // ==================================================
        // WAIT THEN ANIMATE SOLD CARD
        // ==================================================

        if (playerCard != null) {

            playerCard.postDelayed(
                    this::animateSoldPlayer,
                    1200
            );
        }
    }


    // ==================================================
    // PLAY HAMMER SOUND
    // ==================================================

    private void playHammerSound() {

        if (hammerSound == null) {
            return;
        }


        try {

            hammerSound.seekTo(
                    0
            );

            hammerSound.start();

        } catch (Exception ignored) {

        }
    }


    // ==================================================
    // SOLD PLAYER CARD ANIMATION
    // ==================================================

    private void animateSoldPlayer() {

        if (playerCard == null ||
                resultAnimationRunning) {

            return;
        }


        resultAnimationRunning =
                true;


        /*
         * SOLD PLAYER:
         *
         * Move the old card
         * out to the RIGHT.
         */

        playerCard
                .animate()
                .translationX(
                        playerCard.getWidth()
                                + 500f
                )
                .alpha(
                        0f
                )
                .setDuration(
                        700
                )
                .withEndAction(() -> {


                    // ==================================================
                    // HOST LOADS NEXT PLAYER
                    // ==================================================

                    if (isHost) {

                        loadNextPlayer();
                    }


                    // ==================================================
                    // HIDE SOLD OVERLAY
                    // ==================================================

                    if (txtSoldOverlay != null) {

                        txtSoldOverlay.setVisibility(
                                View.GONE
                        );
                    }


                    if (playerCard == null) {

                        resultAnimationRunning =
                                false;

                        return;
                    }


                    /*
                     * Put card outside
                     * the LEFT side.
                     */

                    playerCard.setTranslationX(
                            -playerCard.getWidth()
                                    - 500f
                    );


                    playerCard.setAlpha(
                            0f
                    );


                    /*
                     * NEW PLAYER:
                     *
                     * Card enters
                     * FROM THE LEFT.
                     */

                    playerCard
                            .animate()
                            .translationX(
                                    0f
                            )
                            .alpha(
                                    1f
                            )
                            .setDuration(
                                    700
                            )
                            .withEndAction(() -> {

                                resultAnimationRunning =
                                        false;

                            })
                            .start();

                })
                .start();
    }


    // ==================================================
    // UPDATE BIDDING CONTROLS
    // ==================================================

    private void updateBiddingControls() {


        // ==================================================
        // PLAYER GAVE UP
        // ==================================================

        if (hasGivenUp) {

            if (etBidAmount != null) {

                etBidAmount.setEnabled(
                        false
                );
            }


            if (btnBid != null) {

                btnBid.setEnabled(
                        false
                );

                btnBid.setAlpha(
                        0.4f
                );
            }


            if (btnGiveUp != null) {

                btnGiveUp.setEnabled(
                        false
                );

                btnGiveUp.setAlpha(
                        0.4f
                );
            }


            return;
        }


        // ==================================================
        // HIGHEST BIDDER MUST WAIT
        // ==================================================

        if (amIHighestBidder) {

            if (etBidAmount != null) {

                etBidAmount.setEnabled(
                        false
                );
            }


            if (btnBid != null) {

                btnBid.setEnabled(
                        false
                );

                btnBid.setAlpha(
                        0.4f
                );
            }


            if (btnGiveUp != null) {

                btnGiveUp.setEnabled(
                        false
                );

                btnGiveUp.setAlpha(
                        0.4f
                );
            }


            return;
        }


        // ==================================================
        // PLAYER CAN BID / GIVE UP
        // ==================================================

        if (etBidAmount != null) {

            etBidAmount.setEnabled(
                    true
            );
        }


        if (btnBid != null) {

            btnBid.setEnabled(
                    true
            );

            btnBid.setAlpha(
                    1f
            );
        }


        if (btnGiveUp != null) {

            btnGiveUp.setEnabled(
                    true
            );

            btnGiveUp.setAlpha(
                    1f
            );
        }
    }


    // ==================================================
    // LISTEN TO MY BUDGET
    // ==================================================

    private void listenToMyBudget() {

        if (myPlayerRef == null) {
            return;
        }


        budgetListener =
                myPlayerRef
                        .child("budget")
                        .addValueEventListener(

                                new ValueEventListener() {

                                    @Override
                                    public void onDataChange(
                                            @NonNull DataSnapshot snapshot
                                    ) {

                                        Long budget =
                                                snapshot.getValue(
                                                        Long.class
                                                );


                                        if (budget == null) {

                                            budget =
                                                    STARTING_BUDGET;
                                        }


                                        myBudget =
                                                budget;


                                        if (txtRemainingBudget != null) {

                                            txtRemainingBudget
                                                    .setText(
                                                            "$"
                                                                    + myBudget
                                                                    + "M"
                                                    );
                                        }
                                    }


                                    @Override
                                    public void onCancelled(
                                            @NonNull DatabaseError error
                                    ) {

                                    }
                                }
                        );
    }


    // ==================================================
    // DESTROY VIEW
    // ==================================================

    @Override
    public void onDestroyView() {


        // ==================================================
        // CANCEL LIVE DOT ANIMATION
        // ==================================================

        if (liveDot != null) {

            liveDot.animate()
                    .cancel();
        }


        // ==================================================
        // CANCEL PLAYER CARD ANIMATION
        // ==================================================

        if (playerCard != null) {

            playerCard.animate()
                    .cancel();
        }


        // ==================================================
        // REMOVE FIREBASE AUCTION LISTENER
        // ==================================================

        if (auctionRef != null &&
                auctionListener != null) {

            auctionRef
                    .removeEventListener(
                            auctionListener
                    );
        }


        // ==================================================
        // REMOVE BUDGET LISTENER
        // ==================================================

        if (myPlayerRef != null &&
                budgetListener != null) {

            myPlayerRef
                    .child("budget")
                    .removeEventListener(
                            budgetListener
                    );
        }


        // ==================================================
        // RELEASE HAMMER SOUND
        // ==================================================

        if (hammerSound != null) {

            try {

                if (hammerSound.isPlaying()) {

                    hammerSound.stop();
                }

            } catch (Exception ignored) {

            }


            hammerSound.release();

            hammerSound =
                    null;
        }


        // ==================================================
        // RELEASE CARD SOUND
        // ==================================================

        if (cardSound != null) {

            try {

                if (cardSound.isPlaying()) {

                    cardSound.stop();
                }

            } catch (Exception ignored) {

            }


            cardSound.release();

            cardSound =
                    null;
        }


        // ==================================================
        // CLEAR VIEW REFERENCES
        // ==================================================

        playerCard =
                null;

        liveDot =
                null;

        imgAuctionPlayer =
                null;

        txtAuctionPlayerName =
                null;

        txtPlayerPosition =
                null;

        txtCurrentBid =
                null;

        txtHighestBidder =
                null;

        txtRemainingBudget =
                null;

        txtSoldOverlay =
                null;

        etBidAmount =
                null;

        btnBid =
                null;

        btnGiveUp =
                null;


        super.onDestroyView();
    }
}