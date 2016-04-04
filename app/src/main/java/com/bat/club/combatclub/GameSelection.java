package com.bat.club.combatclub;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class GameSelection extends AppCompatActivity {
    ArrayList<Game> mGameList = new ArrayList<>();
    ListView mGameListView;
    GameArrayAdapter mAdapter;

    SessionIDS m_cred;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_selection);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // load credentials
        loadCred();

        // set adapter
        mGameListView = (ListView) findViewById(R.id.listView);
        mGameListView.setItemsCanFocus(true);

        mAdapter = new GameArrayAdapter(GameSelection.this, mGameList);
        mGameListView.setAdapter(mAdapter);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewDialog alert = new ViewDialog();
                alert.showDialog(GameSelection.this);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        final Handler h = new Handler();
        final int delay = 5000; // 5 sec

        h.postDelayed(new Runnable() {
            public void run() {
                loadGames();
                h.postDelayed(this, delay);
            }
        }, delay);

    }

    public void loadGames() {

        CombatClubRestClient.post("/GetGames", new RequestParams(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String jsonRes = new String(responseBody, "UTF-8");
                    jsonRes = CombatClubRestClient.interprateResponse(jsonRes);
                    JSONArray gameJsonArray = new JSONArray(jsonRes);
                    mGameList.clear();
                    // convert to array list of games
                    for (int i = 0; i < gameJsonArray.length(); i++) {
                        // get Game json object
                        JSONObject gameJsonObject = gameJsonArray.getJSONObject(i);
                        // load necessary values
                        int gameId = gameJsonObject.getInt("id");
                        String gameName = gameJsonObject.getString("name");
                        int players0 = gameJsonObject.getInt("players0");
                        int players1 = gameJsonObject.getInt("players1");
                        int tickets0 = gameJsonObject.getInt("tickets0");
                        int tickets1 = gameJsonObject.getInt("tickets1");
                        int playersCap = gameJsonObject.getInt("playersCap");
                        int ticketsCap = gameJsonObject.getInt("ticketsCap");
                        mGameList.add(new Game(gameId, gameName, playersCap, ticketsCap, new int[]{players0, players1}, new int[]{tickets0, tickets1}));
                    }
                    mAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void createGame(String gameName) {
        CombatClubRestClient.post("/CreateGame", new RequestParams("gameName", gameName), new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String jsonRes = new String(responseBody, "UTF-8");
                    jsonRes = CombatClubRestClient.interprateResponse(jsonRes);
                    JSONObject response = new JSONObject(jsonRes);
                    if (response.getInt("Code") < 0) {
                        Snackbar snackbar = Snackbar
                                .make(mGameListView, response.getString("Message"), Snackbar.LENGTH_LONG);

                        snackbar.show();
                    }
                } catch (Exception ex) {
                }
                loadGames();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void loadCred() {
        try {
            Intent intent = getIntent();
            int playerID = intent.getIntExtra(SessionIDS.KEYS[0], -1);
            if (playerID == -1) throw new Exception();
            String playerName = intent.getStringExtra(SessionIDS.KEYS[1]);
            int gameID = intent.getIntExtra(SessionIDS.KEYS[2], -1);
            int teamID = intent.getIntExtra(SessionIDS.KEYS[3], -1);
            m_cred = new SessionIDS(playerID, playerName, gameID, teamID);
        } catch (Exception e) {
            m_cred = new SessionIDS(1, "Noyloy", 0, 0);
        }
    }

    private void joinGame() {
        RequestParams params = new RequestParams();
        params.add("soldierID", m_cred.playerID + "");
        params.add("gameID", m_cred.gameID + "");
        params.add("teamID", m_cred.teamID + "");
        CombatClubRestClient.post("/JoinTeam", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String jsonRes = new String(responseBody, "UTF-8");
                    jsonRes = CombatClubRestClient.interprateResponse(jsonRes);
                    JSONObject response = new JSONObject(jsonRes);
                    if (response.getInt("Code") < 0) {
                        Snackbar snackbar = Snackbar
                                .make(mGameListView, response.getString("Message"), Snackbar.LENGTH_LONG);

                        snackbar.show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), GameSession.class);
                        intent.putExtra(SessionIDS.KEYS[0], m_cred.playerID);
                        intent.putExtra(SessionIDS.KEYS[1], m_cred.playerName);
                        intent.putExtra(SessionIDS.KEYS[2], m_cred.gameID);
                        intent.putExtra(SessionIDS.KEYS[3], m_cred.teamID);
                        startActivity(intent);
                    }
                } catch (Exception ex) {
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Snackbar snackbar = Snackbar
                        .make(mGameListView, "Failed Joining", Snackbar.LENGTH_LONG);

                snackbar.show();
            }
        });
    }

    // costume dialog when opening a new game
    public class ViewDialog {

        public void showDialog(Activity activity) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.new_game_dialog);

            final EditText text = (EditText) dialog.findViewById(R.id.gameName);

            Button cancelButton = (Button) dialog.findViewById(R.id.cancel_btn);
            Button enterButton = (Button) dialog.findViewById(R.id.enterBtn);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            enterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createGame(text.getText().toString());
                    dialog.dismiss();
                }
            });

            dialog.show();

        }
    }

    // game adapter
    public class GameArrayAdapter extends ArrayAdapter<Game> {
        public GameArrayAdapter(Context context, ArrayList<Game> games) {
            super(context, 0, games);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Game game = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.game_record, parent, false);
            }
            TextView gameID = (TextView) convertView.findViewById(R.id.gameID);
            TextView gameName = (TextView) convertView.findViewById(R.id.game_record_name);
            TextView ticketsStat = (TextView) convertView.findViewById(R.id.game_record_tickets_text);
            TextView playersStat = (TextView) convertView.findViewById(R.id.game_record_players_text);

            ImageView team0 = (ImageView) convertView.findViewById(R.id.team0img);
            ImageView team1 = (ImageView) convertView.findViewById(R.id.team1img);
            team0.setTag(game.gameID);
            team1.setTag(game.gameID);

            team0.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int gameId = (int) v.getTag();
                    m_cred.gameID = gameId;
                    m_cred.teamID = 0;
                    // join the game
                    joinGame();
                }
            });
            team1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int gameId = (int) v.getTag();
                    m_cred.gameID = gameId;
                    m_cred.teamID = 1;
                    joinGame();
                }
            });

            gameID.setText(game.gameID + "");
            gameName.setText(game.gameName + "");
            playersStat.setText(game.teamsCount[0] + "/" + game.playersCap + " Players " + game.teamsCount[1] + "/" + game.playersCap);
            ticketsStat.setText(game.ticketsCount[0] + "/" + game.ticketsCap + " Tickets " + game.ticketsCount[1] + "/" + game.ticketsCap);

            return convertView;
        }
    }

    // polling task
    public class Poller extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... arg0) {
            loadGames();
            try {
                Thread.sleep(1000);
            }catch (Exception e){}
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            new Poller().execute("");
        }
    }
}
