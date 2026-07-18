package com.sandeep.auctionarena;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AuctionPagerAdapter extends FragmentStateAdapter {

    private final String roomCode;
    private final String playerId;
    private final String playerName;
    private final boolean isHost;

    public AuctionPagerAdapter(
            @NonNull FragmentActivity fragmentActivity,
            String roomCode,
            String playerId,
            String playerName,
            boolean isHost
    ) {
        super(fragmentActivity);

        this.roomCode = roomCode;
        this.playerId = playerId;
        this.playerName = playerName;
        this.isHost = isHost;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        Fragment fragment;

        if (position == 0) {

            fragment = new AuctionFragment();

        } else {

            fragment = new MyTeamFragment();
        }

        Bundle bundle = new Bundle();

        bundle.putString(
                "ROOM_CODE",
                roomCode
        );

        bundle.putString(
                "PLAYER_ID",
                playerId
        );

        bundle.putString(
                "PLAYER_NAME",
                playerName
        );

        bundle.putBoolean(
                "IS_HOST",
                isHost
        );

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public int getItemCount() {

        return 2;
    }
}