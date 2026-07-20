package com.sandeep.auctionarena;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormationFragment extends Fragment {


    // ==================================================
    // CONFIG
    // ==================================================

    private static final String DATABASE_URL =
            "https://auctionarena-c777d-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int MAX_FORMATION_PLAYERS = 11;

    private static final long STARTING_BUDGET = 1000L;


    // ==================================================
    // ROOM / PLAYER DATA
    // ==================================================

    private String roomCode;
    private String playerId;
    private String playerName;

    private boolean isHost;


    // ==================================================
    // UI
    // ==================================================

    private ViewGroup formationContainer;

    private TextView txtFormationTitle;
    private TextView txtFormationBudget;
    private TextView txtFormationPlayerCount;
    private TextView txtEmptyFormation;

    private View btnAddFormationPlayer;
    private View btnSubmitFormation;


    // ==================================================
    // FIREBASE
    // ==================================================

    private DatabaseReference roomRef;
    private DatabaseReference myPlayerRef;
    private DatabaseReference teamRef;
    private DatabaseReference formationRef;

    private ValueEventListener budgetListener;
    private ValueEventListener teamListener;
    private ValueEventListener formationListener;

    // NEW:
    // Host watches all managers' submission states.
    private ValueEventListener playersSubmissionListener;

    // NEW:
    // Every manager watches the final match result.
    private ValueEventListener matchResultListener;


    // ==================================================
    // LOCAL DATA
    // ==================================================

    private final List<FormationPlayer> teamPlayers =
            new ArrayList<>();

    private final Map<String, FormationPlayer> formationPlayers =
            new HashMap<>();

    private final Map<String, View> formationPlayerViews =
            new HashMap<>();


    // ==================================================
    // STATE
    // ==================================================

    private long currentBudget =
            STARTING_BUDGET;

    private boolean formationLoaded =
            false;

    private boolean formationSubmitted =
            false;

    // Prevent the Victory / Defeat dialog
    // from appearing more than once.
    private boolean resultShown =
            false;

    // Prevent repeated host-side result calculation
    // while Firebase listeners are firing.
    private boolean resultCalculationStarted =
            false;


    // ==================================================
    // CONSTRUCTOR
    // ==================================================

    public FormationFragment() {

        // Required empty constructor.
    }


    // ==================================================
    // ON CREATE
    // ==================================================

    @Override
    public void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


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


        // ==================================================
        // CONNECT UI
        // ==================================================

        formationContainer =
                view.findViewById(
                        R.id.formationContainer
                );


        txtFormationTitle =
                view.findViewById(
                        R.id.txtFormationTitle
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


        btnSubmitFormation =
                view.findViewById(
                        R.id.btnSubmitFormation
                );


        // ==================================================
        // VALIDATE ROOM / PLAYER
        // ==================================================

        if (roomCode == null ||
                roomCode.trim().isEmpty() ||
                playerId == null ||
                playerId.trim().isEmpty()) {


            Toast.makeText(
                    requireContext(),
                    "Room or player data missing",
                    Toast.LENGTH_SHORT
            ).show();


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


        myPlayerRef =
                roomRef
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
        // TITLE
        // ==================================================

        if (txtFormationTitle != null) {


            if (playerName != null &&
                    !playerName.trim().isEmpty()) {


                txtFormationTitle.setText(

                        playerName
                                .trim()
                                .toUpperCase()
                                + "'S FORMATION"

                );


            } else {


                txtFormationTitle.setText(
                        "FORMATION"
                );
            }
        }


        // ==================================================
        // ADD PLAYER
        // ==================================================

        if (btnAddFormationPlayer != null) {


            btnAddFormationPlayer.setOnClickListener(

                    v -> showAddPlayerDialog()

            );
        }


        // ==================================================
        // SUBMIT FORMATION
        // ==================================================

        if (btnSubmitFormation != null) {


            btnSubmitFormation.setOnClickListener(

                    v -> submitFormation()

            );
        }


        // ==================================================
        // START FIREBASE LISTENERS
        // ==================================================

        listenToBudget();

        listenToTeam();

        listenToFormation();


        // ==================================================
        // NEW RESULT FLOW LISTENERS
        // ==================================================

        // Host continuously watches for all managers
        // finishing their formations.
        listenToAllFormationSubmissions();

        // Every manager listens for the same final result.
        listenToMatchResult();
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


                                        Long budget =
                                                getLongValue(
                                                        snapshot
                                                );


                                        if (budget == null) {


                                            budget =
                                                    STARTING_BUDGET;
                                        }


                                        currentBudget =
                                                budget;


                                        if (txtFormationBudget != null) {


                                            txtFormationBudget.setText(

                                                    "$"
                                                            + currentBudget
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
    // LISTEN TO TEAM
    // ==================================================

    private void listenToTeam() {

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


                                teamPlayers.clear();


                                for (DataSnapshot playerSnapshot :
                                        snapshot.getChildren()) {


                                    Object idObject =
                                            playerSnapshot
                                                    .child("id")
                                                    .getValue();


                                    String id =
                                            idObject != null

                                                    ? String.valueOf(
                                                    idObject
                                            )

                                                    : playerSnapshot.getKey();


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


                                    Long price =
                                            getLongValue(

                                                    playerSnapshot.child(
                                                            "price"
                                                    )

                                            );


                                    if (id == null ||
                                            id.trim().isEmpty() ||
                                            name == null ||
                                            name.trim().isEmpty()) {


                                        continue;
                                    }


                                    FormationPlayer player =
                                            new FormationPlayer();


                                    player.id =
                                            id;


                                    player.name =
                                            name;


                                    player.originalPosition =
                                            normalizePosition(
                                                    position
                                            );


                                    player.currentPosition =
                                            player.originalPosition;


                                    player.type =
                                            type;


                                    player.image =
                                            image;


                                    player.price =
                                            price != null

                                                    ? price

                                                    : 0L;


                                    teamPlayers.add(
                                            player
                                    );
                                }


                                updateFormationCounter();

                                updateEmptyState();
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
    //
    // YOUR EXISTING NO-BLINK FIX IS PRESERVED.
    // Existing player Views are updated directly.
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


                                List<String> firebasePlayerIds =
                                        new ArrayList<>();


                                for (DataSnapshot playerSnapshot :
                                        snapshot.getChildren()) {


                                    String id =
                                            playerSnapshot.getKey();


                                    if (id == null ||
                                            id.trim().isEmpty()) {


                                        continue;
                                    }


                                    firebasePlayerIds.add(
                                            id
                                    );


                                    String name =
                                            playerSnapshot
                                                    .child("name")
                                                    .getValue(
                                                            String.class
                                                    );


                                    String originalPosition =
                                            playerSnapshot
                                                    .child("originalPosition")
                                                    .getValue(
                                                            String.class
                                                    );


                                    String currentPosition =
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


                                    Long price =
                                            getLongValue(

                                                    playerSnapshot.child(
                                                            "price"
                                                    )

                                            );


                                    Double x =
                                            getDoubleValue(

                                                    playerSnapshot.child(
                                                            "x"
                                                    )

                                            );


                                    Double y =
                                            getDoubleValue(

                                                    playerSnapshot.child(
                                                            "y"
                                                    )

                                            );


                                    // ======================================
                                    // EXISTING PLAYER
                                    // ======================================

                                    FormationPlayer existingPlayer =
                                            formationPlayers.get(
                                                    id
                                            );


                                    if (existingPlayer != null) {


                                        if (name != null) {


                                            existingPlayer.name =
                                                    name;
                                        }


                                        if (originalPosition != null &&
                                                !originalPosition
                                                        .trim()
                                                        .isEmpty()) {


                                            existingPlayer.originalPosition =
                                                    normalizePosition(
                                                            originalPosition
                                                    );
                                        }


                                        if (currentPosition != null &&
                                                !currentPosition
                                                        .trim()
                                                        .isEmpty()) {


                                            existingPlayer.currentPosition =
                                                    normalizePosition(
                                                            currentPosition
                                                    );
                                        }


                                        existingPlayer.type =
                                                type;


                                        existingPlayer.image =
                                                image;


                                        if (price != null) {


                                            existingPlayer.price =
                                                    price;
                                        }


                                        if (x != null) {


                                            existingPlayer.x =
                                                    x.floatValue();
                                        }


                                        if (y != null) {


                                            existingPlayer.y =
                                                    y.floatValue();
                                        }


                                        // ==================================
                                        // UPDATE SAME VIEW
                                        // ==================================

                                        View existingView =
                                                formationPlayerViews.get(
                                                        id
                                                );


                                        if (existingView != null) {


                                            TextView nameView =
                                                    existingView.findViewById(

                                                            R.id.txtFormationPlayerName

                                                    );


                                            TextView positionView =
                                                    existingView.findViewById(

                                                            R.id.txtFormationPlayerPosition

                                                    );


                                            if (nameView != null) {


                                                nameView.setText(
                                                        existingPlayer.name
                                                );
                                            }


                                            if (positionView != null) {


                                                positionView.setText(
                                                        existingPlayer.currentPosition
                                                );
                                            }


                                            placePlayerView(
                                                    existingView,
                                                    existingPlayer
                                            );
                                        }


                                        continue;
                                    }


                                    // ======================================
                                    // NEW PLAYER
                                    // ======================================

                                    FormationPlayer player =
                                            new FormationPlayer();


                                    player.id =
                                            id;


                                    player.name =
                                            name != null

                                                    ? name

                                                    : "";


                                    player.originalPosition =
                                            normalizePosition(
                                                    originalPosition
                                            );


                                    if (currentPosition == null ||
                                            currentPosition
                                                    .trim()
                                                    .isEmpty()) {


                                        player.currentPosition =
                                                player.originalPosition;


                                    } else {


                                        player.currentPosition =
                                                normalizePosition(
                                                        currentPosition
                                                );
                                    }


                                    player.type =
                                            type;


                                    player.image =
                                            image;


                                    player.price =
                                            price != null

                                                    ? price

                                                    : 0L;


                                    player.x =
                                            x != null

                                                    ? x.floatValue()

                                                    : -1f;


                                    player.y =
                                            y != null

                                                    ? y.floatValue()

                                                    : -1f;


                                    formationPlayers.put(
                                            id,
                                            player
                                    );


                                    if (!formationPlayerViews
                                            .containsKey(
                                                    id
                                            )) {


                                        createFormationPlayerView(
                                                player
                                        );
                                    }
                                }


                                // ==========================================
                                // REMOVE PLAYERS DELETED FROM FIREBASE
                                // ==========================================

                                List<String> localPlayerIds =
                                        new ArrayList<>(

                                                formationPlayers.keySet()

                                        );


                                for (String localId :
                                        localPlayerIds) {


                                    if (!firebasePlayerIds.contains(
                                            localId
                                    )) {


                                        formationPlayers.remove(
                                                localId
                                        );


                                        View removedView =
                                                formationPlayerViews.remove(
                                                        localId
                                                );


                                        if (removedView != null &&
                                                formationContainer != null) {


                                            formationContainer.removeView(
                                                    removedView
                                            );
                                        }
                                    }
                                }


                                formationLoaded =
                                        true;


                                updateFormationCounter();

                                updateEmptyState();
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
    // SAFE DOUBLE FROM FIREBASE
    // ==================================================

    @Nullable
    private Double getDoubleValue(
            DataSnapshot snapshot
    ) {

        if (snapshot == null ||
                !snapshot.exists()) {


            return null;
        }


        Object value =
                snapshot.getValue();


        if (value == null) {


            return null;
        }


        if (value instanceof Double) {


            return (Double) value;
        }


        if (value instanceof Long) {


            return ((Long) value)
                    .doubleValue();
        }


        if (value instanceof Integer) {


            return ((Integer) value)
                    .doubleValue();
        }


        if (value instanceof Float) {


            return ((Float) value)
                    .doubleValue();
        }


        try {


            return Double.parseDouble(

                    String.valueOf(
                            value
                    )

            );


        } catch (NumberFormatException exception) {


            return null;
        }
    }


    // ==================================================
    // SAFE LONG FROM FIREBASE
    // ==================================================

    @Nullable
    private Long getLongValue(
            DataSnapshot snapshot
    ) {

        if (snapshot == null ||
                !snapshot.exists()) {


            return null;
        }


        Object value =
                snapshot.getValue();


        if (value == null) {


            return null;
        }


        if (value instanceof Long) {


            return (Long) value;
        }


        if (value instanceof Integer) {


            return ((Integer) value)
                    .longValue();
        }


        if (value instanceof Double) {


            return ((Double) value)
                    .longValue();
        }


        if (value instanceof Float) {


            return ((Float) value)
                    .longValue();
        }


        try {


            return Long.parseLong(

                    String.valueOf(
                            value
                    )

            );


        } catch (NumberFormatException exception) {


            return null;
        }
    }


    // ==================================================
    // SHOW ADD PLAYER DIALOG
    // ==================================================

    private void showAddPlayerDialog() {

        if (!isAdded()) {

            return;
        }


        if (formationSubmitted) {


            Toast.makeText(
                    requireContext(),
                    "Formation already submitted",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        if (formationPlayers.size() >=
                MAX_FORMATION_PLAYERS) {


            Toast.makeText(
                    requireContext(),
                    "Formation already has 11 players",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        List<FormationPlayer> availablePlayers =
                new ArrayList<>();


        for (FormationPlayer player :
                teamPlayers) {


            if (player.id != null &&
                    !formationPlayers.containsKey(
                            player.id
                    )) {


                availablePlayers.add(
                        player
                );
            }
        }


        if (availablePlayers.isEmpty()) {


            Toast.makeText(
                    requireContext(),
                    "No available players to add",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        String[] playerNames =
                new String[
                        availablePlayers.size()
                        ];


        for (int i = 0;
             i < availablePlayers.size();
             i++) {


            FormationPlayer player =
                    availablePlayers.get(
                            i
                    );


            playerNames[i] =

                    player.name

                            + "  •  "

                            + player.originalPosition;
        }


        new AlertDialog.Builder(
                requireContext()
        )

                .setTitle(
                        "Add Player"
                )

                .setItems(

                        playerNames,

                        (dialog, which) -> {


                            if (which < 0 ||
                                    which >= availablePlayers.size()) {


                                return;
                            }


                            addPlayerToFormation(

                                    availablePlayers.get(
                                            which
                                    )

                            );
                        }

                )

                .setNegativeButton(
                        "CANCEL",
                        null
                )

                .show();
    }


    // ==================================================
    // ADD PLAYER TO FORMATION
    // ==================================================

    private void addPlayerToFormation(
            FormationPlayer sourcePlayer
    ) {

        if (sourcePlayer == null ||
                sourcePlayer.id == null ||
                formationContainer == null ||
                formationSubmitted) {


            return;
        }


        if (formationPlayers.size() >=
                MAX_FORMATION_PLAYERS) {


            return;
        }


        if (formationPlayers.containsKey(
                sourcePlayer.id
        )) {


            return;
        }


        FormationPlayer player =
                new FormationPlayer();


        player.id =
                sourcePlayer.id;


        player.name =
                sourcePlayer.name;


        player.originalPosition =
                normalizePosition(
                        sourcePlayer.originalPosition
                );


        player.currentPosition =
                player.originalPosition;


        player.type =
                sourcePlayer.type;


        player.image =
                sourcePlayer.image;


        player.price =
                sourcePlayer.price;


        float[] coordinates =
                getDefaultCoordinatesForPosition(

                        player.originalPosition

                );


        player.x =
                coordinates[0];


        player.y =
                coordinates[1];


        formationPlayers.put(
                player.id,
                player
        );


        createFormationPlayerView(
                player
        );


        updateFormationCounter();

        updateEmptyState();


        saveFormationPlayer(
                player
        );
    }


    // ==================================================
    // DEFAULT FIELD COORDINATES
    //
    // UNCHANGED FROM YOUR CURRENT CODE
    // ==================================================

    private float[] getDefaultCoordinatesForPosition(
            String position
    ) {

        String normalized =
                normalizePosition(
                        position
                );


        switch (normalized) {


            case "GK":

                return new float[]{
                        0.50f,
                        1.00f
                };


            case "LB":

                return new float[]{
                        0.00f,
                        0.70f
                };


            case "RB":

                return new float[]{
                        1.00f,
                        0.70f
                };


            case "CB":

                return new float[]{
                        0.50f,
                        0.72f
                };


            case "CDM":

                return new float[]{
                        0.50f,
                        0.58f
                };


            case "CM":

                return new float[]{
                        0.50f,
                        0.46f
                };


            case "CAM":

                return new float[]{
                        0.50f,
                        0.30f
                };


            case "LW":

                return new float[]{
                        0.18f,
                        0.20f
                };


            case "RW":

                return new float[]{
                        0.82f,
                        0.20f
                };


            case "CF":

                return new float[]{
                        0.50f,
                        0.23f
                };


            case "ST":

                return new float[]{
                        0.50f,
                        0.12f
                };


            default:

                return new float[]{
                        0.50f,
                        0.50f
                };
        }
    }


    // ==================================================
    // NORMALIZE POSITION
    // ==================================================

    private String normalizePosition(
            String position
    ) {

        if (position == null ||
                position.trim().isEmpty()) {


            return "CM";
        }


        String normalized =
                position
                        .trim()
                        .toUpperCase();


        switch (normalized) {


            case "GOALKEEPER":

                return "GK";


            case "LEFT BACK":

            case "LWB":

                return "LB";


            case "RIGHT BACK":

            case "RWB":

                return "RB";


            case "CENTER BACK":

            case "CENTRE BACK":

                return "CB";


            case "DEFENSIVE MIDFIELDER":

            case "DM":

                return "CDM";


            case "CENTRAL MIDFIELDER":

                return "CM";


            case "ATTACKING MIDFIELDER":

            case "AM":

                return "CAM";


            case "LEFT MIDFIELDER":

            case "LEFT WINGER":

            case "LM":

                return "LW";


            case "RIGHT MIDFIELDER":

            case "RIGHT WINGER":

            case "RM":

                return "RW";


            case "CENTER FORWARD":

            case "CENTRE FORWARD":

                return "CF";


            case "STRIKER":

            case "FORWARD":

                return "ST";


            default:

                return normalized;
        }
    }


    // ==================================================
    // CREATE FORMATION PLAYER VIEW
    //
    // YOUR NO-DUPLICATE / NO-BLINK BEHAVIOR
    // IS PRESERVED.
    // ==================================================

    private void createFormationPlayerView(
            FormationPlayer player
    ) {

        if (!isAdded() ||
                formationContainer == null ||
                player == null ||
                player.id == null) {


            return;
        }


        if (formationPlayerViews.containsKey(
                player.id
        )) {


            return;
        }


        View playerView =
                LayoutInflater
                        .from(
                                requireContext()
                        )
                        .inflate(
                                R.layout.item_formation_player,
                                formationContainer,
                                false
                        );


        ImageView imageView =
                playerView.findViewById(
                        R.id.imgFormationPlayer
                );


        TextView nameView =
                playerView.findViewById(
                        R.id.txtFormationPlayerName
                );


        TextView positionView =
                playerView.findViewById(
                        R.id.txtFormationPlayerPosition
                );


        if (nameView != null) {


            nameView.setText(
                    player.name
            );
        }


        if (positionView != null) {


            positionView.setText(
                    player.currentPosition
            );
        }


        if (imageView != null &&
                player.image != null &&
                !player.image.trim().isEmpty()) {


            int imageResource =
                    getResources()
                            .getIdentifier(
                                    player.image,
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


        formationContainer.addView(
                playerView
        );


        formationPlayerViews.put(
                player.id,
                playerView
        );


        playerView.post(

                () -> {


                    if (isAdded() &&
                            formationContainer != null &&
                            playerView.getParent() != null) {


                        placePlayerView(
                                playerView,
                                player
                        );
                    }
                }

        );


        enablePlayerDragging(
                playerView,
                player,
                positionView
        );


        playerView.setOnLongClickListener(

                v -> {


                    showRemovePlayerDialog(
                            player
                    );


                    return true;
                }

        );
    }


    // ==================================================
    // PLACE PLAYER ON FIELD
    //
    // UNCHANGED
    // ==================================================

    private void placePlayerView(
            View playerView,
            FormationPlayer player
    ) {

        if (formationContainer == null ||
                playerView == null ||
                player == null) {


            return;
        }


        int containerWidth =
                formationContainer.getWidth();


        int containerHeight =
                formationContainer.getHeight();


        if (containerWidth <= 0 ||
                containerHeight <= 0 ||
                playerView.getWidth() <= 0 ||
                playerView.getHeight() <= 0) {


            return;
        }


        if (player.x < 0f ||
                player.x > 1f ||
                player.y < 0f ||
                player.y > 1f) {


            float[] coordinates =
                    getDefaultCoordinatesForPosition(

                            player.originalPosition

                    );


            player.x =
                    coordinates[0];


            player.y =
                    coordinates[1];
        }


        float maxX =
                Math.max(

                        0f,

                        containerWidth
                                - playerView.getWidth()

                );


        float maxY =
                Math.max(

                        0f,

                        containerHeight
                                - playerView.getHeight()

                );


        playerView.setX(

                player.x * maxX

        );


        playerView.setY(

                player.y * maxY

        );
    }

    // ==================================================
    // ENABLE SMOOTH PLAYER DRAGGING
    // ==================================================

    private void enablePlayerDragging(
            View playerView,
            FormationPlayer player,
            TextView positionView
    ) {

        if (playerView == null ||
                player == null) {

            return;
        }


        playerView.setOnTouchListener(

                new View.OnTouchListener() {


                    private float touchOffsetX;
                    private float touchOffsetY;

                    private float downRawX;
                    private float downRawY;

                    private boolean dragging = false;


                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {

                        if (formationSubmitted ||
                                formationContainer == null) {

                            return false;
                        }


                        switch (event.getActionMasked()) {


                            case MotionEvent.ACTION_DOWN:


                                dragging = false;


                                downRawX =
                                        event.getRawX();


                                downRawY =
                                        event.getRawY();


                                touchOffsetX =
                                        event.getX();


                                touchOffsetY =
                                        event.getY();


                                view.bringToFront();


                                disallowParentIntercept(
                                        view,
                                        true
                                );


                                return true;


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


                                if (distanceX > 5f ||
                                        distanceY > 5f) {


                                    dragging = true;
                                }


                                if (!dragging) {


                                    return true;
                                }


                                disallowParentIntercept(
                                        view,
                                        true
                                );


                                int[] containerLocation =
                                        new int[2];


                                formationContainer
                                        .getLocationOnScreen(
                                                containerLocation
                                        );


                                float newX =

                                        event.getRawX()

                                                - containerLocation[0]

                                                - touchOffsetX;


                                float newY =

                                        event.getRawY()

                                                - containerLocation[1]

                                                - touchOffsetY;


                                float maxX =
                                        Math.max(
                                                0f,
                                                formationContainer.getWidth()
                                                        - view.getWidth()
                                        );


                                float maxY =
                                        Math.max(
                                                0f,
                                                formationContainer.getHeight()
                                                        - view.getHeight()
                                        );


                                newX =
                                        Math.max(
                                                0f,
                                                Math.min(
                                                        newX,
                                                        maxX
                                                )
                                        );


                                newY =
                                        Math.max(
                                                0f,
                                                Math.min(
                                                        newY,
                                                        maxY
                                                )
                                        );


                                // IMPORTANT:
                                // Smooth local movement only.
                                // No Firebase write while dragging.

                                view.setX(
                                        newX
                                );


                                view.setY(
                                        newY
                                );


                                player.x =
                                        maxX > 0f

                                                ? newX / maxX

                                                : 0.5f;


                                player.y =
                                        maxY > 0f

                                                ? newY / maxY

                                                : 0.5f;


                                player.currentPosition =
                                        calculateFormationPosition(
                                                player.x,
                                                player.y
                                        );


                                if (positionView != null) {


                                    positionView.setText(
                                            player.currentPosition
                                    );
                                }


                                return true;


                            case MotionEvent.ACTION_UP:


                                disallowParentIntercept(
                                        view,
                                        false
                                );


                                if (dragging) {


                                    player.currentPosition =
                                            calculateFormationPosition(
                                                    player.x,
                                                    player.y
                                            );


                                    if (positionView != null) {


                                        positionView.setText(
                                                player.currentPosition
                                        );
                                    }


                                    // Save only after release.
                                    saveFormationPlayer(
                                            player
                                    );


                                } else {


                                    view.performClick();
                                }


                                dragging = false;


                                return true;


                            case MotionEvent.ACTION_CANCEL:


                                disallowParentIntercept(
                                        view,
                                        false
                                );


                                dragging = false;


                                return true;


                            default:


                                return false;
                        }
                    }
                }
        );
    }


    // ==================================================
    // PREVENT PARENT FROM STEALING DRAG
    // ==================================================

    private void disallowParentIntercept(
            View view,
            boolean disallow
    ) {

        if (view == null) {

            return;
        }


        android.view.ViewParent parent =
                view.getParent();


        while (parent != null) {


            parent.requestDisallowInterceptTouchEvent(
                    disallow
            );


            parent =
                    parent.getParent();
        }
    }


    // ==================================================
    // CALCULATE POSITION FROM FIELD LOCATION
    // ==================================================

    private String calculateFormationPosition(
            float x,
            float y
    ) {


        if (y >= 0.85f) {


            return "GK";
        }


        if (y >= 0.65f) {


            if (x < 0.20f) {


                return "LB";
            }


            if (x > 0.80f) {


                return "RB";
            }


            return "CB";
        }


        if (y >= 0.53f) {


            return "CDM";
        }


        if (y >= 0.40f) {


            return "CM";
        }


        if (y >= 0.26f) {


            if (x < 0.25f) {


                return "LW";
            }


            if (x > 0.75f) {


                return "RW";
            }


            return "CAM";
        }


        if (x < 0.33f) {


            return "LW";
        }


        if (x > 0.67f) {


            return "RW";
        }


        return "ST";
    }


    // ==================================================
    // SAVE FORMATION PLAYER
    // ==================================================

    private void saveFormationPlayer(
            FormationPlayer player
    ) {

        if (formationRef == null ||
                player == null ||
                player.id == null ||
                player.id.trim().isEmpty()) {


            return;
        }


        Map<String, Object> playerData =
                new HashMap<>();


        playerData.put(
                "id",
                player.id
        );


        playerData.put(
                "name",
                player.name
        );


        playerData.put(
                "originalPosition",
                player.originalPosition
        );


        playerData.put(
                "position",
                player.currentPosition
        );


        playerData.put(
                "type",
                player.type
        );


        playerData.put(
                "image",
                player.image
        );


        playerData.put(
                "price",
                player.price
        );


        playerData.put(
                "x",
                player.x
        );


        playerData.put(
                "y",
                player.y
        );


        formationRef
                .child(
                        player.id
                )
                .updateChildren(
                        playerData
                );
    }


    // ==================================================
    // REMOVE PLAYER DIALOG
    // ==================================================

    private void showRemovePlayerDialog(
            FormationPlayer player
    ) {

        if (!isAdded() ||
                player == null ||
                formationSubmitted) {


            return;
        }


        String displayName =
                player.name != null

                        ? player.name

                        : "this player";


        new AlertDialog.Builder(
                requireContext()
        )

                .setTitle(
                        "Remove Player?"
                )

                .setMessage(
                        "Remove "
                                + displayName
                                + " from your formation?"
                )

                .setNegativeButton(
                        "CANCEL",
                        null
                )

                .setPositiveButton(
                        "REMOVE",
                        (dialog, which) ->
                                removePlayerFromFormation(
                                        player
                                )
                )

                .show();
    }


    // ==================================================
    // REMOVE PLAYER
    // ==================================================

    private void removePlayerFromFormation(
            FormationPlayer player
    ) {

        if (player == null ||
                player.id == null ||
                formationSubmitted) {


            return;
        }


        String id =
                player.id;


        formationPlayers.remove(
                id
        );


        View playerView =
                formationPlayerViews.remove(
                        id
                );


        if (formationContainer != null &&
                playerView != null) {


            formationContainer.removeView(
                    playerView
            );
        }


        updateFormationCounter();

        updateEmptyState();


        if (formationRef != null) {


            formationRef
                    .child(
                            id
                    )
                    .removeValue();
        }
    }


    // ==================================================
    // UPDATE COUNTER
    // ==================================================

    private void updateFormationCounter() {

        if (txtFormationPlayerCount == null) {


            return;
        }


        txtFormationPlayerCount.setText(

                formationPlayers.size()

                        + " / "

                        + MAX_FORMATION_PLAYERS

        );
    }


    // ==================================================
    // EMPTY STATE
    // ==================================================

    private void updateEmptyState() {

        if (txtEmptyFormation == null) {


            return;
        }


        txtEmptyFormation.setVisibility(

                formationPlayers.isEmpty()

                        ? View.VISIBLE

                        : View.GONE

        );
    }


    // ==================================================
    // SUBMIT FORMATION
    // ==================================================

    private void submitFormation() {

        if (!isAdded() ||
                myPlayerRef == null ||
                roomRef == null) {


            return;
        }


        if (formationSubmitted) {


            Toast.makeText(
                    requireContext(),
                    "Formation already submitted",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        if (formationPlayers.size() !=
                MAX_FORMATION_PLAYERS) {


            Toast.makeText(
                    requireContext(),
                    "Add all 11 players before submitting",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        boolean hasGoalkeeper =
                false;


        for (FormationPlayer player :
                formationPlayers.values()) {


            if ("GK".equals(
                    player.currentPosition
            )) {


                hasGoalkeeper = true;

                break;
            }
        }


        if (!hasGoalkeeper) {


            Toast.makeText(
                    requireContext(),
                    "Your formation needs a goalkeeper",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        new AlertDialog.Builder(
                requireContext()
        )

                .setTitle(
                        "Submit Formation?"
                )

                .setMessage(
                        "Your final formation will be submitted. "
                                + "Make sure all 11 players are positioned correctly."
                )

                .setNegativeButton(
                        "CANCEL",
                        null
                )

                .setPositiveButton(
                        "SUBMIT",
                        (dialog, which) ->
                                confirmFormationSubmission()
                )

                .show();
    }


    // ==================================================
    // CONFIRM SUBMISSION
    // ==================================================

    private void confirmFormationSubmission() {

        if (myPlayerRef == null) {


            return;
        }


        formationSubmitted = true;


        Map<String, Object> updates =
                new HashMap<>();


        updates.put(
                "formationSubmitted",
                true
        );


        updates.put(
                "formationSubmittedAt",
                System.currentTimeMillis()
        );


        myPlayerRef
                .updateChildren(
                        updates
                )

                .addOnSuccessListener(

                        unused -> {


                            if (!isAdded()) {


                                return;
                            }


                            Toast.makeText(
                                    requireContext(),
                                    "Formation submitted. Waiting for other managers...",
                                    Toast.LENGTH_SHORT
                            ).show();


                            if (btnAddFormationPlayer != null) {


                                btnAddFormationPlayer.setEnabled(
                                        false
                                );


                                btnAddFormationPlayer.setAlpha(
                                        0.4f
                                );
                            }


                            if (btnSubmitFormation != null) {


                                btnSubmitFormation.setEnabled(
                                        false
                                );


                                btnSubmitFormation.setAlpha(
                                        0.4f
                                );
                            }


                            // Optional immediate host check.
                            // Continuous listener below handles the
                            // case where host submitted first.

                            if (isHost) {


                                checkAllFormationsSubmitted();
                            }
                        }

                )

                .addOnFailureListener(

                        error -> {


                            formationSubmitted = false;


                            if (isAdded()) {


                                Toast.makeText(
                                        requireContext(),
                                        "Failed to submit formation",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                );
    }


    // ==================================================
    // NEW:
    // HOST CONTINUOUSLY WATCHES ALL SUBMISSIONS
    //
    // Fixes:
    // HOST submits first
    // GUEST submits later
    // Host now detects guest submission automatically.
    // ==================================================

    private void listenToAllFormationSubmissions() {

        if (!isHost ||
                roomRef == null) {


            return;
        }


        playersSubmissionListener =
                roomRef
                        .child(
                                "players"
                        )
                        .addValueEventListener(

                                new ValueEventListener() {


                                    @Override
                                    public void onDataChange(
                                            @NonNull DataSnapshot snapshot
                                    ) {


                                        checkSubmissionSnapshot(
                                                snapshot
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
    // SINGLE CHECK
    // ==================================================

    private void checkAllFormationsSubmitted() {

        if (!isHost ||
                roomRef == null) {


            return;
        }


        roomRef
                .child(
                        "players"
                )
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {


                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {


                                checkSubmissionSnapshot(
                                        snapshot
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
    // CHECK SUBMISSION SNAPSHOT
    // ==================================================

    private void checkSubmissionSnapshot(
            DataSnapshot snapshot
    ) {

        if (!isHost ||
                resultCalculationStarted) {


            return;
        }


        int totalManagers = 0;

        int submittedManagers = 0;


        for (DataSnapshot managerSnapshot :
                snapshot.getChildren()) {


            totalManagers++;


            Boolean submitted =
                    managerSnapshot
                            .child(
                                    "formationSubmitted"
                            )
                            .getValue(
                                    Boolean.class
                            );


            if (Boolean.TRUE.equals(
                    submitted
            )) {


                submittedManagers++;
            }
        }


        if (totalManagers >= 2 &&
                submittedManagers == totalManagers) {


            resultCalculationStarted = true;


            onAllFormationsSubmitted(
                    snapshot
            );
        }
    }


    // ==================================================
    // ALL FORMATIONS SUBMITTED
    // ==================================================

    private void onAllFormationsSubmitted(
            DataSnapshot playersSnapshot
    ) {

        if (!isHost ||
                roomRef == null) {


            resultCalculationStarted = false;

            return;
        }


        // First check whether a result already exists.
        // This avoids overwriting an existing winner.

        roomRef
                .child(
                        "matchResult"
                )
                .addListenerForSingleValueEvent(

                        new ValueEventListener() {


                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot resultSnapshot
                            ) {


                                Boolean alreadyCompleted =
                                        resultSnapshot
                                                .child(
                                                        "completed"
                                                )
                                                .getValue(
                                                        Boolean.class
                                                );


                                if (Boolean.TRUE.equals(
                                        alreadyCompleted
                                )) {


                                    return;
                                }


                                calculateAndSaveResult(
                                        playersSnapshot
                                );
                            }


                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {


                                resultCalculationStarted = false;
                            }
                        }
                );
    }


    // ==================================================
    // CALCULATE AND SAVE RESULT
    // ==================================================

    private void calculateAndSaveResult(
            DataSnapshot playersSnapshot
    ) {

        if (!isHost ||
                roomRef == null) {


            resultCalculationStarted = false;

            return;
        }


        String winnerId = null;

        String winnerName = null;

        long highestScore =
                Long.MIN_VALUE;


        Map<String, Long> managerScores =
                new HashMap<>();


        for (DataSnapshot managerSnapshot :
                playersSnapshot.getChildren()) {


            String managerId =
                    managerSnapshot.getKey();


            if (managerId == null) {


                continue;
            }


            String managerName =
                    managerSnapshot
                            .child(
                                    "name"
                            )
                            .getValue(
                                    String.class
                            );


            DataSnapshot managerFormation =
                    managerSnapshot.child(
                            "formation"
                    );


            long score =
                    calculateFormationScore(
                            managerFormation
                    );


            managerScores.put(
                    managerId,
                    score
            );


            if (winnerId == null ||
                    score > highestScore) {


                winnerId =
                        managerId;


                winnerName =
                        managerName;


                highestScore =
                        score;
            }
        }


        if (winnerId == null) {


            resultCalculationStarted = false;

            return;
        }


        Map<String, Object> result =
                new HashMap<>();


        result.put(
                "completed",
                true
        );


        result.put(
                "winnerId",
                winnerId
        );


        result.put(
                "winnerName",
                winnerName != null

                        ? winnerName

                        : "Winner"
        );


        result.put(
                "winningScore",
                highestScore
        );


        result.put(
                "completedAt",
                System.currentTimeMillis()
        );


        result.put(
                "scores",
                managerScores
        );


        roomRef
                .child(
                        "matchResult"
                )
                .setValue(
                        result
                )

                .addOnSuccessListener(

                        unused -> {


                            if (roomRef == null) {


                                return;
                            }


                            Map<String, Object> updates =
                                    new HashMap<>();


                            updates.put(
                                    "gamePhase",
                                    "result"
                            );


                            updates.put(
                                    "allFormationsSubmitted",
                                    true
                            );


                            updates.put(
                                    "formationsCompletedAt",
                                    System.currentTimeMillis()
                            );


                            roomRef.updateChildren(
                                    updates
                            );
                        }

                )

                .addOnFailureListener(

                        error -> {


                            resultCalculationStarted = false;
                        }

                );
    }


    // ==================================================
    // CALCULATE FORMATION SCORE
    // ==================================================

    private long calculateFormationScore(
            DataSnapshot formationSnapshot
    ) {

        long score = 0L;

        int goalkeeperCount = 0;

        int defenderCount = 0;

        int midfielderCount = 0;

        int attackerCount = 0;


        for (DataSnapshot playerSnapshot :
                formationSnapshot.getChildren()) {


            Long price =
                    getLongValue(
                            playerSnapshot.child(
                                    "price"
                            )
                    );


            String originalPosition =
                    normalizePosition(

                            playerSnapshot
                                    .child(
                                            "originalPosition"
                                    )
                                    .getValue(
                                            String.class
                                    )

                    );


            String currentPosition =
                    normalizePosition(

                            playerSnapshot
                                    .child(
                                            "position"
                                    )
                                    .getValue(
                                            String.class
                                    )

                    );


            if (price != null) {


                score += price;
            }


            // Natural position bonus.

            if (originalPosition.equals(
                    currentPosition
            )) {


                score += 15L;
            }


            switch (currentPosition) {


                case "GK":


                    goalkeeperCount++;

                    break;


                case "LB":

                case "RB":

                case "CB":


                    defenderCount++;

                    break;


                case "CDM":

                case "CM":

                case "CAM":


                    midfielderCount++;

                    break;


                case "LW":

                case "RW":

                case "CF":

                case "ST":


                    attackerCount++;

                    break;
            }
        }


        if (goalkeeperCount == 1) {


            score += 40L;
        }


        if (defenderCount >= 3 &&
                defenderCount <= 5) {


            score += 35L;
        }


        if (midfielderCount >= 2 &&
                midfielderCount <= 5) {


            score += 35L;
        }


        if (attackerCount >= 1 &&
                attackerCount <= 4) {


            score += 30L;
        }


        return score;
    }


    // ==================================================
    // NEW:
    // BOTH DEVICES LISTEN FOR MATCH RESULT
    // ==================================================

    private void listenToMatchResult() {

        if (roomRef == null) {


            return;
        }


        matchResultListener =
                roomRef
                        .child(
                                "matchResult"
                        )
                        .addValueEventListener(

                                new ValueEventListener() {


                                    @Override
                                    public void onDataChange(
                                            @NonNull DataSnapshot snapshot
                                    ) {


                                        Boolean completed =
                                                snapshot
                                                        .child(
                                                                "completed"
                                                        )
                                                        .getValue(
                                                                Boolean.class
                                                        );


                                        if (!Boolean.TRUE.equals(
                                                completed
                                        )) {


                                            return;
                                        }


                                        String winnerId =
                                                snapshot
                                                        .child(
                                                                "winnerId"
                                                        )
                                                        .getValue(
                                                                String.class
                                                        );


                                        String winnerName =
                                                snapshot
                                                        .child(
                                                                "winnerName"
                                                        )
                                                        .getValue(
                                                                String.class
                                                        );


                                        if (winnerId == null ||
                                                playerId == null) {


                                            return;
                                        }


                                        if (resultShown) {


                                            return;
                                        }


                                        Long myScore =
                                                getLongValue(

                                                        snapshot
                                                                .child(
                                                                        "scores"
                                                                )
                                                                .child(
                                                                        playerId
                                                                )

                                                );


                                        boolean didWin =
                                                playerId.equals(
                                                        winnerId
                                                );


                                        resultShown = true;


                                        showMatchResult(
                                                didWin,
                                                winnerName,
                                                myScore
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
    // SHOW VICTORY / DEFEAT
    // ==================================================

    private void showMatchResult(
            boolean didWin,
            String winnerName,
            Long myScore
    ) {

        if (!isAdded()) {


            return;
        }


        String title;

        String message;


        if (didWin) {


            title =
                    "🏆 VICTORY";


            message =
                    "Congratulations!\n\n"
                            + "Your squad and formation won the match.";


        } else {


            title =
                    "DEFEAT";


            message =
                    "The match is over.\n\n"
                            + (
                            winnerName != null

                                    ? winnerName
                                      + " won the match."

                                    : "Your opponent won the match."
                    );
        }


        if (myScore != null) {


            message +=
                    "\n\nYour Team Score: "
                            + myScore;
        }


        new AlertDialog.Builder(
                requireContext()
        )

                .setTitle(
                        title
                )

                .setMessage(
                        message
                )

                .setCancelable(
                        false
                )

                .setPositiveButton(
                        "OK",
                        (dialog, which) ->
                                dialog.dismiss()
                )

                .show();
    }


    // ==================================================
    // CLEANUP
    // ==================================================

    @Override
    public void onDestroyView() {


        if (formationContainer != null) {


            disallowParentIntercept(
                    formationContainer,
                    false
            );
        }


        // Budget listener.

        if (myPlayerRef != null &&
                budgetListener != null) {


            myPlayerRef
                    .child(
                            "budget"
                    )
                    .removeEventListener(
                            budgetListener
                    );
        }


        // Team listener.

        if (teamRef != null &&
                teamListener != null) {


            teamRef.removeEventListener(
                    teamListener
            );
        }


        // Formation listener.

        if (formationRef != null &&
                formationListener != null) {


            formationRef.removeEventListener(
                    formationListener
            );
        }


        // NEW:
        // Host submission listener.

        if (roomRef != null &&
                playersSubmissionListener != null) {


            roomRef
                    .child(
                            "players"
                    )
                    .removeEventListener(
                            playersSubmissionListener
                    );
        }


        // NEW:
        // Match result listener.

        if (roomRef != null &&
                matchResultListener != null) {


            roomRef
                    .child(
                            "matchResult"
                    )
                    .removeEventListener(
                            matchResultListener
                    );
        }


        formationPlayerViews.clear();


        formationContainer = null;

        txtFormationTitle = null;

        txtFormationBudget = null;

        txtFormationPlayerCount = null;

        txtEmptyFormation = null;

        btnAddFormationPlayer = null;

        btnSubmitFormation = null;


        super.onDestroyView();
    }


    // ==================================================
    // FORMATION PLAYER MODEL
    // ==================================================

    private static class FormationPlayer {


        String id;


        String name;


        String originalPosition;


        String currentPosition;


        String type;


        String image;


        long price;


        float x = -1f;


        float y = -1f;
    }
}