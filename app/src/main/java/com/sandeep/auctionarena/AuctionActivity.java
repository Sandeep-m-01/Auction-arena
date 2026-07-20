package com.sandeep.auctionarena;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class AuctionActivity extends AppCompatActivity {

    // ==================================================
    // UI
    // ==================================================

    private ViewPager2 auctionViewPager;


    // ==================================================
    // ROOM / PLAYER DATA
    // ==================================================

    private String roomCode;
    private String playerId;
    private String playerName;

    private boolean isHost;


    // ==================================================
    // LOCAL STATE
    // ==================================================

    // Prevent multiple leave dialogs from opening
    private boolean leaveDialogShowing = false;


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
        // LOAD ACTIVITY LAYOUT
        // ==================================================

        setContentView(
                R.layout.activity_auction
        );


        // ==================================================
        // CONNECT VIEWPAGER
        // ==================================================

        auctionViewPager =
                findViewById(
                        R.id.auctionViewPager
                );


        // ==================================================
        // GET DATA FROM WAITING LOBBY
        // ==================================================

        roomCode =
                getIntent().getStringExtra(
                        "ROOM_CODE"
                );


        playerId =
                getIntent().getStringExtra(
                        "PLAYER_ID"
                );


        playerName =
                getIntent().getStringExtra(
                        "PLAYER_NAME"
                );


        isHost =
                getIntent().getBooleanExtra(
                        "IS_HOST",
                        false
                );


        // ==================================================
        // VALIDATE ROOM CODE
        // ==================================================

        if (roomCode == null ||
                roomCode.trim().isEmpty()) {

            finish();

            return;
        }


        // ==================================================
        // PLAYER ID FALLBACK
        // ==================================================

        if (playerId == null ||
                playerId.trim().isEmpty()) {


            if (playerName != null &&
                    !playerName.trim().isEmpty()) {


                playerId =
                        playerName
                                .trim()
                                .toLowerCase()
                                .replace(" ", "_");


            } else {


                playerId =
                        "unknown_player";
            }
        }


        // ==================================================
        // CREATE VIEWPAGER ADAPTER
        // ==================================================

        AuctionPagerAdapter adapter =
                new AuctionPagerAdapter(
                        this,
                        roomCode,
                        playerId,
                        playerName,
                        isHost
                );


        // ==================================================
        // ATTACH ADAPTER
        // ==================================================

        auctionViewPager.setAdapter(
                adapter
        );


        // ==================================================
        // OPEN AUCTION PAGE FIRST
        // ==================================================

        auctionViewPager.setCurrentItem(
                0,
                false
        );


        // ==================================================
        // BACK BUTTON HANDLING
        // ==================================================

        setupBackButton();
    }


    // ==================================================
    // BACK BUTTON
    // ==================================================

    private void setupBackButton() {


        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {


                                // ==========================================
                                // SHOW LEAVE CONFIRMATION
                                // ==========================================

                                showLeaveAuctionDialog();
                            }
                        }
                );
    }


    // ==================================================
    // SHOW LEAVE AUCTION DIALOG
    // ==================================================

    private void showLeaveAuctionDialog() {


        // Prevent duplicate dialogs

        if (leaveDialogShowing) {

            return;
        }


        leaveDialogShowing =
                true;


        AlertDialog dialog =
                new AlertDialog.Builder(this)

                        .setTitle(
                                "Leave Auction?"
                        )

                        .setMessage(
                                "Are you sure you want to leave the auction? " +
                                        "Leaving may end the match for you."
                        )

                        .setNegativeButton(
                                "STAY",
                                (dialogInterface, which) -> {

                                    leaveDialogShowing =
                                            false;

                                    dialogInterface.dismiss();
                                }
                        )

                        .setPositiveButton(
                                "LEAVE",
                                (dialogInterface, which) -> {

                                    leaveDialogShowing =
                                            false;


                                    // ======================================
                                    // FOR NOW:
                                    //
                                    // Close AuctionActivity.
                                    //
                                    // NEXT STEP:
                                    // We will update Firebase here and mark
                                    // this player as disconnected/left.
                                    // ======================================

                                    finish();
                                }
                        )

                        .create();


        // ==================================================
        // HANDLE DIALOG CANCEL
        // ==================================================

        dialog.setOnCancelListener(
                dialogInterface -> {

                    leaveDialogShowing =
                            false;
                }
        );


        // ==================================================
        // HANDLE DIALOG DISMISS
        // ==================================================

        dialog.setOnDismissListener(
                dialogInterface -> {

                    leaveDialogShowing =
                            false;
                }
        );


        // ==================================================
        // SHOW DIALOG
        // ==================================================

        dialog.show();
    }
}