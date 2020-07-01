package org.secuso.privacyfriendlysudoku.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.Symbol;
import org.secuso.privacyfriendlysudoku.controller.helper.GameInfoContainer;
import org.secuso.privacyfriendlysudoku.controller.qqwing.QQWing;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.ui.listener.IFinalizeDialogFragmentListener;
import org.secuso.privacyfriendlysudoku.ui.view.CreateSudokuSpecialButtonLayout;
import org.secuso.privacyfriendlysudoku.ui.view.R;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuFieldLayout;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuKeyboardLayout;
import org.secuso.privacyfriendlysudoku.ui.view.SudokuSpecialButtonLayout;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

public class CreateSudokuActivity extends BaseActivity implements IFinalizeDialogFragmentListener {

    GameController gameController;
    SudokuFieldLayout layout;
    SudokuKeyboardLayout keyboard;
    TextView viewName ;
    CreateSudokuSpecialButtonLayout specialButtonLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPref.getBoolean("pref_dark_mode_setting", false )) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        if(sharedPref.getBoolean("pref_keep_screen_on", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        gameController = new GameController(sharedPref, getApplicationContext());

        if(sharedPref.getBoolean("pref_keep_screen_on", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        Bundle extras = getIntent().getExtras();
        GameType gameType = GameType.valueOf(extras.getString("gameType", GameType.Default_9x9.name()));
        int sectionSize = gameType.getSize();
        int boardSize = sectionSize * sectionSize;

        GameInfoContainer container = new GameInfoContainer(0, GameDifficulty.Unspecified,
                gameType, new int [boardSize], new int [boardSize], new boolean [boardSize][sectionSize]);
        gameController.loadLevel(container);

        setContentView(R.layout.activity_create_sudoku);
        layout = (SudokuFieldLayout)findViewById(R.id.sudokuLayout);
        layout.setSettingsAndGame(sharedPref, gameController);

        keyboard = (SudokuKeyboardLayout) findViewById(R.id.sudokuKeyboardLayout);
        keyboard.removeAllViews();
        keyboard.setGameController(gameController);
        Point p = new Point();
        getWindowManager().getDefaultDisplay().getSize(p);

        int orientation = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ?
                LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;

        keyboard.setKeyBoard(gameController.getSize(), p.x,layout.getHeight()-p.y, orientation);

        specialButtonLayout = (CreateSudokuSpecialButtonLayout) findViewById(R.id.createSudokuLayout);
        specialButtonLayout.setButtons(p.x, gameController, keyboard, getFragmentManager(), orientation, CreateSudokuActivity.this, this);

        viewName = (TextView) findViewById(R.id.gameModeText);
        viewName.setText(getString(gameController.getGameType().getStringResID()));

        gameController.notifyHighlightChangedListeners();

    }

    @Override
    public void onResume(){
        super.onResume();

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Symbol s;
        try {
            s = Symbol.valueOf(sharedPref.getString("pref_symbols", Symbol.Default.name()));
        } catch(IllegalArgumentException e) {
            s = Symbol.Default;
        }
        layout.setSymbols(s);
        keyboard.setSymbols(s);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    public void onFinalizeDialogPositiveClick() {
        Toast.makeText(CreateSudokuActivity.this, R.string.verify_custom_sudoku_process_toast, Toast.LENGTH_SHORT).show();
        GameType gameType = gameController.getGameType();
        int boardSize = gameType.getSize() * gameType.getSize();
        String boardContent = gameController.getCodeOfField();
        GameInfoContainer container = new GameInfoContainer(0, GameDifficulty.Unspecified,
                gameType, new int [boardSize], new int [boardSize], new boolean [boardSize][gameType.getSize()]);
        container.parseFixedValues(boardContent);

        QQWing verifier = new QQWing(gameType, GameDifficulty.Unspecified);
        verifier.setRecordHistory(true);
        verifier.setPuzzle(container.getFixedValues());
        boolean solvable = verifier.solve();

        if(solvable) {
            Toast.makeText(CreateSudokuActivity.this, R.string.finished_verifying_custom_sudoku_toast, Toast.LENGTH_LONG).show();
            final Intent intent = new Intent(this, GameActivity.class);
            intent.setData(Uri.parse("sudoku://" + boardContent));
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(CreateSudokuActivity.this, R.string.failed_to_verify_custom_sudoku_toast, Toast.LENGTH_LONG).show();
        }

    }

    public void onDialogNegativeClick() {

    }
}
