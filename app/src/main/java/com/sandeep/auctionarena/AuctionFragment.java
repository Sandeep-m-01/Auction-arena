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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    // Prevent multiple skip requests
    private boolean hostSkippingPlayer = false;

    private String lastHandledResultId = "";
    private String lastHandledSkipId = "";
    private String lastDisplayedPlayerId = "";


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
                            .getString("ROOM_CODE");


            playerId =
                    getArguments()
                            .getString("PLAYER_ID");


            playerName =
                    getArguments()
                            .getString("PLAYER_NAME");


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

        txtCurrentBid.setText("$0M");

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
        // LIVE DOT
        // ==================================================

        startLiveDotAnimation();


        // ==================================================
        // SOUNDS
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
        // VALIDATE ROOM / PLAYER
        // ==================================================

        if (roomCode == null ||
                roomCode.trim().isEmpty()) {

            return;

        }


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
        // BUDGET
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


            if (hasGivenUp) {

                return;

            }


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


            if (bidText.isEmpty()) {

                etBidAmount.setError(
                        "Enter bid amount in millions"
                );

                return;

            }


            long bidAmount;


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


            if (bidAmount <= 0) {

                etBidAmount.setError(
                        "Bid must be at least $1M"
                );

                return;

            }


            if (bidAmount > myBudget) {

                etBidAmount.setError(
                        "Your budget is $"
                                + myBudget
                                + "M"
                );

                return;

            }


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
    // INITIALIZE BUDGET
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

    private void placeBid(
            long newBid
    ) {

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


                        Long currentBid =
                                currentData
                                        .child("currentBid")
                                        .getValue(
                                                Long.class
                                        );


                        if (currentBid == null) {

                            currentBid = 0L;

                        }


                        String highestBidderId =
                                currentData
                                        .child("highestBidderId")
                                        .getValue(
                                                String.class
                                        );


                        // Highest bidder cannot bid again
                        if (playerId.equals(
                                highestBidderId
                        )) {

                            return Transaction.abort();

                        }


                        // New bid must be higher
                        if (newBid <= currentBid) {

                            return Transaction.abort();

                        }


                        // Cannot exceed budget
                        if (newBid > myBudget) {

                            return Transaction.abort();

                        }


                        currentData
                                .child("currentBid")
                                .setValue(
                                        newBid
                                );


                        currentData
                                .child("highestBidderId")
                                .setValue(
                                        playerId
                                );


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
         * Highest bidder is already winning.
         * They must wait.
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
                                // CURRENT PLAYER
                                // ==========================================

                                DataSnapshot currentPlayer =
                                        snapshot.child(
                                                "currentPlayer"
                                        );


                                Object footballerIdObject =
                                        currentPlayer
                                                .child("id")
                                                .getValue();


                                String footballerId =
                                        footballerIdObject != null

                                                ? String.valueOf(
                                                footballerIdObject
                                        )

                                                : null;


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
                                // PLAYER IMAGE
                                // ==========================================

                                loadPlayerImage(
                                        imageName
                                );


                                // ==========================================
                                // DETECT NEW PLAYER
                                // ==========================================

                                boolean isNewPlayer =

                                        footballerId != null

                                                &&

                                                !footballerId.equals(
                                                        lastDisplayedPlayerId
                                                );


                                if (isNewPlayer) {

                                    lastDisplayedPlayerId =
                                            footballerId;


                                    playCardSound();


                                    /*
                                     * If the sold/skip animation is not
                                     * already controlling the card,
                                     * animate normally from left.
                                     */

                                    if (!resultAnimationRunning) {

                                        animateNewPlayerCard();

                                    }

                                }


                                // ==========================================
                                // PLAYER NAME
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


                                txtCurrentBid
                                        .setText(

                                                "$"

                                                        + currentBid

                                                        + "M"

                                        );


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


                                amIHighestBidder =

                                        playerId != null

                                                &&

                                                playerId.equals(
                                                        highestBidderId
                                                );


                                // ==========================================
                                // GIVE UP STATUS
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
                                // HIGHEST BIDDER TEXT
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
                                // CHECK SKIPPED
                                // ==========================================

                                Boolean skipped =
                                        snapshot
                                                .child("skipped")
                                                .getValue(
                                                        Boolean.class
                                                );


                                if (Boolean.TRUE.equals(
                                        skipped
                                )) {

                                    handleSkippedPlayer(
                                            snapshot
                                    );

                                    return;

                                }


                                // ==========================================
                                // CHECK COMPLETED / SOLD
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
                                // UPDATE CONTROLS
                                // ==========================================

                                updateBiddingControls();


                                // ==========================================
                                // HOST CHECKS RESULT
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
    // LOAD PLAYER IMAGE
    // ==================================================

    private void loadPlayerImage(
            String imageName
    ) {

        if (!isAdded() ||
                imgAuctionPlayer == null) {

            return;

        }


        if (imageName == null ||
                imageName.trim().isEmpty()) {

            imgAuctionPlayer
                    .setImageDrawable(
                            null
                    );

            return;

        }


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

    }


    // ==================================================
    // NEW PLAYER CARD ANIMATION
    // ==================================================

    private void animateNewPlayerCard() {

        if (playerCard == null) {

            return;

        }


        playerCard
                .animate()
                .cancel();


        float startPosition =
                -(playerCard.getWidth() + 500f);


        playerCard.setTranslationX(
                startPosition
        );


        playerCard.setAlpha(
                0f
        );


        playerCard
                .animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(700)
                .start();

    }


    // ==================================================
    // PLAY CARD SOUND
    // ==================================================

    private void playCardSound() {

        if (cardSound == null) {

            return;

        }


        try {

            if (cardSound.isPlaying()) {

                cardSound.pause();

            }


            cardSound.seekTo(0);

            cardSound.start();

        } catch (Exception ignored) {

        }

    }


    // ==================================================
    // CHECK FOR AUCTION WINNER OR SKIP
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
                        // ALREADY SKIPPED?
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
                        // CURRENT PLAYER EXISTS?
                        // ==========================================

                        DataSnapshot currentPlayerSnapshot =
                                auctionSnapshot.child(
                                        "currentPlayer"
                                );


                        if (!currentPlayerSnapshot.exists()) {

                            return;

                        }


                        // ==========================================
                        // HIGHEST BIDDER
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
                        // COUNT ACTIVE PLAYERS
                        // ==========================================

                        DataSnapshot playersSnapshot =
                                roomSnapshot.child(
                                        "players"
                                );


                        int totalPlayers = 0;

                        int activePlayers = 0;

                        String remainingPlayerId =
                                null;


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


                            if (!Boolean.TRUE.equals(
                                    participantGaveUp
                            )) {

                                activePlayers++;

                                remainingPlayerId =
                                        id;

                            }

                        }


                        /*
                         * Wait until there are at least
                         * two players in the room.
                         */

                        if (totalPlayers < 2) {

                            return;

                        }


                        // ==========================================
                        // EVERYONE GAVE UP -> SKIP
                        // ==========================================

                        /*
                         * Example:
                         *
                         * Player A -> GIVE UP
                         * Player B -> GIVE UP
                         *
                         * activePlayers = 0
                         *
                         * Nobody wins.
                         * No budget deducted.
                         * Player is skipped.
                         */

                        if (activePlayers == 0) {

                            skipCurrentPlayer(
                                    auctionSnapshot
                            );

                            return;

                        }


                        // ==========================================
                        // ONE PLAYER LEFT -> SOLD
                        // ==========================================

                        /*
                         * There must be:
                         *
                         * 1 active player
                         * A valid highest bidder
                         * A bid greater than 0
                         *
                         * The remaining player must also
                         * be the highest bidder.
                         */

                        if (activePlayers == 1 &&
                                highestBidderId != null &&
                                currentBid > 0 &&
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

                        hostCheckingWinner = false;

                    }

                }

        );

    }


    // ==================================================
    // SKIP CURRENT PLAYER
    // ==================================================

    private void skipCurrentPlayer(
            DataSnapshot auctionSnapshot
    ) {

        if (!isHost ||
                auctionRef == null ||
                hostSkippingPlayer) {

            return;

        }


        DataSnapshot footballerSnapshot =
                auctionSnapshot.child(
                        "currentPlayer"
                );


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


        /*
         * Unique skip ID.
         *
         * Every device receives this ID and
         * runs the skip animation only once.
         */

        String skipId =
                footballerId

                        + "_SKIP_"

                        + System.currentTimeMillis();


        hostSkippingPlayer = true;


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

                    hostSkippingPlayer = false;

                });

    }


    // ==================================================
    // HANDLE SKIPPED PLAYER
    // ==================================================

    private void handleSkippedPlayer(
            DataSnapshot auctionSnapshot
    ) {

        String skipId =
                auctionSnapshot
                        .child("skipId")
                        .getValue(
                                String.class
                        );


        if (skipId == null) {

            return;

        }


        /*
         * Prevent the same skip animation
         * from running more than once.
         */

        if (skipId.equals(
                lastHandledSkipId
        )) {

            return;

        }


        lastHandledSkipId =
                skipId;


        // ==================================================
        // DISABLE CONTROLS
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
        // SHOW SKIPPED TEXT
        // ==================================================

        if (txtHighestBidder != null) {

            txtHighestBidder
                    .setText(
                            "PLAYER SKIPPED"
                    );

        }


        // ==================================================
        // ANIMATE SKIP
        // ==================================================

        animateSkippedPlayer();

    }


    // ==================================================
    // ANIMATE SKIPPED PLAYER
    // ==================================================

    private void animateSkippedPlayer() {

        if (playerCard == null ||
                resultAnimationRunning) {

            return;

        }


        resultAnimationRunning = true;


        /*
         * Skipped player leaves to the RIGHT.
         */

        playerCard
                .animate()
                .translationX(
                        playerCard.getWidth()
                                + 500f
                )
                .alpha(0f)
                .setDuration(700)
                .withEndAction(() -> {


                    /*
                     * Only the host changes Firebase.
                     */

                    if (isHost) {

                        loadNextPlayer();

                    }


                    /*
                     * Prepare card on left.
                     */

                    if (playerCard != null) {

                        playerCard.setTranslationX(

                                -playerCard.getWidth()
                                        - 500f

                        );


                        playerCard.setAlpha(
                                0f
                        );

                    }


                    /*
                     * Wait for Firebase to send
                     * the new player's data.
                     */

                    if (playerCard != null) {

                        playerCard.postDelayed(() -> {

                            if (playerCard == null) {

                                return;

                            }


                            playerCard
                                    .animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .setDuration(700)
                                    .withEndAction(() -> {

                                        resultAnimationRunning =
                                                false;

                                    })
                                    .start();

                        }, 300);

                    }

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


        DataSnapshot footballerSnapshot =
                auctionSnapshot.child(
                        "currentPlayer"
                );


        if (!footballerSnapshot.exists()) {

            return;

        }


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


        String footballerName =
                footballerSnapshot
                        .child("name")
                        .getValue(
                                String.class
                        );


        String position =
                footballerSnapshot
                        .child("position")
                        .getValue(
                                String.class
                        );


        String type =
                footballerSnapshot
                        .child("type")
                        .getValue(
                                String.class
                        );


        String image =
                footballerSnapshot
                        .child("image")
                        .getValue(
                                String.class
                        );


        String resultId =
                footballerId

                        + "_"

                        + System.currentTimeMillis();


        // ==================================================
        // RESULT
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


                    deductWinnerBudget(
                            winnerId,
                            winningBid
                    );


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
    // ADD PLAYER TO WINNER TEAM
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
    // LOAD NEXT PLAYER
    // ==================================================

    private void loadNextPlayer() {

        if (!isHost ||
                auctionRef == null ||
                !isAdded()) {

            return;

        }


        List<Player> players =
                PlayerLoader.loadPlayers(
                        requireContext()
                );


        if (players == null ||
                players.isEmpty()) {

            return;

        }


        Random random =
                new Random();


        Player selectedPlayer =
                players.get(

                        random.nextInt(
                                players.size()
                        )

                );


        // ==================================================
        // PLAYER DATA
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
        // RESET AUCTION
        // ==================================================

        Map<String, Object> updates =
                new HashMap<>();


        updates.put(
                "currentPlayer",
                playerData
        );


        updates.put(
                "currentBid",
                0
        );


        updates.put(
                "highestBidderId",
                null
        );


        updates.put(
                "highestBidderName",
                null
        );


        updates.put(
                "givenUpPlayers",
                null
        );


        updates.put(
                "completed",
                false
        );


        updates.put(
                "result",
                null
        );


        // Reset skip state
        updates.put(
                "skipped",
                false
        );


        updates.put(
                "skipId",
                null
        );


        auctionRef
                .updateChildren(
                        updates
                )
                .addOnCompleteListener(task -> {

                    /*
                     * Host can now check for another
                     * skip in the new auction.
                     */

                    hostSkippingPlayer =
                            false;

                });

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


        if (resultId.equals(
                lastHandledResultId
        )) {

            return;

        }


        lastHandledResultId =
                resultId;


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
        // DISABLE CONTROLS
        // ==================================================

        etBidAmount.setEnabled(
                false
        );


        btnBid.setEnabled(
                false
        );


        btnGiveUp.setEnabled(
                false
        );


        btnBid.setAlpha(
                0.4f
        );


        btnGiveUp.setAlpha(
                0.4f
        );


        // ==================================================
        // HAMMER SOUND
        // ==================================================

        playHammerSound();


        // ==================================================
        // SOLD OVERLAY
        // ==================================================

        txtSoldOverlay.setVisibility(
                View.VISIBLE
        );


        // ==================================================
        // RESULT TEXT
        // ==================================================

        if (footballerName != null &&
                winnerName != null) {


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


            txtHighestBidder
                    .setText(
                            resultText
                    );

        }


        // ==================================================
        // MOVE SOLD CARD
        // ==================================================

        if (playerCard != null) {

            playerCard.postDelayed(

                    this::animateSoldPlayer,

                    1200

            );

        }

    }


    // ==================================================
    // PLAY HAMMER
    // ==================================================

    private void playHammerSound() {

        if (hammerSound == null) {

            return;

        }


        try {

            hammerSound.seekTo(0);

            hammerSound.start();

        } catch (Exception ignored) {

        }

    }


    // ==================================================
    // SOLD PLAYER ANIMATION
    // ==================================================

    private void animateSoldPlayer() {

        if (playerCard == null ||
                resultAnimationRunning) {

            return;

        }


        resultAnimationRunning = true;


        playerCard
                .animate()
                .translationX(
                        playerCard.getWidth()
                                + 500f
                )
                .alpha(0f)
                .setDuration(700)
                .withEndAction(() -> {


                    /*
                     * Only host loads next player.
                     */

                    if (isHost) {

                        loadNextPlayer();

                    }


                    if (txtSoldOverlay != null) {

                        txtSoldOverlay
                                .setVisibility(
                                        View.GONE
                                );

                    }


                    if (playerCard != null) {

                        playerCard.setTranslationX(

                                -playerCard.getWidth()
                                        - 500f

                        );


                        playerCard.setAlpha(
                                0f
                        );

                    }


                    if (playerCard != null) {

                        playerCard.postDelayed(() -> {

                            if (playerCard == null) {

                                return;

                            }


                            playerCard
                                    .animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .setDuration(700)
                                    .withEndAction(() -> {

                                        resultAnimationRunning =
                                                false;

                                    })
                                    .start();

                        }, 300);

                    }

                })
                .start();

    }


    // ==================================================
    // UPDATE CONTROLS
    // ==================================================

    private void updateBiddingControls() {

        if (etBidAmount == null ||
                btnBid == null ||
                btnGiveUp == null) {

            return;

        }


        // ==========================================
        // PLAYER GAVE UP
        // ==========================================

        if (hasGivenUp) {

            etBidAmount.setEnabled(
                    false
            );


            btnBid.setEnabled(
                    false
            );


            btnGiveUp.setEnabled(
                    false
            );


            btnBid.setAlpha(
                    0.4f
            );


            btnGiveUp.setAlpha(
                    0.4f
            );


            return;

        }


        // ==========================================
        // HIGHEST BIDDER MUST WAIT
        // ==========================================

        if (amIHighestBidder) {

            etBidAmount.setEnabled(
                    false
            );


            btnBid.setEnabled(
                    false
            );


            btnGiveUp.setEnabled(
                    false
            );


            btnBid.setAlpha(
                    0.4f
            );


            btnGiveUp.setAlpha(
                    0.4f
            );


            return;

        }


        // ==========================================
        // CAN BID / GIVE UP
        // ==========================================

        etBidAmount.setEnabled(
                true
        );


        btnBid.setEnabled(
                true
        );


        btnGiveUp.setEnabled(
                true
        );


        btnBid.setAlpha(
                1f
        );


        btnGiveUp.setAlpha(
                1f
        );

    }


    // ==================================================
    // LISTEN TO BUDGET
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
        // CANCEL ANIMATIONS
        // ==================================================

        if (liveDot != null) {

            liveDot.animate()
                    .cancel();

        }


        if (playerCard != null) {

            playerCard.animate()
                    .cancel();

        }


        // ==================================================
        // REMOVE FIREBASE LISTENERS
        // ==================================================

        if (auctionRef != null &&
                auctionListener != null) {

            auctionRef
                    .removeEventListener(
                            auctionListener
                    );

        }


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

            hammerSound = null;

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

            cardSound = null;

        }


        // ==================================================
        // CLEAR VIEW REFERENCES
        // ==================================================

        playerCard = null;

        liveDot = null;

        imgAuctionPlayer = null;

        txtAuctionPlayerName = null;

        txtPlayerPosition = null;

        txtCurrentBid = null;

        txtHighestBidder = null;

        txtRemainingBudget = null;

        txtSoldOverlay = null;

        etBidAmount = null;

        btnBid = null;

        btnGiveUp = null;


        super.onDestroyView();

    }

}