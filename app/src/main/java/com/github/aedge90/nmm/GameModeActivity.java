package com.github.aedge90.nmm;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GameModeActivity extends FragmentActivity {

    protected final static int RESULT_RESTART = Activity.RESULT_FIRST_USER + 1;
    
    protected Lock lock = new ReentrantLock();
    protected Condition selection = lock.newCondition();
    protected volatile boolean selected;
    private final Object mPauseLock = new Object();
    private boolean mPaused;

    volatile Move currMove;
    Thread gameThread;
    Options options;
    GridLayout fieldLayout;
    GameBoardView fieldView;
    TextView progressText;
    ProgressBar progressBar;
    ProgressUpdater progressUpdater;
    GameBoard field;
    int screenWidth;
    ImageView greenSector;

    Toast lastToast;
    final GameModeActivity THIS = this;

    volatile State state;
    protected enum State {
        SET, MOVEFROM, MOVETO, WAITING, KILL, GAMEOVER
    }

    Player currPlayer;
    Player playerBlack;
    Player playerWhite;

    private void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    StackTraceElement[] trace = e.getStackTrace();
                    String tracem = "";
                    for(int i=0; i<trace.length; i++){
                        tracem += trace[i] + "\n";
                    }
                    Log.e("GameModeActivity", "Uncaught Exception detected in thread {}" + t + e + "\n" + tracem);

                    final String message = e.getMessage();

                    //display message so that the user sees that something went wrong
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                            new AlertDialog.Builder(THIS)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setTitle("Error")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNeutralButton("Quit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            })
                            .show();
                        }
                    });

                }
            });
        } catch (SecurityException e) {
            Log.e("GameModeActivity", "Could not set the Default Uncaught Exception Handler" + e);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setDefaultUncaughtExceptionHandler();

        options = getIntent().getParcelableExtra("own.projects.lemiroapp.Options");

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;

        setContentView(R.layout.gameboard_layout);
        fieldLayout = (GridLayout) findViewById(R.id.field);
        progressText = (TextView) findViewById(R.id.progressText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressUpdater = new ProgressUpdater(progressBar, this);

        currMove = null;
        fieldView = new GameBoardView(THIS, fieldLayout);

        mPaused = false;
        
        init();

        gameThread.start();
        
    }

    protected void init(){

        state = State.WAITING;

        playerBlack = options.playerBlack;
        playerWhite = options.playerWhite;
        playerBlack.setOtherPlayer(playerWhite);
        playerWhite.setOtherPlayer(playerBlack);

        // Mill Settings are Set
        if (options.millVariant == Options.MillVariant.MILL5) {
            playerBlack.setSetCount(5);
            playerWhite.setSetCount(5);
            field = new Mill5();
            fieldLayout.setBackgroundResource(R.drawable.gameboard5);
        } else if (options.millVariant == Options.MillVariant.MILL7) {
            playerBlack.setSetCount(7);
            playerWhite.setSetCount(7);
            field = new Mill7();
            fieldLayout.setBackgroundResource(R.drawable.gameboard7);
        } else if (options.millVariant == Options.MillVariant.MILL9) {
            playerBlack.setSetCount(9);
            playerWhite.setSetCount(9);
            field = new Mill9();
            fieldLayout.setBackgroundResource(R.drawable.gameboard9);
        }
        selected = false;

        setSectorListeners();

        gameThread = createGameThread();

    }

    Thread createGameThread(){

        Runnable game = new Runnable(){

            @Override
            public void run(){

                Strategy playerBlackBrain = null;
                Strategy playerWhiteBrain = null;
                if(playerWhite.getDifficulty() != null) {
                    playerWhiteBrain = new Strategy(field, playerWhite, progressUpdater);
                }
                if(playerBlack.getDifficulty() != null) {
                    playerBlackBrain = new Strategy(field, playerBlack, progressUpdater);
                }

                if(options.whoStarts.equals(playerWhite.getColor())){
                    currPlayer = playerWhite;
                }else{
                    currPlayer = playerBlack;
                }

                try {
                    while(true){

                        if(currPlayer.getDifficulty() == null) {
                            humanTurn(currPlayer);
                        }else{
                            if(currPlayer.getColor().equals(Options.Color.WHITE)) {
                                botTurn(currPlayer, playerWhiteBrain);
                            }else{
                                botTurn(currPlayer, playerBlackBrain);
                            }
                        }

                        if(ShowGameOverMessageIfWon()){
                            state = State.GAMEOVER;
                            break;
                        }
                        if(field.getState(currPlayer).equals(GameBoard.GameState.REMIS)
                                && field.getRemisCount() % Math.round(GameBoard.REMISMAX/2) == 0){    //only ask every REMISMAX/2 moves if there was a remis
                            showRemisMsg();
                            waitforSelection();
                        }

                        currPlayer = currPlayer.getOtherPlayer();

                        //if the activity was paused wait here until it is resumed
                        synchronized (mPauseLock) {
                            while (mPaused) {
                                mPauseLock.wait();
                            }
                        }

                    }
                } catch ( InterruptedException e ) {
                    Log.d("GameModeActivity", "Interrupted!");
                    e.printStackTrace();
                    gameThread.interrupt();
                }
            }

        };

        return new Thread(game);

    }

    @Override
    public void onPause() {
        super.onPause();
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            new AlertDialog.Builder(THIS)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(getResources().getString(R.string.options))
            .setMessage(getResources().getString(R.string.what_do))
            .setPositiveButton(getResources().getString(R.string.quit_game), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    quitGame();
                }
            })
            .setNegativeButton(getResources().getString(R.string.cancel), null)
            .setNeutralButton(getResources().getString(R.string.start_new_game), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startNewGame();
                }
            })
            .show();
            return true;
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }

    private void startNewGame() {
        setResult(RESULT_RESTART);
        gameThread.interrupt();
        finish();
    }

    private void quitGame() {
        setResult(RESULT_CANCELED);
        gameThread.interrupt();
        finish();
    }

    private void signalSelection(){
        lock.lock();
        selected = true;
        selection.signal();
        lock.unlock();
    }

    private void waitforSelection() throws InterruptedException{
        lock.lock();
        try {
            while(!selected) { //necessary to avoid lost wakeup
                selection.await();
            }
        } finally {
            selected = false;
            lock.unlock();
        }
    }

    void setSectorListeners() {

        for (int y = 0; y < GameBoard.LENGTH; y++) {
            for (int x = 0; x < GameBoard.LENGTH; x++) {
                if (!field.getColorAt(x, y).equals(Options.Color.INVALID)) {
                    fieldView.getPos(new Position(x, y)).setOnClickListener(
                            new  OnFieldClickListener(x,y));
                }
            }
        }
    }

    void humanTurn(final Player human) throws InterruptedException{

        currMove = null;
        Position newPosition;
        // wait until the last animation is finished before waiting for the selection, as otherwise a selection can
        // be chosen while the animation plays, which seems not right
        fieldView.waitforAnimation();

        refreshTextHuman(human);

        if(human.getSetCount() <= 0){
            state = State.MOVEFROM;
            // wait until a source piece and its destination position is chosen
            waitforSelection();
            fieldView.makeMove(currMove, human.getColor(), new OnFieldClickListener(currMove.getSrc()), new OnFieldClickListener(currMove.getDest()));
        }else{
            state = State.SET;
            // wait for human to set
            waitforSelection();
            fieldView.makeSetMove(currMove, human.getColor(), new OnFieldClickListener(currMove.getDest()));
        }
        newPosition = currMove.getDest();

        field.executeSetOrMovePhase(currMove, human);

        if (field.isInMill(newPosition, human.getColor())) {
            Position[] mill = field.getMill(newPosition, human.getColor());
            fieldView.waitforAnimation();
            fieldView.paintMillOnUIThread(mill);
            fieldView.waitforAnimation();
            state = State.KILL;
            //wait until kill is chosen
            waitforSelection();
            fieldView.unpaintMillOnUIThread();
            fieldView.waitforAnimation();
            fieldView.animateKill(currMove.getKill(), new OnFieldClickListener(currMove.getKill()));

            field.executeKillPhase(currMove, human);
        }

    }

    void botTurn(final Player bot, Strategy brain) throws InterruptedException{

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    fieldView.waitforAnimation();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                refreshTextBot(bot);
                progressUpdater.setActive(true);
            }
        });

        t.start();

        Position newPosition;
        currMove = brain.computeMove();

        // Thread t waits until the last animation is finished while computing the move is done in this thread
        // when the animation is finished the text, that its the bots turn that started computing already, is shown
        // and the progress is shown. Wait here until the animation is done AND the move is computed
        t.join();

        // inactivate progress until the animations are finished
        progressUpdater.setActive(false);

        if(bot.getSetCount() <= 0){
            fieldView.makeMove(currMove, bot.getColor(), new OnFieldClickListener(currMove.getSrc()), new OnFieldClickListener(currMove.getDest()));

        }else{
            fieldView.makeSetMove(currMove, bot.getColor(), new OnFieldClickListener(currMove.getDest()));
        }
        newPosition = currMove.getDest();

        field.executeSetOrMovePhase(currMove, bot);

        if (currMove.getKill() != null) {
            Position[] mill = field.getMill(newPosition, bot.getColor());
            fieldView.waitforAnimation();
            fieldView.paintMillOnUIThread(mill);
            fieldView.waitforAnimation();

            Thread.sleep(GameBoardView.ANIM_DURATION);

            fieldView.unpaintMillOnUIThread();
            fieldView.waitforAnimation();
            fieldView.animateKill(currMove.getKill(), new OnFieldClickListener(currMove.getKill()));

            field.executeKillPhase(currMove, bot);
        }

    }

    private void refreshTextBot(Player bot){
        if(bot.getOtherPlayer().getDifficulty() == null) {
            setTextinUIThread(progressText, R.string.bots_turn);
        }else{
            String localizedDifficulty = getString(getResources().getIdentifier(bot.getDifficulty().name(), "string", getPackageName()));
            String s = getString(R.string.turn_of);
            if(bot.getColor().equals(Options.Color.BLACK)) {
                s += " " + getString(R.string.black) + " (" + localizedDifficulty + ")" ;
            }else{
                s += " " + getString(R.string.white) + " (" + localizedDifficulty + ")" ;
            }
            setTextinUIThread(progressText, s);
        }
    }

    private void refreshTextHuman(Player human){
        if(human.getOtherPlayer().getDifficulty() != null) {
            setTextinUIThread(progressText, R.string.your_turn);
        }else{
            if(human.getColor().equals(Options.Color.BLACK)) {
                setTextinUIThread(progressText, R.string.black_turn);
            }else{
                setTextinUIThread(progressText, R.string.white_turn);
            }
        }
    }

    //shows a Toast and cancels others if they are showing
    protected void showToast(String text){
        if(lastToast != null) {
            lastToast.cancel();
        }
        Toast toast = Toast.makeText(this,text ,Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM,0,0);
        toast.show();
        lastToast = toast;
    }
    
    void setTextinUIThread(final TextView view, final String text){
        runOnUiThread(new Runnable() {
            public void run() {
                view.setText(text);
            }
        });
    }
    
    void setTextinUIThread(final TextView view, final int stringID){
        runOnUiThread(new Runnable() {
            public void run() {
                view.setText(getString(stringID));
            }
        });
    }

    boolean ShowGameOverMessageIfWon() {

        String message;
        if(currPlayer.getDifficulty() == null && currPlayer.getOtherPlayer().getDifficulty() != null) {
            //the winning player is a human and the bot has lost
            message = getResources().getString(R.string.you_have_won);
            if(field.getState(currPlayer).equals(GameBoard.GameState.WON_NO_MOVES)){
                showGameOverMsg(message, getResources().getString(R.string.won_no_moves_human));
                setTextinUIThread(progressText, message);
                return true;
            }else if (field.getState(currPlayer).equals(GameBoard.GameState.WON_KILLED_ALL)) {
                showGameOverMsg(message, getResources().getString(R.string.won_no_pieces_human));
                setTextinUIThread(progressText, message);
                return true;
            }
        }else if (currPlayer.getDifficulty() != null && currPlayer.getOtherPlayer().getDifficulty() == null){
            //bot has won against a human player
            message = getResources().getString(R.string.you_have_lost);
            if(field.getState(currPlayer).equals(GameBoard.GameState.WON_NO_MOVES)){
                showGameOverMsg(message, getResources().getString(R.string.lost_no_moves));
                setTextinUIThread(progressText, message);
                return true;
            }else if (field.getState(currPlayer).equals(GameBoard.GameState.WON_KILLED_ALL)) {
                showGameOverMsg(message, getResources().getString(R.string.lost_no_pieces));
                setTextinUIThread(progressText, message);
                return true;
            }
        }else{
            //bot has won against another bot. Or human has won against another human
            String winningColor;
            if(currPlayer.getColor().equals(Options.Color.BLACK)){
                winningColor = getResources().getString(R.string.black);
            }else{
                winningColor = getResources().getString(R.string.white);
            }
            message = winningColor + " " + getResources().getString(R.string.has_won);
            if(field.getState(currPlayer).equals(GameBoard.GameState.WON_NO_MOVES)){
                showGameOverMsg(message, getResources().getString(R.string.won_no_moves_bot));
                setTextinUIThread(progressText, message);
                return true;
            }else if (field.getState(currPlayer).equals(GameBoard.GameState.WON_KILLED_ALL)) {
                showGameOverMsg(message, getResources().getString(R.string.won_no_pieces_bot));
                setTextinUIThread(progressText, message);
                return true;
            }
        }

        return false;
    }
    
    private void showGameOverMsg(final String title, final String message){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable(){

                @Override
                public void run() {

                    new AlertDialog.Builder(THIS)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(getResources().getString(R.string.quit_game), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            quitGame();
                        }
                    })
                    .setNeutralButton(getResources().getString(R.string.start_new_game), new DialogInterface.OnClickListener(){
                       @Override
                       public void onClick(DialogInterface dialogInterface, int id) {
                           //dont ask if player really want to start new game as game is over here
                           setResult(RESULT_OK);
                           finish();
                       }
                    })
                    .setNegativeButton(getResources().getString(R.string.show_gameboard), null)
                    .show();
                }
            });
     }

    private void showRemisMsg(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final String message = String.format(getString(R.string.remis_message), field.getRemisCount());
        runOnUiThread(new Runnable(){

            @Override
            public void run() {

                new AlertDialog.Builder(THIS)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(getString(R.string.remis_title))
                        .setMessage(message)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int id) {
                                signalSelection();
                            }
                        })
                        .setNegativeButton(getString(R.string.start_new_game), new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialogInterface, int id) {
                                startNewGame();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }

    protected class OnFieldClickListener implements View.OnClickListener {

        final int x;
        final int y;

        OnFieldClickListener(int x ,int y){
            this.x = x;
            this.y = y;
        }

        OnFieldClickListener(Position pos){
            this.x = pos.getX();
            this.y = pos.getY();
        }

        @Override
        public void onClick(View arg0) {

            if(state == State.GAMEOVER){
                showToast(getString(R.string.game_is_over));
            }
            if(playerWhite.getDifficulty() != null && playerBlack.getDifficulty() != null) {
                showToast(getString(R.string.clicking_in_botvsbot));
                return;
            }

            if (state == State.SET) {
                if(field.getColorAt(new Position(x,y)).equals(currPlayer.getColor())
                        || field.getColorAt(new Position(x,y)).equals((currPlayer.getOtherPlayer().getColor()))){
                    showToast(getResources().getString(R.string.pos_occupied));
                }else{
                    Position pos = new Position(x, y);
                    currMove = new Move(pos, null, null);
                    state = State.WAITING;
                    signalSelection();
                }
            } else if (state == State.MOVEFROM) {
                if(!(field.getColorAt(new Position(x,y)).equals(currPlayer.getColor()))){
                    showToast(getResources().getString(R.string.nothing_to_move));
                }else{
                    greenSector = fieldView.createSector(Options.Color.GREEN, x, y);
                    fieldLayout.addView(greenSector);
                    //set invalid position for now so that constructor doesnt throw IllegalArgumentException
                    currMove = new Move(new Position(-1,-1), new Position(x,y), null);
                    state = State.MOVETO;
                }
            } else if (state == State.MOVETO) {
                if(!field.movePossible(currMove.getSrc(), new Position(x,y))){
                    state = State.MOVEFROM;
                    //signal that currMove could not be set
                    currMove = null;
                    fieldLayout.removeView(greenSector);
                    showToast(getResources().getString(R.string.can_not_move));
                }else{
                    fieldLayout.removeView(greenSector);
                    currMove = new Move(new Position(x,y), currMove.getSrc(), null);
                    state = State.WAITING;
                    signalSelection();
                }
            } else if (state == State.WAITING) {
                showToast(getResources().getString(R.string.not_your_turn));
            }else if (state == State.KILL) {
                if(!(field.getColorAt(new Position(x,y)).equals(currPlayer.getOtherPlayer().getColor()))){
                    showToast(getResources().getString(R.string.nothing_to_kill));
                }else if(field.isInMill(new Position(x,y), currPlayer.getOtherPlayer().getColor())){
                    //if every single stone of enemy is part of a mill we are allowed to kill
                    LinkedList<Position> enemypos = field.getPositions(currPlayer.getOtherPlayer().getColor());
                    boolean allInMill = true;
                    for(int i = 0; i<enemypos.size(); i++){
                        if(!field.isInMill(enemypos.get(i), currPlayer.getOtherPlayer().getColor())){
                            allInMill = false;
                            break;
                        }
                    }
                    if(allInMill){
                        Position killPos = new Position(x, y);
                        currMove = new Move(currMove.getDest(), currMove.getSrc(), killPos);
                        state = State.WAITING;
                        signalSelection();
                    }else{
                        showToast(getResources().getString(R.string.cannot_kill_mill));
                    }
                }else{
                    Position killPos = new Position(x, y);
                    currMove = new Move(currMove.getDest(), currMove.getSrc(), killPos);
                    state = State.WAITING;
                    signalSelection();
                }
            }

        }
    }
}
