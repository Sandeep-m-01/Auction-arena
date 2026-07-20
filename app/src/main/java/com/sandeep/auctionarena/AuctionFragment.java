package com.sandeep.auctionarena;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final long STARTING_BUDGET = 1000L;

    private static final int MAX_SQUAD_SIZE = 11;

    private static final String PHASE_AUCTION = "auction";

    private static final String PHASE_FORMATION = "formation";


    // ==================================================
    // ROOM / PLAYER
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

    private View btnBid;
    private View btnGiveUp;


    // ==================================================
    // FIREBASE
    // ==================================================

    private DatabaseReference roomRef;
    private DatabaseReference auctionRef;
    private DatabaseReference myPlayerRef;
    private DatabaseReference gamePhaseRef;

    private ValueEventListener auctionListener;
    private ValueEventListener budgetListener;
    private ValueEventListener teamListener;
    private ValueEventListener gamePhaseListener;


    // ==================================================
    // SOUND
    // ==================================================

    private MediaPlayer hammerSound;
    private MediaPlayer cardSound;


    // ==================================================
    // LOCAL STATE
    // ==================================================

    private long myBudget = STARTING_BUDGET;

    private boolean hasGivenUp = false;
    private boolean amIHighestBidder = false;
    private boolean mySquadFull = false;

    private boolean hostCheckingWinner = false;
    private boolean hostSkippingPlayer = false;

    private boolean resultAnimationRunning = false;

    private boolean auctionPhaseComplete = false;

    private String currentGamePhase = PHASE_AUCTION;

    private String lastShownPlayerId = null;
    private String lastHandledResultId = null;


    // ==================================================
    // CONSTRUCTOR
    // ==================================================

    public AuctionFragment() {

        // Required empty constructor.
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
                    getArguments().getString(
                            "ROOM_CODE"
                    );


            playerId =
                    getArguments().getString(
                            "PLAYER_ID"
                    );


            playerName =
                    getArguments().getString(
                            "PLAYER_NAME"
                    );


            isHost =
                    getArguments().getBoolean(
                            "IS_HOST",
                            false
                    );
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
        // CONNECT UI
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
        // VALIDATE DATA
        // ==================================================

        if (roomCode == null ||
                roomCode.trim().isEmpty() ||
                playerId == null ||
                playerId.trim().isEmpty()) {

            disableAuctionControls();

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
                roomRef.child(
                        "auction"
                );


        myPlayerRef =
                roomRef
                        .child("players")
                        .child(playerId);


        gamePhaseRef =
                roomRef.child(
                        "gamePhase"
                );


        // ==================================================
        // SOUNDS
        // ==================================================

        try {

            hammerSound =
                    MediaPlayer.create(
                            requireContext(),
                            R.raw.hammer
                    );

        } catch (Exception ignored) {

            hammerSound = null;
        }


        try {

            cardSound =
                    MediaPlayer.create(
                            requireContext(),
                            R.raw.card
                    );

        } catch (Exception ignored) {

            cardSound = null;
        }


        // ==================================================
        // LIVE DOT
        // ==================================================

        startLiveDotAnimation();


        // ==================================================
        // BUTTONS
        // ==================================================

        btnBid.setOnClickListener(v ->
                placeBid()
        );


        btnGiveUp.setOnClickListener(v ->
                giveUp()
        );


        // ==================================================
        // FIREBASE LISTENERS
        // ==================================================

        listenToGamePhase();

        listenToMyBudget();

        listenToMyTeam();

        listenToAuction();


        // ==================================================
        // HOST LOADS FIRST PLAYER
        // ==================================================

        if (isHost) {

            initializeFirstPlayer();
        }
    }


    // ==================================================
    // LIVE DOT ANIMATION
    // ==================================================

    private void startLiveDotAnimation() {

        if (liveDot == null) {

            return;
        }


        AlphaAnimation animation =
                new AlphaAnimation(
                        1f,
                        0.25f
                );


        animation.setDuration(
                700
        );


        animation.setRepeatMode(
                Animation.REVERSE
        );


        animation.setRepeatCount(
                Animation.INFINITE
        );


        liveDot.startAnimation(
                animation
        );
    }


    // ==================================================
    // LISTEN TO GAME PHASE
    // ==================================================

    private void listenToGamePhase() {

        if (gamePhaseRef == null) {

            return;
        }


        gamePhaseListener =
                gamePhaseRef.addValueEventListener(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                String phase =
                                        snapshot.getValue(
                                                String.class
                                        );


                                if (phase == null ||
                                        phase.trim().isEmpty()) {

                                    currentGamePhase =
                                            PHASE_AUCTION;

                                } else {

                                    currentGamePhase =
                                            phase;
                                }


                                auctionPhaseComplete =
                                        !PHASE_AUCTION.equals(
                                                currentGamePhase
                                        );


                                updateBiddingControls();
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
    // LISTEN TO MY TEAM
    // ==================================================

    private void listenToMyTeam() {

        if (myPlayerRef == null) {

            return;
        }


        teamListener =
                myPlayerRef
                        .child("team")
                        .addValueEventListener(

                                new ValueEventListener() {

                                    @Override
                                    public void onDataChange(
                                            @NonNull DataSnapshot snapshot
                                    ) {

                                        mySquadFull =
                                                snapshot
                                                        .getChildrenCount()
                                                        >= MAX_SQUAD_SIZE;


                                        updateBiddingControls();


                                        if (isHost) {

                                            checkIfAllSquadsComplete();
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
    // CHECK IF ALL SQUADS HAVE 11
    // ==================================================

    private void checkIfAllSquadsComplete() {

        if (!isHost ||
                roomRef == null ||
                auctionPhaseComplete) {

            return;
        }


        roomRef
                .child("players")
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                int managerCount = 0;

                                int completedManagers = 0;


                                for (DataSnapshot managerSnapshot :
                                        snapshot.getChildren()) {


                                    managerCount++;


                                    long teamSize =
                                            managerSnapshot
                                                    .child("team")
                                                    .getChildrenCount();


                                    if (teamSize >= MAX_SQUAD_SIZE) {

                                        completedManagers++;
                                    }
                                }


                                if (managerCount >= 2 &&
                                        completedManagers == managerCount) {

                                    moveToFormationPhase();
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
    // MOVE TO FORMATION
    // ==================================================

    private void moveToFormationPhase() {

        if (!isHost ||
                roomRef == null ||
                auctionPhaseComplete) {

            return;
        }


        auctionPhaseComplete = true;


        disableAuctionControls();


        Map<String, Object> updates =
                new HashMap<>();


        updates.put(
                "gamePhase",
                PHASE_FORMATION
        );


        updates.put(
                "auction/completed",
                true
        );


        updates.put(
                "auction/phaseComplete",
                true
        );


        roomRef.updateChildren(
                updates
        );
    }


    // ==================================================
    // PLACE BID
    // ==================================================

    private void placeBid() {

        if (auctionRef == null ||
                auctionPhaseComplete ||
                mySquadFull ||
                hasGivenUp ||
                amIHighestBidder) {

            return;
        }


        String bidText =
                etBidAmount
                        .getText()
                        .toString()
                        .trim();


        if (TextUtils.isEmpty(
                bidText
        )) {

            Toast.makeText(
                    requireContext(),
                    "Enter a bid amount",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        long bidAmount;


        try {

            bidAmount =
                    Long.parseLong(
                            bidText
                    );

        } catch (NumberFormatException exception) {

            Toast.makeText(
                    requireContext(),
                    "Enter a valid bid",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (bidAmount <= 0) {

            Toast.makeText(
                    requireContext(),
                    "Bid must be greater than 0",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (bidAmount > myBudget) {

            Toast.makeText(
                    requireContext(),
                    "Not enough budget",
                    Toast.LENGTH_SHORT
            ).show();

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
                                        .getValue(Boolean.class);


                        Boolean skipped =
                                currentData
                                        .child("skipped")
                                        .getValue(Boolean.class);


                        if (Boolean.TRUE.equals(completed) ||
                                Boolean.TRUE.equals(skipped)) {

                            return Transaction.abort();
                        }


                        Long currentBid =
                                currentData
                                        .child("currentBid")
                                        .getValue(Long.class);


                        if (currentBid == null) {

                            currentBid = 0L;
                        }


                        if (bidAmount <= currentBid) {

                            return Transaction.abort();
                        }


                        Boolean gaveUp =
                                currentData
                                        .child("givenUpPlayers")
                                        .child(playerId)
                                        .getValue(Boolean.class);


                        if (Boolean.TRUE.equals(gaveUp)) {

                            return Transaction.abort();
                        }


                        currentData
                                .child("currentBid")
                                .setValue(
                                        bidAmount
                                );


                        currentData
                                .child("highestBidderId")
                                .setValue(
                                        playerId
                                );


                        currentData
                                .child("highestBidderName")
                                .setValue(
                                        playerName != null
                                                ? playerName
                                                : playerId
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

                        if (!committed &&
                                isAdded()) {

                            Toast.makeText(
                                    requireContext(),
                                    "Bid must be higher than the current bid",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }


                        if (committed &&
                                etBidAmount != null) {

                            etBidAmount.setText(
                                    ""
                            );
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
                auctionPhaseComplete ||
                mySquadFull ||
                hasGivenUp ||
                amIHighestBidder) {

            return;
        }


        auctionRef
                .child("givenUpPlayers")
                .child(playerId)
                .setValue(
                        true
                );
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

                                if (!isAdded() ||
                                        auctionPhaseComplete) {

                                    return;
                                }


                                // ==========================================
                                // CURRENT FOOTBALLER
                                // ==========================================

                                DataSnapshot currentPlayerSnapshot =
                                        snapshot.child(
                                                "currentPlayer"
                                        );


                                if (currentPlayerSnapshot.exists()) {


                                    Object idObject =
                                            currentPlayerSnapshot
                                                    .child("id")
                                                    .getValue();


                                    String footballerId =
                                            idObject != null
                                                    ? String.valueOf(idObject)
                                                    : "";


                                    String name =
                                            currentPlayerSnapshot
                                                    .child("name")
                                                    .getValue(String.class);


                                    String position =
                                            currentPlayerSnapshot
                                                    .child("position")
                                                    .getValue(String.class);


                                    String image =
                                            currentPlayerSnapshot
                                                    .child("image")
                                                    .getValue(String.class);


                                    if (txtAuctionPlayerName != null) {

                                        txtAuctionPlayerName.setText(
                                                name != null
                                                        ? name
                                                        : ""
                                        );
                                    }


                                    if (txtPlayerPosition != null) {

                                        txtPlayerPosition.setText(
                                                position != null
                                                        ? position
                                                        : ""
                                        );
                                    }


                                    loadPlayerImage(
                                            image
                                    );


                                    // ======================================
                                    // NEW FOOTBALLER
                                    // ======================================

                                    if (!footballerId.equals(
                                            lastShownPlayerId
                                    )) {

                                        lastShownPlayerId =
                                                footballerId;


                                        lastHandledResultId =
                                                null;


                                        resultAnimationRunning =
                                                false;


                                        animateNewPlayerCard();

                                        playCardSound();
                                    }
                                }


                                // ==========================================
                                // CURRENT BID
                                // ==========================================

                                Long currentBid =
                                        snapshot
                                                .child("currentBid")
                                                .getValue(Long.class);


                                if (currentBid == null) {

                                    currentBid = 0L;
                                }


                                if (txtCurrentBid != null) {

                                    txtCurrentBid.setText(
                                            "$"
                                                    + currentBid
                                                    + "M"
                                    );
                                }


                                // ==========================================
                                // HIGHEST BIDDER
                                // ==========================================

                                String highestBidderId =
                                        snapshot
                                                .child("highestBidderId")
                                                .getValue(String.class);


                                String highestBidderName =
                                        snapshot
                                                .child("highestBidderName")
                                                .getValue(String.class);


                                amIHighestBidder =
                                        playerId.equals(
                                                highestBidderId
                                        );


                                if (txtHighestBidder != null) {

                                    if (highestBidderName == null ||
                                            highestBidderName
                                                    .trim()
                                                    .isEmpty()) {

                                        txtHighestBidder.setText(
                                                "NO BIDS YET"
                                        );

                                    } else {

                                        txtHighestBidder.setText(
                                                "HIGHEST: "
                                                        + highestBidderName
                                        );
                                    }
                                }


                                // ==========================================
                                // MY GIVE-UP STATE
                                // ==========================================

                                Boolean gaveUp =
                                        snapshot
                                                .child("givenUpPlayers")
                                                .child(playerId)
                                                .getValue(Boolean.class);


                                hasGivenUp =
                                        Boolean.TRUE.equals(
                                                gaveUp
                                        );


                                // ==========================================
                                // SKIPPED
                                // ==========================================

                                Boolean skipped =
                                        snapshot
                                                .child("skipped")
                                                .getValue(Boolean.class);


                                if (Boolean.TRUE.equals(
                                        skipped
                                )) {

                                    handleSkippedAuction(
                                            snapshot
                                    );

                                    return;
                                }


                                // ==========================================
                                // SOLD
                                // ==========================================

                                Boolean completed =
                                        snapshot
                                                .child("completed")
                                                .getValue(Boolean.class);


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
                                // ACTIVE AUCTION
                                // ==========================================

                                updateBiddingControls();


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

            imgAuctionPlayer.setImageDrawable(
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

            imgAuctionPlayer.setImageResource(
                    imageResource
            );

        } else {

            imgAuctionPlayer.setImageDrawable(
                    null
            );
        }
    }


    // ==================================================
    // NEW PLAYER ANIMATION
    // ==================================================

    private void animateNewPlayerCard() {

        if (playerCard == null) {

            return;
        }


        playerCard
                .animate()
                .cancel();


        playerCard.setTranslationX(
                -(playerCard.getWidth()
                        + 500f)
        );


        playerCard.setAlpha(
                0f
        );


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


            cardSound.seekTo(
                    0
            );


            cardSound.start();

        } catch (Exception ignored) {

        }
    }


    // ==================================================
    // CHECK FOR WINNER / SKIP
    //
    // FIXED GIVE-UP LOGIC:
    //
    // A bids + B gives up
    // -> A wins
    //
    // A gives up + B has not bid
    // -> WAIT for B
    // -> DO NOT SKIP
    //
    // A and B both give up with no valid bid
    // -> SKIP
    // ==================================================

    private void checkForAuctionWinner() {

        if (!isHost ||
                roomRef == null ||
                auctionRef == null ||
                auctionPhaseComplete ||
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


                        if (auctionPhaseComplete) {

                            return;
                        }


                        DataSnapshot auctionSnapshot =
                                roomSnapshot.child(
                                        "auction"
                                );


                        Boolean completed =
                                auctionSnapshot
                                        .child("completed")
                                        .getValue(Boolean.class);


                        if (Boolean.TRUE.equals(
                                completed
                        )) {

                            return;
                        }


                        Boolean skipped =
                                auctionSnapshot
                                        .child("skipped")
                                        .getValue(Boolean.class);


                        if (Boolean.TRUE.equals(
                                skipped
                        )) {

                            return;
                        }


                        DataSnapshot currentPlayerSnapshot =
                                auctionSnapshot.child(
                                        "currentPlayer"
                                );


                        if (!currentPlayerSnapshot.exists()) {

                            return;
                        }


                        String highestBidderId =
                                auctionSnapshot
                                        .child("highestBidderId")
                                        .getValue(String.class);


                        String highestBidderName =
                                auctionSnapshot
                                        .child("highestBidderName")
                                        .getValue(String.class);


                        Long currentBid =
                                auctionSnapshot
                                        .child("currentBid")
                                        .getValue(Long.class);


                        if (currentBid == null) {

                            currentBid = 0L;
                        }


                        DataSnapshot playersSnapshot =
                                roomSnapshot.child(
                                        "players"
                                );


                        int eligibleManagers = 0;

                        int activeManagers = 0;

                        String remainingManagerId = null;


                        for (DataSnapshot participant :
                                playersSnapshot.getChildren()) {


                            String id =
                                    participant.getKey();


                            if (id == null) {

                                continue;
                            }


                            long teamSize =
                                    participant
                                            .child("team")
                                            .getChildrenCount();


                            // Manager already has full squad.
                            if (teamSize >= MAX_SQUAD_SIZE) {

                                continue;
                            }


                            eligibleManagers++;


                            Boolean participantGaveUp =
                                    auctionSnapshot
                                            .child("givenUpPlayers")
                                            .child(id)
                                            .getValue(Boolean.class);


                            if (!Boolean.TRUE.equals(
                                    participantGaveUp
                            )) {

                                activeManagers++;

                                remainingManagerId =
                                        id;
                            }
                        }


                        // ==================================================
                        // EVERYONE HAS 11
                        // ==================================================

                        if (eligibleManagers == 0) {

                            moveToFormationPhase();

                            return;
                        }


                        // ==================================================
                        // TWO OR MORE ACTIVE MANAGERS
                        //
                        // Keep bidding.
                        // ==================================================

                        if (activeManagers > 1) {

                            return;
                        }


                        // ==================================================
                        // EXACTLY ONE ACTIVE MANAGER
                        // ==================================================

                        if (activeManagers == 1) {


                            // If remaining manager is highest bidder,
                            // they win immediately.

                            if (highestBidderId != null &&
                                    highestBidderId.equals(
                                            remainingManagerId
                                    ) &&
                                    currentBid > 0) {


                                finishAuction(
                                        highestBidderId,
                                        highestBidderName,
                                        currentBid,
                                        auctionSnapshot
                                );
                            }


                            // IMPORTANT:
                            //
                            // If remaining manager has NOT bid yet,
                            // do nothing.
                            //
                            // This fixes the bug where one GIVE UP
                            // immediately skipped the footballer.

                            return;
                        }


                        // ==================================================
                        // ZERO ACTIVE MANAGERS
                        // ==================================================

                        if (activeManagers == 0) {


                            // If there is still a valid highest bid,
                            // award the footballer to that bidder.

                            if (highestBidderId != null &&
                                    currentBid > 0) {


                                DataSnapshot highestBidderSnapshot =
                                        playersSnapshot.child(
                                                highestBidderId
                                        );


                                if (highestBidderSnapshot.exists()) {


                                    long teamSize =
                                            highestBidderSnapshot
                                                    .child("team")
                                                    .getChildrenCount();


                                    if (teamSize <
                                            MAX_SQUAD_SIZE) {


                                        finishAuction(
                                                highestBidderId,
                                                highestBidderName,
                                                currentBid,
                                                auctionSnapshot
                                        );


                                        return;
                                    }
                                }
                            }


                            // Nobody has a valid bid.
                            // Everyone gave up.
                            // Skip footballer.

                            skipCurrentPlayer(
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

    private void skipCurrentPlayer(
            DataSnapshot auctionSnapshot
    ) {

        if (!isHost ||
                auctionRef == null ||
                hostSkippingPlayer ||
                auctionPhaseComplete) {

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


        hostSkippingPlayer =
                true;


        String skipId =
                footballerId
                        + "_"
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
                .addOnFailureListener(error -> {

                    hostSkippingPlayer =
                            false;

                });
    }


    // ==================================================
    // HANDLE SKIPPED AUCTION
    // ==================================================

    private void handleSkippedAuction(
            DataSnapshot auctionSnapshot
    ) {

        if (auctionPhaseComplete) {

            return;
        }


        String skipId =
                auctionSnapshot
                        .child("skipId")
                        .getValue(
                                String.class
                        );


        if (skipId == null) {

            return;
        }


        String handledSkipId =
                "SKIP_"
                        + skipId;


        // Prevent same skip being handled repeatedly.
        if (handledSkipId.equals(
                lastHandledResultId
        )) {

            return;
        }


        lastHandledResultId =
                handledSkipId;


        disableAuctionControls();


        if (txtHighestBidder != null) {

            txtHighestBidder.setText(
                    "NO SALE"
            );
        }


        // ==================================================
        // HOST LOADS NEXT PLAYER
        //
        // The next player is loaded from Firebase.
        // This is not dependent on an animation finishing.
        // ==================================================

        if (isHost &&
                playerCard != null) {


            playerCard.postDelayed(

                    () -> {

                        if (!auctionPhaseComplete &&
                                isAdded()) {

                            loadNextPlayer();
                        }

                    },

                    1000
            );


        } else if (isHost) {


            loadNextPlayer();
        }
    }


    // ==================================================
    // FINISH CURRENT AUCTION
    // ==================================================

    private void finishAuction(
            String winnerId,
            String winnerName,
            long winningBid,
            DataSnapshot auctionSnapshot
    ) {

        if (!isHost ||
                roomRef == null ||
                auctionRef == null ||
                auctionPhaseComplete) {

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


        // ==================================================
        // UNIQUE RESULT ID
        //
        // Used by both phones to make sure the SOLD result
        // is only shown once.
        // ==================================================

        String resultId =
                footballerId
                        + "_"
                        + System.currentTimeMillis();


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
                winnerName != null
                        ? winnerName
                        : winnerId
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
        // MARK CURRENT AUCTION COMPLETED
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


                    // ======================================
                    // DEDUCT WINNING BID
                    // ======================================

                    deductWinnerBudget(
                            winnerId,
                            winningBid
                    );


                    // ======================================
                    // ADD PLAYER TO WINNER'S TEAM
                    // ======================================

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


        // Using footballerId as the Firebase key prevents
        // the same footballer from being added twice.

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

        if (!isHost ||
                auctionRef == null ||
                auctionPhaseComplete) {

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

                                if (auctionPhaseComplete ||
                                        snapshot.exists()) {

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
                roomRef == null ||
                auctionRef == null ||
                auctionPhaseComplete ||
                !isAdded()) {

            hostSkippingPlayer =
                    false;

            return;
        }


        // ==================================================
        // CHECK IF EVERY MANAGER ALREADY HAS 11
        // ==================================================

        roomRef
                .child("players")
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot playersSnapshot
                            ) {

                                if (!isAdded() ||
                                        auctionPhaseComplete) {

                                    hostSkippingPlayer =
                                            false;

                                    return;
                                }


                                int managerCount =
                                        0;


                                int completedManagers =
                                        0;


                                for (DataSnapshot manager :
                                        playersSnapshot.getChildren()) {


                                    managerCount++;


                                    long teamSize =
                                            manager
                                                    .child("team")
                                                    .getChildrenCount();


                                    if (teamSize >=
                                            MAX_SQUAD_SIZE) {

                                        completedManagers++;
                                    }
                                }


                                // ==========================================
                                // ALL MANAGERS HAVE 11
                                // ==========================================

                                if (managerCount >= 2 &&
                                        completedManagers ==
                                                managerCount) {


                                    hostSkippingPlayer =
                                            false;


                                    moveToFormationPhase();


                                    return;
                                }


                                // ==========================================
                                // LOAD AN UNUSED FOOTBALLER
                                // ==========================================

                                selectNextUnusedPlayer();
                            }


                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                hostSkippingPlayer =
                                        false;
                            }
                        }
                );
    }


    // ==================================================
    // SELECT NEXT UNUSED PLAYER
    // ==================================================

    private void selectNextUnusedPlayer() {

        if (!isHost ||
                roomRef == null ||
                auctionRef == null ||
                auctionPhaseComplete ||
                !isAdded()) {

            hostSkippingPlayer =
                    false;

            return;
        }


        // ==================================================
        // LOAD ALL PLAYERS FROM YOUR LOCAL PLAYER LOADER
        // ==================================================

        List<Player> allPlayers =
                PlayerLoader.loadPlayers(
                        requireContext()
                );


        if (allPlayers == null ||
                allPlayers.isEmpty()) {

            hostSkippingPlayer =
                    false;

            return;
        }


        // ==================================================
        // GET USED PLAYER IDS
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
                                        auctionPhaseComplete ||
                                        roomRef == null ||
                                        auctionRef == null) {

                                    hostSkippingPlayer =
                                            false;

                                    return;
                                }


                                Set<String> usedPlayerIds =
                                        new HashSet<>();


                                for (DataSnapshot usedPlayer :
                                        snapshot.getChildren()) {


                                    Boolean used =
                                            usedPlayer.getValue(
                                                    Boolean.class
                                            );


                                    String id =
                                            usedPlayer.getKey();


                                    if (Boolean.TRUE.equals(
                                            used
                                    ) &&
                                            id != null) {


                                        usedPlayerIds.add(
                                                id
                                        );
                                    }
                                }


                                // ==================================================
                                // BUILD AVAILABLE PLAYER LIST
                                // ==================================================

                                List<Player> availablePlayers =
                                        new ArrayList<>();


                                for (Player player :
                                        allPlayers) {


                                    String id =
                                            String.valueOf(
                                                    player.getId()
                                            );


                                    if (!usedPlayerIds.contains(
                                            id
                                    )) {


                                        availablePlayers.add(
                                                player
                                        );
                                    }
                                }


                                // ==================================================
                                // NO PLAYERS LEFT
                                // ==================================================

                                if (availablePlayers.isEmpty()) {


                                    hostSkippingPlayer =
                                            false;


                                    Toast.makeText(
                                            requireContext(),
                                            "No more footballers available",
                                            Toast.LENGTH_LONG
                                    ).show();


                                    return;
                                }


                                // ==================================================
                                // PICK RANDOM UNUSED PLAYER
                                // ==================================================

                                Player selectedPlayer =
                                        availablePlayers.get(

                                                new Random()
                                                        .nextInt(
                                                                availablePlayers.size()
                                                        )

                                        );


                                String selectedPlayerId =
                                        String.valueOf(
                                                selectedPlayer.getId()
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
                                // ATOMIC FIREBASE UPDATE
                                //
                                // This:
                                //
                                // 1. Marks footballer as used.
                                // 2. Sets new current footballer.
                                // 3. Resets bid.
                                // 4. Resets highest bidder.
                                // 5. Resets GIVE UP states.
                                // 6. Clears previous result.
                                // ==================================================

                                Map<String, Object> updates =
                                        new HashMap<>();


                                updates.put(
                                        "usedPlayers/"
                                                + selectedPlayerId,
                                        true
                                );


                                updates.put(
                                        "auction/currentPlayer",
                                        playerData
                                );


                                updates.put(
                                        "auction/currentBid",
                                        0
                                );


                                updates.put(
                                        "auction/highestBidderId",
                                        null
                                );


                                updates.put(
                                        "auction/highestBidderName",
                                        null
                                );


                                updates.put(
                                        "auction/givenUpPlayers",
                                        null
                                );


                                updates.put(
                                        "auction/completed",
                                        false
                                );


                                updates.put(
                                        "auction/result",
                                        null
                                );


                                updates.put(
                                        "auction/skipped",
                                        false
                                );


                                updates.put(
                                        "auction/skipId",
                                        null
                                );


                                // ==================================================
                                // APPLY NEW AUCTION
                                // ==================================================

                                roomRef
                                        .updateChildren(
                                                updates
                                        )
                                        .addOnCompleteListener(task -> {


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
                            }
                        }
                );
    }


    // ==================================================
    // HANDLE COMPLETED AUCTION
    // ==================================================

    private void handleCompletedAuction(
            DataSnapshot resultSnapshot
    ) {

        if (!resultSnapshot.exists() ||
                auctionPhaseComplete) {

            return;
        }


        String resultId =
                resultSnapshot
                        .child("resultId")
                        .getValue(
                                String.class
                        );


        if (resultId == null ||
                resultId.equals(
                        lastHandledResultId
                )) {

            return;
        }


        // Prevent the same SOLD result from being handled
        // multiple times on this device.

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
        // LOCK CONTROLS
        // ==================================================

        disableAuctionControls();


        // ==================================================
        // HAMMER SOUND
        // ==================================================

        playHammerSound();


        // ==================================================
        // SOLD OVERLAY
        // ==================================================

        if (txtSoldOverlay != null) {

            txtSoldOverlay.setVisibility(
                    View.VISIBLE
            );
        }


        // ==================================================
        // RESULT TEXT
        // ==================================================

        if (txtHighestBidder != null &&
                footballerName != null &&
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


            txtHighestBidder.setText(
                    resultText
            );
        }


        // ==================================================
        // HOST LOADS NEXT PLAYER AFTER 2 SECONDS
        //
        // IMPORTANT:
        // Firebase progression is independent of animation.
        // ==================================================

        if (isHost &&
                playerCard != null) {


            playerCard.postDelayed(

                    () -> {

                        if (!auctionPhaseComplete &&
                                isAdded()) {

                            loadNextPlayer();
                        }

                    },

                    2000
            );


        } else if (isHost) {


            loadNextPlayer();
        }


        // ==================================================
        // VISUAL SOLD ANIMATION
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

            if (hammerSound.isPlaying()) {

                hammerSound.pause();
            }


            hammerSound.seekTo(
                    0
            );


            hammerSound.start();

        } catch (Exception ignored) {

        }
    }


    // ==================================================
    // SOLD PLAYER ANIMATION
    //
    // VISUAL ONLY
    //
    // Loading the next player does NOT depend on this.
    // ==================================================

    private void animateSoldPlayer() {

        if (playerCard == null ||
                resultAnimationRunning ||
                auctionPhaseComplete) {

            return;
        }


        resultAnimationRunning =
                true;


        playerCard
                .animate()
                .cancel();


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


                    if (txtSoldOverlay != null) {

                        txtSoldOverlay.setVisibility(
                                View.GONE
                        );
                    }


                    resultAnimationRunning =
                            false;

                })
                .start();
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

                                            txtRemainingBudget.setText(

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
    // UPDATE BIDDING CONTROLS
    // ==================================================

    private void updateBiddingControls() {

        if (auctionPhaseComplete ||
                mySquadFull ||
                hasGivenUp ||
                amIHighestBidder) {


            disableAuctionControls();

            return;
        }


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
    // DISABLE AUCTION CONTROLS
    // ==================================================

    private void disableAuctionControls() {

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
    // CLEANUP
    // ==================================================

    @Override
    public void onDestroyView() {

        super.onDestroyView();


        // ==================================================
        // REMOVE FIREBASE LISTENERS
        // ==================================================

        if (auctionRef != null &&
                auctionListener != null) {

            auctionRef.removeEventListener(
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


        if (myPlayerRef != null &&
                teamListener != null) {

            myPlayerRef
                    .child("team")
                    .removeEventListener(
                            teamListener
                    );
        }


        if (gamePhaseRef != null &&
                gamePhaseListener != null) {

            gamePhaseRef.removeEventListener(
                    gamePhaseListener
            );
        }


        // ==================================================
        // STOP ANIMATIONS
        // ==================================================

        if (playerCard != null) {

            playerCard
                    .animate()
                    .cancel();
        }


        if (liveDot != null) {

            liveDot.clearAnimation();
        }


        // ==================================================
        // RELEASE HAMMER SOUND
        // ==================================================

        if (hammerSound != null) {


            try {

                hammerSound.release();

            } catch (Exception ignored) {

            }


            hammerSound =
                    null;
        }


        // ==================================================
        // RELEASE CARD SOUND
        // ==================================================

        if (cardSound != null) {


            try {

                cardSound.release();

            } catch (Exception ignored) {

            }


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
    }
}