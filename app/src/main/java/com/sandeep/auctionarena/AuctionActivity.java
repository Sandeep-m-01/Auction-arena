package com.sandeep.auctionarena;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class AuctionActivity extends AppCompatActivity {

    private ViewPager2 auctionViewPager;

    private String roomCode;
    private String playerId;
    private String playerName;
    private boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        Window window = getWindow();

        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // Load activity layout
        setContentView(R.layout.activity_auction);

        // Connect ViewPager2
        auctionViewPager =
                findViewById(R.id.auctionViewPager);

        // Get data sent from WaitingLobbyActivity
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

        // Room code is required
        if (roomCode == null || roomCode.isEmpty()) {
            finish();
            return;
        }

        // Temporary fallback
        // Later every participant will have a unique Firebase player ID
        if (playerId == null || playerId.isEmpty()) {

            if (playerName != null && !playerName.isEmpty()) {

                playerId = playerName
                        .trim()
                        .toLowerCase()
                        .replace(" ", "_");

            } else {

                playerId = "unknown_player";
            }
        }

        // Create ViewPager adapter
        AuctionPagerAdapter adapter =
                new AuctionPagerAdapter(
                        this,
                        roomCode,
                        playerId,
                        playerName,
                        isHost
                );

        // Attach adapter
        auctionViewPager.setAdapter(
                adapter
        );

        // Open AuctionFragment first
        auctionViewPager.setCurrentItem(
                0,
                false
        );
    }
}