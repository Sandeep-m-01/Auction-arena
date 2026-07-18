package com.sandeep.auctionarena;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PlayerLoader {

    public static List<Player> loadPlayers(Context context) {

        List<Player> players = new ArrayList<>();

        try {

            // Open assets/players.json
            InputStream inputStream =
                    context.getAssets().open("players.json");

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(inputStream)
                    );

            StringBuilder jsonBuilder =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {

                jsonBuilder.append(line);
            }

            reader.close();

            inputStream.close();


            // Convert JSON text into array
            JSONArray jsonArray =
                    new JSONArray(
                            jsonBuilder.toString()
                    );


            // Read every footballer
            for (int i = 0;
                 i < jsonArray.length();
                 i++) {

                JSONObject object =
                        jsonArray.getJSONObject(i);


                int id =
                        object.getInt("id");


                String name =
                        object.getString("name");


                String position =
                        object.getString("position");


                String type =
                        object.getString("type");


                // Image is optional for now
                String image =
                        object.optString(
                                "image",
                                ""
                        );


                Player player =
                        new Player(
                                id,
                                name,
                                position,
                                type,
                                image
                        );


                players.add(player);
            }


        } catch (Exception e) {

            e.printStackTrace();
        }


        return players;
    }
}