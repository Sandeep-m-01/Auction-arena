package com.sandeep.auctionarena;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyTeamFragment extends Fragment {

    // ==================================================
    // CONFIG
    // ==================================================

    private static final String DATABASE_URL =
            "https://auctionarena-c777d-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Money stored in millions
    private static final long STARTING_BUDGET = 1000L;


    // ==================================================
    // ROOM / PLAYER DATA
    // ==================================================

    private String roomCode;
    private String playerId;
    private String playerName;


    // ==================================================
    // UI
    // ==================================================

    private TextView txtMyTeamTitle;
    private TextView txtTeamBudget;
    private TextView txtPlayerCount;

    private LinearLayout teamPlayersContainer;


    // ==================================================
    // FIREBASE
    // ==================================================

    private DatabaseReference myPlayerRef;

    private ValueEventListener budgetListener;
    private ValueEventListener teamListener;


    // ==================================================
    // CONSTRUCTOR
    // ==================================================

    public MyTeamFragment() {
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


        // Get arguments from AuctionPagerAdapter

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
        }


        // Default player name

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
                R.layout.fragment_my_team,
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

        txtMyTeamTitle =
                view.findViewById(
                        R.id.txtMyTeamTitle
                );


        txtTeamBudget =
                view.findViewById(
                        R.id.txtTeamBudget
                );


        txtPlayerCount =
                view.findViewById(
                        R.id.txtPlayerCount
                );


        teamPlayersContainer =
                view.findViewById(
                        R.id.teamPlayersContainer
                );


        // ==================================================
        // DEFAULT UI
        // ==================================================

        txtMyTeamTitle.setText(
                "MY TEAM"
        );


        txtTeamBudget.setText(
                "$1000M"
        );


        txtPlayerCount.setText(
                "0"
        );


        // ==================================================
        // VALIDATE ROOM
        // ==================================================

        if (roomCode == null ||
                roomCode.trim().isEmpty()) {

            showEmptyTeamMessage();

            return;
        }


        // ==================================================
        // VALIDATE PLAYER
        // ==================================================

        if (playerId == null ||
                playerId.trim().isEmpty()) {

            showEmptyTeamMessage();

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


        // ==================================================
        // START LISTENERS
        // ==================================================

        listenToBudget();

        listenToTeam();
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
                                                txtTeamBudget == null) {

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


                                        txtTeamBudget.setText(
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
    // LISTEN TO TEAM
    // ==================================================

    private void listenToTeam() {

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

                                        if (!isAdded() ||
                                                teamPlayersContainer == null) {

                                            return;
                                        }


                                        // Remove old cards

                                        teamPlayersContainer
                                                .removeAllViews();


                                        int playerCount = 0;


                                        // ==================================================
                                        // LOOP THROUGH BOUGHT PLAYERS
                                        // ==================================================

                                        for (DataSnapshot playerSnapshot :
                                                snapshot.getChildren()) {


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
                                                    playerSnapshot
                                                            .child("price")
                                                            .getValue(
                                                                    Long.class
                                                            );


                                            // Create card

                                            View playerCard =
                                                    createPlayerCard(
                                                            name,
                                                            position,
                                                            type,
                                                            image,
                                                            price
                                                    );


                                            teamPlayersContainer
                                                    .addView(
                                                            playerCard
                                                    );


                                            playerCount++;
                                        }


                                        // ==================================================
                                        // UPDATE COUNT
                                        // ==================================================

                                        if (txtPlayerCount != null) {

                                            txtPlayerCount.setText(
                                                    String.valueOf(
                                                            playerCount
                                                    )
                                            );
                                        }


                                        // ==================================================
                                        // EMPTY TEAM
                                        // ==================================================

                                        if (playerCount == 0) {

                                            showEmptyTeamMessage();
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
    // CREATE PLAYER CARD
    // ==================================================

    private View createPlayerCard(
            String name,
            String position,
            String type,
            String imageName,
            Long price
    ) {

        // ==================================================
        // MAIN CARD
        // ==================================================

        LinearLayout card =
                new LinearLayout(
                        requireContext()
                );


        card.setOrientation(
                LinearLayout.HORIZONTAL
        );


        card.setGravity(
                Gravity.CENTER_VERTICAL
        );


        card.setPadding(
                dpToPx(12),
                dpToPx(8),
                dpToPx(15),
                dpToPx(8)
        );


        card.setBackgroundResource(
                R.drawable.menu_card
        );


        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(90)
                );


        cardParams.setMargins(
                0,
                0,
                0,
                dpToPx(12)
        );


        card.setLayoutParams(
                cardParams
        );


        // ==================================================
        // PLAYER IMAGE
        // ==================================================

        ImageView playerImage =
                new ImageView(
                        requireContext()
                );


        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        dpToPx(70),
                        dpToPx(70)
                );


        playerImage.setLayoutParams(
                imageParams
        );


        // Fit image inside the box
        // This prevents excessive cropping

        playerImage.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );


        playerImage.setAdjustViewBounds(
                true
        );


        // ==================================================
        // LOAD LOCAL DRAWABLE
        // ==================================================

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

                playerImage.setImageResource(
                        imageResource
                );
            }
        }


        card.addView(
                playerImage
        );


        // ==================================================
        // PLAYER INFO CONTAINER
        // ==================================================

        LinearLayout infoContainer =
                new LinearLayout(
                        requireContext()
                );


        infoContainer.setOrientation(
                LinearLayout.VERTICAL
        );


        infoContainer.setGravity(
                Gravity.CENTER_VERTICAL
        );


        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                );


        infoParams.setMargins(
                dpToPx(15),
                0,
                dpToPx(10),
                0
        );


        infoContainer.setLayoutParams(
                infoParams
        );


        // ==================================================
        // PLAYER NAME
        // ==================================================

        TextView txtName =
                new TextView(
                        requireContext()
                );


        if (name != null &&
                !name.trim().isEmpty()) {

            txtName.setText(
                    name.toUpperCase()
            );

        } else {

            txtName.setText(
                    "PLAYER"
            );
        }


        txtName.setTextColor(
                Color.WHITE
        );


        txtName.setTextSize(
                20
        );


        // API 24 compatible font loading

        txtName.setTypeface(
                ResourcesCompat.getFont(
                        requireContext(),
                        R.font.bebas_neue
                )
        );


        txtName.setSingleLine(
                true
        );


        infoContainer.addView(
                txtName
        );


        // ==================================================
        // POSITION + TYPE
        // ==================================================

        TextView txtDetails =
                new TextView(
                        requireContext()
                );


        StringBuilder details =
                new StringBuilder();


        if (position != null &&
                !position.trim().isEmpty()) {

            details.append(
                    position.toUpperCase()
            );
        }


        if (type != null &&
                !type.trim().isEmpty()) {


            if (details.length() > 0) {

                details.append(
                        "  •  "
                );
            }


            details.append(
                    type.toUpperCase()
            );
        }


        txtDetails.setText(
                details.toString()
        );


        txtDetails.setTextColor(
                Color.parseColor(
                        "#AAFFFFFF"
                )
        );


        txtDetails.setTextSize(
                13
        );


        txtDetails.setTypeface(
                ResourcesCompat.getFont(
                        requireContext(),
                        R.font.bebas_neue
                )
        );


        infoContainer.addView(
                txtDetails
        );


        card.addView(
                infoContainer
        );


        // ==================================================
        // PRICE CONTAINER
        // ==================================================

        LinearLayout priceContainer =
                new LinearLayout(
                        requireContext()
                );


        priceContainer.setOrientation(
                LinearLayout.VERTICAL
        );


        priceContainer.setGravity(
                Gravity.CENTER
        );


        // ==================================================
        // PRICE LABEL
        // ==================================================

        TextView txtPriceLabel =
                new TextView(
                        requireContext()
                );


        txtPriceLabel.setText(
                "BOUGHT FOR"
        );


        txtPriceLabel.setTextColor(
                Color.parseColor(
                        "#88FFFFFF"
                )
        );


        txtPriceLabel.setTextSize(
                10
        );


        txtPriceLabel.setTypeface(
                ResourcesCompat.getFont(
                        requireContext(),
                        R.font.bebas_neue
                )
        );


        txtPriceLabel.setLetterSpacing(
                0.05f
        );


        priceContainer.addView(
                txtPriceLabel
        );


        // ==================================================
        // PRICE
        // ==================================================

        TextView txtPrice =
                new TextView(
                        requireContext()
                );


        if (price != null) {

            txtPrice.setText(
                    "$"
                            + price
                            + "M"
            );

        } else {

            txtPrice.setText(
                    "$0M"
            );
        }


        txtPrice.setTextColor(
                Color.parseColor(
                        "#D4AF37"
                )
        );


        txtPrice.setTextSize(
                20
        );


        txtPrice.setTypeface(
                ResourcesCompat.getFont(
                        requireContext(),
                        R.font.bebas_neue
                )
        );


        txtPrice.setGravity(
                Gravity.CENTER
        );


        priceContainer.addView(
                txtPrice
        );


        card.addView(
                priceContainer
        );


        return card;
    }


    // ==================================================
    // SHOW EMPTY TEAM
    // ==================================================

    private void showEmptyTeamMessage() {

        if (teamPlayersContainer == null) {

            return;
        }


        // Make sure container is empty

        teamPlayersContainer.removeAllViews();


        TextView emptyMessage =
                new TextView(
                        requireContext()
                );


        emptyMessage.setText(
                "NO PLAYERS YET\n" +
                        "WIN AN AUCTION TO BUILD YOUR TEAM"
        );


        emptyMessage.setTextColor(
                Color.parseColor(
                        "#88FFFFFF"
                )
        );


        emptyMessage.setTextSize(
                17
        );


        emptyMessage.setGravity(
                Gravity.CENTER
        );


        // API 24 compatible font loading

        emptyMessage.setTypeface(
                ResourcesCompat.getFont(
                        requireContext(),
                        R.font.bebas_neue
                )
        );


        emptyMessage.setLetterSpacing(
                0.05f
        );


        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(140)
                );


        emptyMessage.setLayoutParams(
                params
        );


        teamPlayersContainer.addView(
                emptyMessage
        );
    }


    // ==================================================
    // DP TO PIXELS
    // ==================================================

    private int dpToPx(
            int dp
    ) {

        return Math.round(
                dp *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }


    // ==================================================
    // DESTROY VIEW
    // ==================================================

    @Override
    public void onDestroyView() {


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

        if (myPlayerRef != null &&
                teamListener != null) {

            myPlayerRef
                    .child("team")
                    .removeEventListener(
                            teamListener
                    );
        }


        // ==================================================
        // CLEAR VIEW REFERENCES
        // ==================================================

        txtMyTeamTitle = null;

        txtTeamBudget = null;

        txtPlayerCount = null;

        teamPlayersContainer = null;


        super.onDestroyView();
    }
}