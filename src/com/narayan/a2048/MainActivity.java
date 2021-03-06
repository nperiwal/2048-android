package com.narayan.a2048;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerLib;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.plus.Plus;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.sdk.InMobiSdk;
import com.narayan.a2048.basegameutils.BaseGameUtils;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Automatically start the sign-in flow when the Activity starts
    private boolean mAutoStartSignInFlow = false;

    // request codes we use when invoking an external activity
    private static final int RC_RESOLVE = 5000;
    private static final int RC_UNUSED = 5001;
    private static final int RC_SIGN_IN = 9001;

    // tag for debug logging
    final boolean ENABLE_DEBUG = true;

    public static final String TAG = "MAIN_ACTIVITY";

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SCORE = "score";
    private static final String HIGH_SCORE = "high score temp";
    private static final String UNDO_SCORE = "undo score";
    private static final String CAN_UNDO = "can undo";
    private static final String UNDO_GRID = "undo";
    private static final String GAME_STATE = "game state";
    private static final String UNDO_GAME_STATE = "undo game state";
    private MainView view;

    private RelativeLayout mAdRelativeLayout;

    InterstitialAd mInterstitialAd;

    private static final long BANNER_PLACEMENT_ID = 1458409275434l;
    private static final long INTERSTITIAL_PLACEMENT_ID = 1458857929542l;
    public InMobiInterstitial inMobiInterstitial;
    InMobiInterstitial.InterstitialAdListener interstitialAdListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppsFlyerLib.setAppsFlyerKey(getString(R.string.apps_flyer_dev_key));
        AppsFlyerLib.sendTracking(getApplicationContext());

        InMobiSdk.init(getApplicationContext(), getString(R.string.inmobi_account_id));
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG);

        mAdRelativeLayout = new RelativeLayout(this);

        super.onCreate(savedInstanceState);
        view = new MainView(this);

        mAdRelativeLayout.addView(view);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        view.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }
        setContentView(mAdRelativeLayout);
        placeInmobiBannerAd();
        placeInmobiInterstitialAd();

        //placeAdView();
        //configureInterstitialAd();
    }

    public void placeInmobiInterstitialAd() {
        setInmobiInterstitialListener();
        inMobiInterstitial = new InMobiInterstitial(this, INTERSTITIAL_PLACEMENT_ID,
                interstitialAdListener);
        requestInmobiInterstitialAd();
    }

    public void requestInmobiInterstitialAd() {
        if (inMobiInterstitial != null) {
            if (inMobiInterstitial.isReady()) {
                inMobiInterstitial.show();
            } else {
                inMobiInterstitial.load();
            }
        }
    }

    public void setInmobiInterstitialListener() {
        interstitialAdListener = new InMobiInterstitial.InterstitialAdListener() {
            @Override
            public void onAdLoadSucceeded(InMobiInterstitial ad) {
            }
            @Override
            public void onAdLoadFailed(InMobiInterstitial ad, InMobiAdRequestStatus requestStatus) {}
            @Override
            public void onAdDisplayed(InMobiInterstitial ad) {}
            @Override
            public void onAdDismissed(InMobiInterstitial ad) {
                inMobiInterstitial.load();
            }
            @Override
            public void onAdInteraction(InMobiInterstitial ad, Map<Object, Object> params) {}
            @Override
            public void onAdRewardActionCompleted(InMobiInterstitial ad, Map<Object, Object> rewards) {}
            @Override
            public void onUserLeftApplication(InMobiInterstitial ad) {}
        };
    }

    private void placeInmobiBannerAd() {
        InMobiBanner imbanner = new InMobiBanner(this, BANNER_PLACEMENT_ID);
        int width = toPixelUnits(320);
        int height= toPixelUnits(50);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        mAdRelativeLayout.addView(imbanner, params);
        imbanner.setRefreshInterval(45);
        imbanner.load();
    }

    /*private void configureInterstitialAd(){
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id1));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });

        requestNewInterstitial();
    }

    public void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    private void placeAdView() {

        AdView mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.BANNER);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        mAdView.setLayoutParams(params);

        mAdRelativeLayout.addView(mAdView);

        mAdView.setAdUnitId(getString(R.string.banner_ad_unit_id1));


        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }*/

    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): connecting");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("hasState", true);
        save();
    }

    protected void onPause() {
        super.onPause();
        save();
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        Tile[][] undoField = view.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, (int)field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, (int)undoField[xx][yy].getValue());
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, view.game.score);
        editor.putLong(HIGH_SCORE, view.game.highScore);
        editor.putLong(UNDO_SCORE, view.game.lastScore);
        editor.putBoolean(CAN_UNDO, view.game.canUndo);
        editor.putInt(GAME_STATE, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
        editor.commit();
    }

    protected void onResume() {
        Log.d(TAG, "onResume():");
        super.onResume();
        load();
    }

    private void load() {
        Log.d(TAG, "onLoad():");
        //Stopping all animations
        view.game.aGrid.cancelAnimations();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                int value = settings.getInt(xx + " " + yy, -1);
                if (value > 0) {
                    view.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    view.game.grid.field[xx][yy] = null;
                }

                int undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0) {
                    view.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (value == 0) {
                    view.game.grid.undoField[xx][yy] = null;
                }
            }
        }

        view.game.score = settings.getLong(SCORE, view.game.score);
        view.game.highScore = settings.getLong(HIGH_SCORE, view.game.highScore);
        view.game.lastScore = settings.getLong(UNDO_SCORE, view.game.lastScore);
        view.game.canUndo = settings.getBoolean(CAN_UNDO, view.game.canUndo);
        view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState);
        view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult()");
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected(): connected to Google APIs");

        // Set the greeting appropriately on main menu
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): attempting to connect");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(): attempting to resolve");
        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed(): already resolving");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;
            if (!BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }
    }

    public void pushCurrentScoreToLeaderboard(long score) {
        Log.d(TAG, "onPushCurrentScoreToLeaderboard)");
        if (isSignedIn()) {
            Log.d(TAG, "onPushCurrentScoreToLeaderboard : signed in)");

            Games.Leaderboards.loadCurrentPlayerLeaderboardScore(mGoogleApiClient,
                    getString(R.string.leaderboard_high_scores), LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    LeaderboardVariant.COLLECTION_PUBLIC )
                    .setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
                        @Override
                        public void onResult(Leaderboards.LoadPlayerScoreResult arg0) {
                            LeaderboardScore c = arg0.getScore();
                            if (c != null) {
                                System.out.println("raw: " + c.getRawScore());
                                if (c.getRawScore() < view.game.highScore) {
                                    if (isSignedIn()) {
                                        Games.Leaderboards.submitScore(mGoogleApiClient,
                                                getString(R.string.leaderboard_high_scores),
                                                view.game.highScore);
                                    }
                                }
                            } else {
                                System.out.println("Could not fetch the score");
                            }
                        }
                    });

            Games.Leaderboards.submitScore(mGoogleApiClient,
                    getString(R.string.leaderboard_high_scores), score);


        }
        if (view!=null && view.game!=null) {
            if (score >= view.game.highScore) {
                Map<String, Object> eventValue = new HashMap<String, Object>();
                if (score >= 1000 && score < 2000) {
                    eventValue.put(AFInAppEventParameterName.LEVEL, 1);
                    eventValue.put(AFInAppEventParameterName.SCORE, 1000);
                    AppsFlyerLib.trackEvent(getApplicationContext(), AFInAppEventType.LEVEL_ACHIEVED, eventValue);
                } else if (score >= 2000 && score < 5000) {
                    eventValue.put(AFInAppEventParameterName.LEVEL, 2);
                    eventValue.put(AFInAppEventParameterName.SCORE, 2000);
                    AppsFlyerLib.trackEvent(getApplicationContext(), AFInAppEventType.LEVEL_ACHIEVED, eventValue);
                } else if (score >= 5000 && score < 10000) {
                    eventValue.put(AFInAppEventParameterName.LEVEL, 3);
                    eventValue.put(AFInAppEventParameterName.SCORE, 5000);
                    AppsFlyerLib.trackEvent(getApplicationContext(), AFInAppEventType.LEVEL_ACHIEVED, eventValue);
                } else if (score >= 10000 && score < 50000) {
                    eventValue.put(AFInAppEventParameterName.LEVEL, 4);
                    eventValue.put(AFInAppEventParameterName.SCORE, 10000);
                    AppsFlyerLib.trackEvent(getApplicationContext(), AFInAppEventType.LEVEL_ACHIEVED, eventValue);
                } else if (score >= 50000 && score < 100000) {
                    eventValue.put(AFInAppEventParameterName.LEVEL, 5);
                    eventValue.put(AFInAppEventParameterName.SCORE, 50000);
                    AppsFlyerLib.trackEvent(getApplicationContext(), AFInAppEventType.LEVEL_ACHIEVED, eventValue);
                } else if (score >= 100000) {
                    eventValue.put(AFInAppEventParameterName.LEVEL, 6);
                    eventValue.put(AFInAppEventParameterName.SCORE, 100000);
                    AppsFlyerLib.trackEvent(getApplicationContext(), AFInAppEventType.LEVEL_ACHIEVED, eventValue);
                }
            }
        }
    }

    public void startLeaderboard() {
        Log.d(TAG, "onStartLeaderboard()");
        mSignInClicked = true;
        if (isSignedIn()) {
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient,
                    getString(R.string.leaderboard_high_scores)), RC_UNUSED);
        } else {
            mGoogleApiClient.connect();
        }
    }

    private int toPixelUnits(int dipUnit) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dipUnit * density);
    }

}
