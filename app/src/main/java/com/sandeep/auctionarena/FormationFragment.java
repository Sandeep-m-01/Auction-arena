package com.sandeep.auctionarena;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FormationFragment extends Fragment {

    // ==================================================
    // CONFIG
    // ==================================================

    private static final String DATABASE_URL =
            "https://auctionarena-c777d-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final long STARTING_BUDGET = 1000L;

    private static final int MAX_FORMATION_PLAYERS = 11;


    // ==================================================
    // ROOM / PLAYER
    // ==================================================

    private String roomCode;
    private String playerId;


    // ==================================================
    // UI
    // ==================================================

    private FrameLayout formationContainer;

    private TextView txtFormationBudget;
    private TextView txtFormationPlayerCount;
    private TextView txtEmptyFormation;

    private View btnAddFormationPlayer;


    // ==================================================
    // FIREBASE
    // ==================================================

    private DatabaseReference myPlayerRef;
    private DatabaseReference teamRef;
    private DatabaseReference formationRef;

    private ValueEventListener budgetListener;
    private ValueEventListener teamListener;
    private ValueEventListener formationListener;


    // ==================================================
    // LOCAL DATA
    // ==================================================

    private final List<TeamPlayer> ownedPlayers =
            new ArrayList<>();

    private final Set<String> formationPlayerIds =
            new HashSet<>();

    private int formationPlayerCount = 0;

    private boolean playerBeingDragged = false;


    // ==================================================
    // CONSTRUCTOR
    // ==================================================

    public FormationFragment() {
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
                    getArguments().getString(
                            "ROOM_CODE"
                    );

            playerId =
                    getArguments().getString(
                            "PLAYER_ID"
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
                R.layout.fragment_formation,
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


        formationContainer =
                view.findViewById(
                        R.id.formationContainer
                );


        txtFormationBudget =
                view.findViewById(
                        R.id.txtFormationBudget
                );


        txtFormationPlayerCount =
                view.findViewById(
                        R.id.txtFormationPlayerCount
                );


        txtEmptyFormation =
                view.findViewById(
                        R.id.txtEmptyFormation
                );


        btnAddFormationPlayer =
                view.findViewById(
                        R.id.btnAddFormationPlayer
                );


        // ==================================================
        // DEFAULT UI
        // ==================================================

        txtFormationBudget.setText(
                "$1000M"
        );


        txtFormationPlayerCount.setText(
                "0 / 11"
        );


        txtEmptyFormation.setVisibility(
                View.VISIBLE
        );


        // ==================================================
        // VALIDATE
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


        myPlayerRef =
                database
                        .getReference()
                        .child("rooms")
                        .child(roomCode)
                        .child("players")
                        .child(playerId);


        teamRef =
                myPlayerRef.child(
                        "team"
                );


        formationRef =
                myPlayerRef.child(
                        "formation"
                );


        // ==================================================
        // LISTENERS
        // ==================================================

        listenToBudget();

        listenToOwnedPlayers();

        listenToFormation();


        // ==================================================
        // ADD PLAYER
        // ==================================================

        btnAddFormationPlayer.setOnClickListener(v -> {

            showPlayerSelectionDialog();

        });
    }


    // ==================================================
    // LISTEN TO BUDGET
    // ==================================================

    private void listenToBudget() {

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

                                        if (!isAdded() ||
                                                txtFormationBudget == null) {

                                            return;
                                        }


                                        Long budget =
                                                snapshot.getValue(
                                                        Long.class
                                                );


                                        if (budget == null) {

                                            budget =
                                                    STARTING_BUDGET;
                                        }


                                        txtFormationBudget.setText(
                                                "$"
                                                        + budget
                                                        + "M"
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


    // ==================================================
    // LOAD OWNED PLAYERS
    // ==================================================

    private void listenToOwnedPlayers() {

        if (teamRef == null) {
            return;
        }


        teamListener =
                teamRef.addValueEventListener(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                ownedPlayers.clear();


                                for (DataSnapshot playerSnapshot :
                                        snapshot.getChildren()) {


                                    String id =
                                            playerSnapshot
                                                    .child("id")
                                                    .getValue(
                                                            String.class
                                                    );


                                    String name =
                                            playerSnapshot
                                                    .child("name")
                                                    .getValue(
                                                            String.class
                                                    );


                                    String position =
                                            playerSnapshot
                                                    .child("position")
                                                    .getValue(
                                                            String.class
                                                    );


                                    String type =
                                            playerSnapshot
                                                    .child("type")
                                                    .getValue(
                                                            String.class
                                                    );


                                    String image =
                                            playerSnapshot
                                                    .child("image")
                                                    .getValue(
                                                            String.class
                                                    );


                                    if (id == null) {

                                        id =
                                                playerSnapshot.getKey();
                                    }


                                    if (id == null) {
                                        continue;
                                    }


                                    TeamPlayer player =
                                            new TeamPlayer();


                                    player.id =
                                            id;


                                    player.name =
                                            name != null
                                                    ? name
                                                    : "PLAYER";


                                    player.position =
                                            position != null
                                                    ? position
                                                    : "";


                                    player.type =
                                            type != null
                                                    ? type
                                                    : "";


                                    player.image =
                                            image != null
                                                    ? image
                                                    : "";


                                    ownedPlayers.add(
                                            player
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
    // LISTEN TO FORMATION
    // ==================================================

    private void listenToFormation() {

        if (formationRef == null) {
            return;
        }


        formationListener =
                formationRef.addValueEventListener(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!isAdded() ||
                                        formationContainer == null) {

                                    return;
                                }


                                /*
                                 * IMPORTANT:
                                 *
                                 * Do not rebuild all cards while
                                 * the user is dragging one.
                                 *
                                 * This makes dragging much smoother.
                                 */

                                if (playerBeingDragged) {

                                    return;
                                }


                                removeFormationPlayerViews();


                                formationPlayerIds.clear();


                                formationPlayerCount =
                                        0;


                                for (DataSnapshot formationPlayer :
                                        snapshot.getChildren()) {


                                    String footballerId =
                                            formationPlayer
                                                    .child("playerId")
                                                    .getValue(
                                                            String.class
                                                    );


                                    String name =
                                            formationPlayer
                                                    .child("name")
                                                    .getValue(
                                                            String.class
                                                    );


                                    /*
                                     * formationPosition is separate
                                     * from the player's original
                                     * football position.
                                     */

                                    String formationPosition =
                                            formationPlayer
                                                    .child("formationPosition")
                                                    .getValue(
                                                            String.class
                                                    );


                                    /*
                                     * Backward compatibility:
                                     *
                                     * Existing Firebase formations
                                     * may only have "position".
                                     */

                                    if (formationPosition == null) {

                                        formationPosition =
                                                formationPlayer
                                                        .child("position")
                                                        .getValue(
                                                                String.class
                                                        );
                                    }


                                    String image =
                                            formationPlayer
                                                    .child("image")
                                                    .getValue(
                                                            String.class
                                                    );


                                    Double x =
                                            getDoubleValue(
                                                    formationPlayer
                                                            .child("x")
                                            );


                                    Double y =
                                            getDoubleValue(
                                                    formationPlayer
                                                            .child("y")
                                            );


                                    if (footballerId == null) {
                                        continue;
                                    }


                                    /*
                                     * Extra protection against
                                     * duplicate cards.
                                     */

                                    if (formationPlayerIds.contains(
                                            footballerId
                                    )) {

                                        continue;
                                    }


                                    formationPlayerIds.add(
                                            footballerId
                                    );


                                    createFormationPlayerView(
                                            footballerId,
                                            name,
                                            formationPosition,
                                            image,
                                            x != null
                                                    ? x.floatValue()
                                                    : 0f,
                                            y != null
                                                    ? y.floatValue()
                                                    : 0f
                                    );


                                    formationPlayerCount++;
                                }


                                updateFormationUI();
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
    // GET DOUBLE FROM FIREBASE
    // ==================================================

    private Double getDoubleValue(
            DataSnapshot snapshot
    ) {

        Object value =
                snapshot.getValue();


        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
        }


        return null;
    }



    // ==================================================
    // SHOW PLAYER SELECTION
    // ==================================================

    private void showPlayerSelectionDialog() {

        if (!isAdded() ||
                formationRef == null) {

            return;
        }


        if (formationPlayerCount >=
                MAX_FORMATION_PLAYERS) {


            new AlertDialog.Builder(
                    requireContext()
            )
                    .setTitle(
                            "Formation Full"
                    )
                    .setMessage(
                            "You already have 11 players on the field."
                    )
                    .setPositiveButton(
                            "OK",
                            null
                    )
                    .show();


            return;
        }


        if (ownedPlayers.isEmpty()) {


            new AlertDialog.Builder(
                    requireContext()
            )
                    .setTitle(
                            "No Players"
                    )
                    .setMessage(
                            "Win players in the auction before adding them to your formation."
                    )
                    .setPositiveButton(
                            "OK",
                            null
                    )
                    .show();


            return;
        }


        formationRef
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (!isAdded()) {
                                    return;
                                }


                                List<TeamPlayer> availablePlayers =
                                        new ArrayList<>();


                                for (TeamPlayer player :
                                        ownedPlayers) {


                                    /*
                                     * Firebase formation uses
                                     * player ID as the key.
                                     *
                                     * Therefore the same player
                                     * cannot be selected twice.
                                     */

                                    if (!snapshot
                                            .child(player.id)
                                            .exists()) {


                                        availablePlayers.add(
                                                player
                                        );
                                    }
                                }


                                if (availablePlayers.isEmpty()) {


                                    new AlertDialog.Builder(
                                            requireContext()
                                    )
                                            .setTitle(
                                                    "No Available Players"
                                            )
                                            .setMessage(
                                                    "All your available players are already on the field."
                                            )
                                            .setPositiveButton(
                                                    "OK",
                                                    null
                                            )
                                            .show();


                                    return;
                                }


                                String[] playerNames =
                                        new String[
                                                availablePlayers.size()
                                                ];


                                for (int i = 0;
                                     i < availablePlayers.size();
                                     i++) {


                                    TeamPlayer player =
                                            availablePlayers.get(
                                                    i
                                            );


                                    playerNames[i] =
                                            player.name
                                                    + " • "
                                                    + player.position;
                                }


                                new AlertDialog.Builder(
                                        requireContext()
                                )
                                        .setTitle(
                                                "Select Player"
                                        )
                                        .setItems(
                                                playerNames,
                                                (dialog, which) -> {


                                                    TeamPlayer selectedPlayer =
                                                            availablePlayers
                                                                    .get(
                                                                            which
                                                                    );


                                                    addPlayerToFormation(
                                                            selectedPlayer
                                                    );

                                                }
                                        )
                                        .setNegativeButton(
                                                "CANCEL",
                                                null
                                        )
                                        .show();
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
    // ADD PLAYER TO FORMATION
    // ==================================================

    private void addPlayerToFormation(
            TeamPlayer player
    ) {

        if (formationRef == null ||
                formationContainer == null ||
                player == null ||
                player.id == null) {

            return;
        }


        /*
         * Extra duplicate protection.
         *
         * If this player already exists,
         * do not overwrite/add another card.
         */

        formationRef
                .child(player.id)
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                if (snapshot.exists() ||
                                        formationContainer == null) {

                                    return;
                                }


                                float defaultX =
                                        Math.max(
                                                0,
                                                formationContainer
                                                        .getWidth()
                                                        / 2f
                                                        - dpToPx(40)
                                        );


                                float defaultY =
                                        Math.max(
                                                0,
                                                formationContainer
                                                        .getHeight()
                                                        / 2f
                                                        - dpToPx(50)
                                        );


                                /*
                                 * Calculate formation position
                                 * from where the card starts.
                                 */

                                String formationPosition =
                                        calculateFormationPosition(
                                                defaultX,
                                                defaultY,
                                                dpToPx(80),
                                                dpToPx(105)
                                        );


                                Map<String, Object> data =
                                        new HashMap<>();


                                data.put(
                                        "playerId",
                                        player.id
                                );


                                data.put(
                                        "name",
                                        player.name
                                );


                                /*
                                 * Keep original position.
                                 *
                                 * Example:
                                 * Messi original = RW
                                 */

                                data.put(
                                        "originalPosition",
                                        player.position
                                );


                                /*
                                 * Formation position changes
                                 * when the card moves.
                                 */

                                data.put(
                                        "formationPosition",
                                        formationPosition
                                );


                                data.put(
                                        "type",
                                        player.type
                                );


                                data.put(
                                        "image",
                                        player.image
                                );


                                data.put(
                                        "x",
                                        defaultX
                                );


                                data.put(
                                        "y",
                                        defaultY
                                );


                                formationRef
                                        .child(player.id)
                                        .setValue(
                                                data
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


    // ==================================================
    // CREATE PLAYER VIEW
    // ==================================================

    private void createFormationPlayerView(
            String footballerId,
            String name,
            String formationPosition,
            String imageName,
            float x,
            float y
    ) {

        if (formationContainer == null ||
                !isAdded()) {

            return;
        }


        // ==================================================
        // PLAYER CARD CONTAINER
        // ==================================================

        LinearLayout playerView =
                new LinearLayout(
                        requireContext()
                );


        playerView.setOrientation(
                LinearLayout.VERTICAL
        );


        playerView.setGravity(
                Gravity.CENTER
        );


        playerView.setTag(
                "formation_player"
        );


        FrameLayout.LayoutParams playerParams =
                new FrameLayout.LayoutParams(
                        dpToPx(80),
                        dpToPx(105)
                );


        playerView.setLayoutParams(
                playerParams
        );


        // ==================================================
        // PLAYER IMAGE
        // ==================================================

        ImageView imageView =
                new ImageView(
                        requireContext()
                );


        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        dpToPx(64),
                        dpToPx(64)
                );


        imageView.setLayoutParams(
                imageParams
        );


        imageView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );


        imageView.setAdjustViewBounds(
                true
        );


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

                imageView.setImageResource(
                        imageResource
                );
            }
        }


        playerView.addView(
                imageView
        );


        // ==================================================
        // PLAYER NAME
        // ==================================================

        TextView nameView =
                new TextView(
                        requireContext()
                );


        nameView.setText(
                getShortPlayerName(
                        name
                )
        );


        nameView.setTextColor(
                Color.WHITE
        );


        nameView.setTextSize(
                13
        );


        nameView.setGravity(
                Gravity.CENTER
        );


        nameView.setSingleLine(
                true
        );


        nameView.setTypeface(
                ResourcesCompat.getFont(
                        requireContext(),
                        R.font.bebas_neue
                )
        );


        nameView.setShadowLayer(
                4f,
                0f,
                2f,
                Color.BLACK
        );


        playerView.addView(
                nameView
        );


        // ==================================================
        // DYNAMIC POSITION
        // ==================================================

        TextView positionView =
                new TextView(
                        requireContext()
                );


        positionView.setText(
                formationPosition != null
                        ? formationPosition.toUpperCase()
                        : "CM"
        );


        positionView.setTextColor(
                Color.parseColor(
                        "#D4AF37"
                )
        );


        positionView.setTextSize(
                11
        );


        positionView.setGravity(
                Gravity.CENTER
        );


        positionView.setTypeface(
                ResourcesCompat.getFont(
                        requireContext(),
                        R.font.bebas_neue
                )
        );


        playerView.addView(
                positionView
        );


        // ==================================================
        // ADD CARD TO FIELD
        // ==================================================

        formationContainer.addView(
                playerView
        );


        // ==================================================
        // RESTORE SAVED POSITION
        // ==================================================

        playerView.post(() -> {

            if (formationContainer == null) {
                return;
            }


            float maxX =
                    Math.max(
                            0,
                            formationContainer.getWidth()
                                    - playerView.getWidth()
                    );


            float maxY =
                    Math.max(
                            0,
                            formationContainer.getHeight()
                                    - playerView.getHeight()
                    );


            playerView.setX(
                    Math.max(
                            0,
                            Math.min(
                                    x,
                                    maxX
                            )
                    )
            );


            playerView.setY(
                    Math.max(
                            0,
                            Math.min(
                                    y,
                                    maxY
                            )
                    )
            );
        });


        // ==================================================
        // ENABLE DRAGGING
        // ==================================================

        makePlayerDraggable(
                playerView,
                positionView,
                footballerId
        );
    }


    // ==================================================
    // MAKE PLAYER DRAGGABLE
    // ==================================================

    private void makePlayerDraggable(
            View playerView,
            TextView positionView,
            String footballerId
    ) {

        playerView.setOnTouchListener(

                new View.OnTouchListener() {

                    private float touchOffsetX;
                    private float touchOffsetY;

                    private float downRawX;
                    private float downRawY;

                    private boolean moved;


                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {

                        if (formationContainer == null) {
                            return false;
                        }


                        switch (event.getActionMasked()) {


                            // ==================================================
                            // START DRAG
                            // ==================================================

                            case MotionEvent.ACTION_DOWN:


                                downRawX =
                                        event.getRawX();


                                downRawY =
                                        event.getRawY();


                                touchOffsetX =
                                        view.getX()
                                                - event.getRawX();


                                touchOffsetY =
                                        view.getY()
                                                - event.getRawY();


                                moved =
                                        false;


                                playerBeingDragged =
                                        true;


                                /*
                                 * Stop ViewPager from stealing
                                 * horizontal drag gestures.
                                 */

                                setViewPagerSwipeEnabled(
                                        false
                                );


                                /*
                                 * Ask parent views not to
                                 * intercept this touch.
                                 */

                                if (view.getParent() != null) {

                                    view.getParent()
                                            .requestDisallowInterceptTouchEvent(
                                                    true
                                            );
                                }


                                view.bringToFront();


                                /*
                                 * Small visual feedback.
                                 */

                                view.animate()
                                        .scaleX(1.08f)
                                        .scaleY(1.08f)
                                        .setDuration(100)
                                        .start();


                                return true;


                            // ==================================================
                            // MOVE CARD
                            // ==================================================

                            case MotionEvent.ACTION_MOVE:


                                float distanceX =
                                        Math.abs(
                                                event.getRawX()
                                                        - downRawX
                                        );


                                float distanceY =
                                        Math.abs(
                                                event.getRawY()
                                                        - downRawY
                                        );


                                /*
                                 * Small movement threshold.
                                 *
                                 * Prevents tiny finger movement
                                 * from counting as a full drag.
                                 */

                                if (distanceX > dpToPx(3) ||
                                        distanceY > dpToPx(3)) {

                                    moved =
                                            true;
                                }


                                float newX =
                                        event.getRawX()
                                                + touchOffsetX;


                                float newY =
                                        event.getRawY()
                                                + touchOffsetY;


                                float maxX =
                                        Math.max(
                                                0,
                                                formationContainer
                                                        .getWidth()
                                                        - view.getWidth()
                                        );


                                float maxY =
                                        Math.max(
                                                0,
                                                formationContainer
                                                        .getHeight()
                                                        - view.getHeight()
                                        );


                                // Keep card inside field

                                newX =
                                        Math.max(
                                                0,
                                                Math.min(
                                                        newX,
                                                        maxX
                                                )
                                        );


                                newY =
                                        Math.max(
                                                0,
                                                Math.min(
                                                        newY,
                                                        maxY
                                                )
                                        );


                                /*
                                 * Direct X/Y updates give
                                 * smoother dragging than
                                 * repeatedly running animations.
                                 */

                                view.setX(
                                        newX
                                );


                                view.setY(
                                        newY
                                );


                                // ==========================================
                                // UPDATE POSITION LIVE
                                // ==========================================

                                String livePosition =
                                        calculateFormationPosition(
                                                newX,
                                                newY,
                                                view.getWidth(),
                                                view.getHeight()
                                        );


                                if (positionView != null) {

                                    positionView.setText(
                                            livePosition
                                    );
                                }


                                return true;


                            // ==================================================
                            // RELEASE CARD
                            // ==================================================

                            case MotionEvent.ACTION_UP:


                                finishPlayerDrag(
                                        view,
                                        positionView,
                                        footballerId,
                                        moved
                                );


                                return true;


                            // ==================================================
                            // DRAG CANCELLED
                            // ==================================================

                            case MotionEvent.ACTION_CANCEL:


                                finishPlayerDrag(
                                        view,
                                        positionView,
                                        footballerId,
                                        moved
                                );


                                return true;


                            default:

                                return false;
                        }
                    }
                }
        );
    }


    // ==================================================
    // FINISH PLAYER DRAG
    // ==================================================

    private void finishPlayerDrag(
            View view,
            TextView positionView,
            String footballerId,
            boolean moved
    ) {


        // Restore card size

        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .start();


        // Allow parent touch handling again

        if (view.getParent() != null) {

            view.getParent()
                    .requestDisallowInterceptTouchEvent(
                            false
                    );
        }


        // Re-enable ViewPager swipe

        setViewPagerSwipeEnabled(
                true
        );


        if (moved) {


            String formationPosition =
                    calculateFormationPosition(
                            view.getX(),
                            view.getY(),
                            view.getWidth(),
                            view.getHeight()
                    );


            if (positionView != null) {

                positionView.setText(
                        formationPosition
                );
            }


            /*
             * Save only when finger is released.
             *
             * This avoids sending dozens of
             * Firebase writes during dragging.
             */

            savePlayerPosition(
                    footballerId,
                    view.getX(),
                    view.getY(),
                    formationPosition
            );
        }


        /*
         * Delay slightly so our Firebase listener
         * does not rebuild cards before the final
         * position write has completed.
         */

        view.postDelayed(
                () -> playerBeingDragged = false,
                150
        );
    }


    // ==================================================
    // CALCULATE FORMATION POSITION
    // ==================================================

    private String calculateFormationPosition(
            float cardX,
            float cardY,
            int cardWidth,
            int cardHeight
    ) {

        if (formationContainer == null ||
                formationContainer.getWidth() <= 0 ||
                formationContainer.getHeight() <= 0) {

            return "CM";
        }


        /*
         * Use the CENTER of the player card.
         */

        float centerX =
                cardX
                        + cardWidth / 2f;


        float centerY =
                cardY
                        + cardHeight / 2f;


        /*
         * Convert coordinates to percentages.
         *
         * xRatio:
         * 0.0 = far left
         * 1.0 = far right
         *
         * yRatio:
         * 0.0 = top
         * 1.0 = bottom
         *
         * IMPORTANT:
         *
         * This assumes your attacking goal
         * is at the TOP of the field PNG.
         *
         * If your attacking direction is
         * bottom-to-top reversed, we can
         * flip this logic later.
         */

        float xRatio =
                centerX
                        / formationContainer.getWidth();


        float yRatio =
                centerY
                        / formationContainer.getHeight();


        // ==================================================
        // GOALKEEPER ZONE
        // Bottom 13%
        // ==================================================

        if (yRatio >= 0.87f) {

            return "GK";
        }


        // ==================================================
        // DEFENCE ZONE
        // 68% - 87%
        // ==================================================

        if (yRatio >= 0.68f) {


            if (xRatio < 0.25f) {

                return "LB";
            }


            if (xRatio > 0.75f) {

                return "RB";
            }


            return "CB";
        }


        // ==================================================
        // DEFENSIVE MIDFIELD
        // 55% - 68%
        // ==================================================

        if (yRatio >= 0.55f) {


            if (xRatio < 0.25f) {

                return "LM";
            }


            if (xRatio > 0.75f) {

                return "RM";
            }


            return "CDM";
        }


        // ==================================================
        // CENTRAL MIDFIELD
        // 38% - 55%
        // ==================================================

        if (yRatio >= 0.38f) {


            if (xRatio < 0.25f) {

                return "LM";
            }


            if (xRatio > 0.75f) {

                return "RM";
            }


            return "CM";
        }


        // ==================================================
        // ATTACKING MIDFIELD
        // 25% - 38%
        // ==================================================

        if (yRatio >= 0.25f) {


            if (xRatio < 0.25f) {

                return "LW";
            }


            if (xRatio > 0.75f) {

                return "RW";
            }


            return "CAM";
        }


        // ==================================================
        // FORWARD / ATTACK ZONE
        // Top 25%
        // ==================================================

        if (xRatio < 0.30f) {

            return "LW";
        }


        if (xRatio > 0.70f) {

            return "RW";
        }


        /*
         * Central forward position.
         *
         * Slightly deeper = CF
         * Very high = ST
         */

        if (yRatio < 0.12f) {

            return "ST";
        }


        return "CF";
    }


    // ==================================================
    // ENABLE / DISABLE VIEWPAGER SWIPE
    // ==================================================

    private void setViewPagerSwipeEnabled(
            boolean enabled
    ) {

        if (!isAdded()) {
            return;
        }


        /*
         * Search upward through parent views
         * until we find the ViewPager2.
         */

        View currentView =
                getView();


        if (currentView == null) {
            return;
        }


        ViewParentSearch:

        while (currentView.getParent() != null) {


            if (currentView.getParent()
                    instanceof ViewPager2) {


                ViewPager2 viewPager =
                        (ViewPager2)
                                currentView.getParent();


                viewPager.setUserInputEnabled(
                        enabled
                );


                break ViewParentSearch;
            }


            if (currentView.getParent()
                    instanceof View) {


                currentView =
                        (View)
                                currentView.getParent();

            } else {

                break;
            }
        }
    }


    // ==================================================
    // SAVE PLAYER POSITION + FORMATION ROLE
    // ==================================================

    private void savePlayerPosition(
            String footballerId,
            float x,
            float y,
            String formationPosition
    ) {

        if (formationRef == null ||
                footballerId == null) {

            return;
        }


        Map<String, Object> updates =
                new HashMap<>();


        updates.put(
                "x",
                x
        );


        updates.put(
                "y",
                y
        );


        updates.put(
                "formationPosition",
                formationPosition
        );


        formationRef
                .child(footballerId)
                .updateChildren(
                        updates
                );
    }


    // ==================================================
    // REMOVE DYNAMIC PLAYER VIEWS
    // ==================================================

    private void removeFormationPlayerViews() {

        if (formationContainer == null) {
            return;
        }


        for (int i =
             formationContainer.getChildCount() - 1;
             i >= 0;
             i--) {


            View child =
                    formationContainer.getChildAt(
                            i
                    );


            Object tag =
                    child.getTag();


            if ("formation_player".equals(
                    tag
            )) {


                formationContainer.removeViewAt(
                        i
                );
            }
        }
    }


    // ==================================================
    // UPDATE UI
    // ==================================================

    private void updateFormationUI() {

        if (txtFormationPlayerCount != null) {


            txtFormationPlayerCount.setText(
                    formationPlayerCount
                            + " / "
                            + MAX_FORMATION_PLAYERS
            );
        }


        if (txtEmptyFormation != null) {


            if (formationPlayerCount == 0) {


                txtEmptyFormation.setVisibility(
                        View.VISIBLE
                );


            } else {


                txtEmptyFormation.setVisibility(
                        View.GONE
                );
            }
        }


        if (btnAddFormationPlayer != null) {


            boolean canAdd =
                    formationPlayerCount
                            < MAX_FORMATION_PLAYERS;


            btnAddFormationPlayer.setEnabled(
                    canAdd
            );


            btnAddFormationPlayer.setAlpha(
                    canAdd
                            ? 1f
                            : 0.4f
            );
        }
    }


    // ==================================================
    // SHORT PLAYER NAME
    // ==================================================

    private String getShortPlayerName(
            String name
    ) {

        if (name == null ||
                name.trim().isEmpty()) {


            return "PLAYER";
        }


        String cleanName =
                name.trim();


        String[] parts =
                cleanName.split(
                        "\\s+"
                );


        if (parts.length == 1) {


            return parts[0]
                    .toUpperCase();
        }


        /*
         * Use last name.
         *
         * Lionel Messi -> MESSI
         * Cristiano Ronaldo -> RONALDO
         * Kylian Mbappe -> MBAPPE
         */

        return parts[
                parts.length - 1
                ]
                .toUpperCase();
    }


    // ==================================================
    // DP TO PX
    // ==================================================

    private int dpToPx(
            int dp
    ) {


        return Math.round(

                dp
                        * getResources()
                        .getDisplayMetrics()
                        .density

        );
    }


    // ==================================================
    // TEAM PLAYER MODEL
    // ==================================================

    private static class TeamPlayer {


        String id;


        String name;


        String position;


        String type;


        String image;
    }


    // ==================================================
    // DESTROY VIEW
    // ==================================================

    @Override
    public void onDestroyView() {


        /*
         * Make sure ViewPager swipe is restored
         * if the fragment is destroyed while
         * a player is being dragged.
         */

        setViewPagerSwipeEnabled(
                true
        );


        playerBeingDragged =
                false;


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
        // REMOVE TEAM LISTENER
        // ==================================================

        if (teamRef != null &&
                teamListener != null) {


            teamRef.removeEventListener(
                    teamListener
            );
        }


        // ==================================================
        // REMOVE FORMATION LISTENER
        // ==================================================

        if (formationRef != null &&
                formationListener != null) {


            formationRef.removeEventListener(
                    formationListener
            );
        }


        // ==================================================
        // CLEAR LOCAL DATA
        // ==================================================

        formationPlayerIds.clear();


        // ==================================================
        // CLEAR VIEW REFERENCES
        // ==================================================

        formationContainer =
                null;


        txtFormationBudget =
                null;


        txtFormationPlayerCount =
                null;


        txtEmptyFormation =
                null;


        btnAddFormationPlayer =
                null;


        super.onDestroyView();
    }
}