package com.github.aedge90.nmm;


import android.test.mock.MockContext;
import android.widget.ProgressBar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotEquals;


// TODO check if bots are smarter for the tests where easy bots are excluded


@RunWith(value = Parameterized.class)
public class StrategyTestParameterized {

    private final Options.Color P1;
    private final Options.Color P2;
    private final Options.Color N = Options.Color.NOTHING;
    private final Options.Color I = Options.Color.INVALID;

    private Player mPlayer1;
    private Player mPlayer2;
    private int nThreads;
    private final static int maxThreads = 9;

    // name attribute is optional, provide an unique name for test
    // multiple parameters, uses Collection<Object[]>
    @Parameterized.Parameters(name = "P1 is {0}, nThreads: {1}")
    public static Collection<Object[] > data() {

        LinkedList<Object[]> player1andnThreadsList = new LinkedList<>();
        for (int i = 0; i < Options.Difficulties.values().length; i++) {

            Player playerBlack = new Player(Options.Color.BLACK);
            Player playerWhite = new Player(Options.Color.WHITE);
            playerBlack.setDifficulty(Options.Difficulties.values()[i]);
            playerWhite.setDifficulty(Options.Difficulties.values()[i]);
            playerBlack.setSetCount(5);
            playerWhite.setSetCount(5);

            for (int nThreads = 1; nThreads <= maxThreads; nThreads++) {
                player1andnThreadsList.add(new Object[]{playerBlack, nThreads});
                player1andnThreadsList.add(new Object[]{playerWhite, nThreads});
            }

        }

        return player1andnThreadsList;
    }

    // Inject paremeters via constructor, constructor is called before each test
    public StrategyTestParameterized(Player player1, int nThreads){

        mPlayer1 = player1;
        if(mPlayer1.getColor().equals(Options.Color.BLACK)){
            mPlayer2 = new Player(Options.Color.WHITE);
        } else {
            mPlayer2 = new Player(Options.Color.BLACK);
        }
        mPlayer2.setDifficulty(mPlayer1.getDifficulty());
        mPlayer2.setSetCount(mPlayer1.getSetCount());
        mPlayer2.setOtherPlayer(mPlayer1);
        mPlayer1.setOtherPlayer(mPlayer2);

        P1 = mPlayer1.getColor();
        P2 = mPlayer2.getColor();

        this.nThreads = nThreads;
    }

    @Test
    public void computeMoveShouldFormPotentialMills () throws InterruptedException {

        Options.Color[][] mill5 =
                {{N , I , I , N , I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , N , N , N , I , I },
                { N , I , N , I , P1, I , N },
                { I , I , N , N , N , I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(4);
        mPlayer2.setSetCount(4);

        Move result = strategy.computeMove();

        assertThat(result.getDest(), anyOf(is(new Position(4,2)), is(new Position(4,4))));
    }

    @Test
    public void computeMoveShouldFormTwoPotentialMillsInOneMove () throws InterruptedException {

        //check if the bot prevents both potential mills instead of only one, in which case P1 could definitely close a mill

        Options.Color[][] mill9 =
                {{N , I , I , N , I , I , N },
                { I , N , I , N , I , N , I },
                { I , I , P1, N , N , I , I },
                { N , N , P2, I , N , N , N },
                { I , I , P2, N , P1, I , I },
                { I , N , I , N , I , N , I },
                { N , I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill9(mill9);

        mPlayer1.setSetCount(7);
        mPlayer2.setSetCount(7);

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());

        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        Move result = strategyP1.computeMove();

        assertEquals(new Position(4,2), result.getDest());

    }

    @Test
    public void computeMoveShouldPreventPotentialMill () throws InterruptedException {

        Options.Color[][] mill5 =
                {{N , I , I , N , I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , N , N , N , I , I },
                { N , I , N , I , N , I , N },
                { I , I , N , N , P2, I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill5(mill5);

        mPlayer1.setSetCount(5);
        mPlayer2.setSetCount(4);

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());

        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(4);
        mPlayer2.setSetCount(4);

        Move result = strategyP1.computeMove();

        assertThat(result.getDest(), anyOf(is(new Position(4,2)), is(new Position(4,3)), is(new Position(3,4)), is(new Position(2,4))));

    }

    @Test
    public void computeMoveShouldPreventTwoPotentialMillsInOneMove () throws InterruptedException {

        //check if the bot prevents both potential mills instead of only one, in which case P1 could definitely close a mill

        Options.Color[][] mill9 =
                {{N , I , I , N , I , I , N },
                { I , N , I , N , I , N , I },
                { I , I , P2, N , N , I , I },
                { N , N , P1, I , N , N , N },
                { I , I , N , N , P2, I , I },
                { I , N , I , N , I , N , I },
                { N , I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill9(mill9);

        mPlayer1.setSetCount(7);
        mPlayer2.setSetCount(8);

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());

        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        Move result = strategyP1.computeMove();

        assertEquals(new Position(4,2), result.getDest());

    }

    @Test
    public void computeMoveShouldPreventMillWhileSetting () throws InterruptedException {

        Options.Color[][] mill5 =
                {{N , I , I , P1, I , I , P2},
                { I , I , I , I , I , I , I },
                { I , I , P1, N , N , I , I },
                { N , I , N , I , N , I , N },
                { I , I , P2, P2, N , I , I },
                { I , I , I , I , I , I , I },
                { P1, I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(2);
        mPlayer2.setSetCount(2);

        Move result = strategy.computeMove();

        assertEquals(new Position(4, 4), result.getDest());
        assertEquals(null, result.getSrc());

    }

    @Test
    public void computeMoveShouldCloseMill() throws InterruptedException {

        Options.Color[][] mill5 =
                {{N , I , I , P1, I , I , P2},
                { I , I , I , I , I , I , I },
                { I , I , P1, N , P2, I , I },
                { P1, I , P1, I , N , I , N },
                { I , I , P2, N , N , I , I },
                { I , I , I , I , I , I , I },
                { P1, I , I , P2, I , I , N}};

        GameBoard gameBoard = new Mill5(mill5);

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result = strategy.computeMove();

        assertEquals(new Position(0, 0), result.getDest());
        assertEquals(new Position(3, 0), result.getSrc());

    }

    @Test
    public void computeMoveShouldUseDoubleMill() throws InterruptedException {

        Options.Color[][] mill5 =
                {{P1, I , I , N , I , I , P2},
                { I , I , I , I , I , I , I },
                { I , I , P1, N , P2, I , I },
                { P1, I , N , I , N , I , N },
                { I , I , P1, N , P2, I , I },
                { I , I , I , I , I , I , I },
                { P1, I , I , P2, I , I , P2}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result1 = strategy.computeMove();

        assertEquals(new Position(2,3), result1.getDest());
        assertEquals(new Position(0,3), result1.getSrc());

        gameBoard.executeCompleteTurn(result1, mPlayer1);

        //just let black do the next move again, white cant do anything
        Move result2 = strategy.computeMove();

        assertEquals(new Position(0,3), result2.getDest());
        assertEquals(new Position(2,3), result2.getSrc());

    }

    @Test
    public void computeMoveShouldCloseMillAndPreventOtherPlayersMill() throws InterruptedException {

        Options.Color[][] mill5 =
                {{N , I , I , P1, I , I , P2},
                { I , I , I , I , I , I , I },
                { I , I , P1, N , N , I , I },
                { P1, I , P1, I , P2, I , N },
                { I , I , P2, N , N , I , I },
                { I , I , I , I , I , I , I },
                { P1, I , I , P2, I , I , P2}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result = strategy.computeMove();

        assertEquals(new Position(0, 0), result.getDest());
        assertEquals(new Position(3, 0), result.getSrc());

        assertNotEquals(new Position(2, 4), result.getKill());
        assertNotEquals(new Position(3, 6), result.getKill());

    }

    @Test
    // on difficulties, when the bot can see the field after 3 of his moves, he must see he has a closed mill then
    // the other player can do nothing about it in this constellation
    public void computeMoveShouldCloseMillOnHighDifficulties() throws InterruptedException {

        //workaround to skip easier difficulties
        if(mPlayer1.getDifficulty().equals(Options.Difficulties.EASIEST)
                || mPlayer1.getDifficulty().equals(Options.Difficulties.EASY)
                || mPlayer1.getDifficulty().equals(Options.Difficulties.NORMAL)){
            return;
        }

        Options.Color[][] mill5 =
                {{N , I , I , N , I , I , P2 },
                { I , I , I , I , I , I , I },
                { I , I , P1, P2, N , I , I },
                { P1, I , N , I , N , I , P1},
                { I , I , P2, N , P2, I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , N , I , I , P1}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategyPlayer1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);
        Strategy strategyPlayer2 = new Strategy(gameBoard, mPlayer2, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result1 = strategyPlayer1.computeMove();
        gameBoard.executeCompleteTurn(result1, mPlayer1);

        Move result2 = strategyPlayer2.computeMove();
        gameBoard.executeCompleteTurn(result2, mPlayer2);

        Move result3 = strategyPlayer1.computeMove();
        gameBoard.executeCompleteTurn(result3, mPlayer1);

        Move result4 = strategyPlayer2.computeMove();
        gameBoard.executeCompleteTurn(result4, mPlayer2);

        Move result5 = strategyPlayer1.computeMove();
        gameBoard.executeCompleteTurn(result5, mPlayer1);

        assertEquals(3, gameBoard.getPositions(mPlayer2.getColor()).size());

    }

    @Test
    //in this scenario P1 makes a mistake, P2 should compute the perfect move, so that P1 cant prevent loosing
    //then P1 should at least try to prevent a mill, although he cant prevent that the P2 still can close
    //his mill in another way. Is ok that P2 does not make the perfect move on EASIEST
    public void computeMoveShouldTryToPreventLoosingEvenIfItsImpossible() throws InterruptedException {

        if(mPlayer2.getDifficulty() == Options.Difficulties.EASIEST){
            return;
        }

        Options.Color[][] mill5 =
                {{N , I , I , N , I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , N , P2, P1, I , I },
                { N , I , N , I , P2, I , P1},
                { I , I , N , N , P2, I , I },
                { I , I , I , I , I , I , I },
                { P2, I , I , N , I , I , P1}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategyPlayer1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);
        Strategy strategyPlayer2 = new Strategy(gameBoard, mPlayer2, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result = new Move(new Position(6,0), new Position(4,2), new Position(4,3));

        gameBoard.executeCompleteTurn(result, mPlayer1);

        Move result2 = strategyPlayer2.computeMove();
        gameBoard.executeCompleteTurn(result2, mPlayer2);

        assertEquals(new Position(4,2), result2.getDest());

        Move result3 = strategyPlayer1.computeMove();
        gameBoard.executeCompleteTurn(result3, mPlayer1);

        assertThat(result3.getDest(), anyOf(is(new Position(2,2)), is(new Position(4,3))));
    }

    @Test
    public void computeMoveShouldCloseMillAndPreventOtherPlayersMillWhenJumping() throws InterruptedException {

        Options.Color[][] mill5 =
                {{P2, I , I , N , I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , N , N , P1, I , I },
                { N , I , N , I , N , I , P1},
                { I , I , P2, N , P2, I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , P2, I , I , P1}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result = strategy.computeMove();
        gameBoard.executeCompleteTurn(result, mPlayer1);

        assertEquals(new Position(6,0), result.getDest());
        assertThat(result.getKill(), anyOf(is(new Position(2,4)), is(new Position(4,4))));

    }

    @Test
    public void computeMoveShouldNotUndoHisMove() throws InterruptedException {

        Options.Color[][] mill5 =
                {{N , I , I , P1, I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , P1, P2, P1, I , I },
                { P1, I , N , I , N , I , N },
                { I , I , P2, P2, N , I , I },
                { I , I , I , I , I , I , I },
                { P2, I , I , P1, I , I , P2}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategyPlayer1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);
        Strategy strategyPlayer2 = new Strategy(gameBoard, mPlayer2, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        LinkedList<Position> positionsP1Before = gameBoard.getPositions(mPlayer1.getColor());

        Move result1 = strategyPlayer1.computeMove();
        gameBoard.executeCompleteTurn(result1, mPlayer1);

        strategyPlayer2.computeMove();
        //do not use this move, but compute it to check that it doesnt influence Player1s decision
        gameBoard.executeCompleteTurn(new Move(new Position(4,4), new Position(3,4), null), mPlayer2);

        Move result2 = strategyPlayer1.computeMove();
        gameBoard.executeCompleteTurn(result2, mPlayer1);

        LinkedList<Position> positionsP1After = gameBoard.getPositions(mPlayer1.getColor());

        assertNotEquals(positionsP1Before, positionsP1After);

    }

    @Test
    public void computeMoveShouldUndoHisMoveIfItClosesMill1() throws InterruptedException {

        Options.Color[][] mill5 =
                {{N , I , I , P1, I , I , P1 },
                { I , I , I , I , I , I , I },
                { I , I , P1, P2, N , I , I },
                { P1, I , N , I , N , I , P2},
                { I , I , P2, P2, N , I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , P1, I , I , P2}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result1 = strategy.computeMove();
        assertEquals(new Position(0,3), result1.getSrc());
        assertEquals(new Position(0,0), result1.getDest());

        //change kill of the move, as it may be another equally evaluated kill, but we want to test with this one
        result1 = new Move(result1.getDest(), result1.getSrc(), new Position(6,6));
        gameBoard.executeCompleteTurn(result1, mPlayer1);

        gameBoard.executeCompleteTurn(new Move(new Position(4,4), new Position(3,4), null), mPlayer2);

        Move result2 = strategy.computeMove();
        gameBoard.executeCompleteTurn(result2, mPlayer1);

        assertEquals(new Position(0,0), result2.getSrc());
        assertEquals(new Position(0,3), result2.getDest());

    }

    @Test
    public void computeMoveShouldUndoHisMoveIfItClosesMill2() throws InterruptedException {

        Options.Color[][] mill5 =
                {{P1, I , I , P1, I , I , P1},
                { I , I , I , I , I , I , I },
                { I , I , P1, P2, N , I , I },
                { N , I , N , I , N , I , P2},
                { I , I , P2, P2, N , I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , P1, I , I , P2}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result1 = strategy.computeMove();
        gameBoard.executeCompleteTurn(result1, mPlayer1);

        assertEquals(new Position(0,3), result1.getDest());

        gameBoard.executeCompleteTurn(new Move(new Position(4,4), new Position(3,4), null), mPlayer2);

        Move result2 = strategy.computeMove();
        gameBoard.executeCompleteTurn(result2, mPlayer1);

        assertEquals(new Position(0,0), result2.getDest());

    }

    @Test
    public void computeMoveShouldWinAsNoMovesLeft() throws InterruptedException {

        Options.Color[][] mill5 =
                {{P1, I , I , N , I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , P2, P1, P2, I , I },
                { N , I , P1, I , N , I , P1},
                { I , I , P2, P1, P2, I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill5(mill5);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result = strategy.computeMove();

        assertEquals(new Position(4, 3), result.getDest());
        assertEquals(new Position(6, 3), result.getSrc());

    }

    //Test if especially bots on easy open their mill, as they cant see the gameboard after 3 moves
    @Test
    public void computeMoveShouldOpenMill() throws InterruptedException {

        Options.Color[][] mill9 =
                {{P1, I , I , N , I , I , N },
                { I , N , I , P2, I , P1, I },
                { I , I , N , N , P2, I , I },
                { N , N , N , I , N , P1, N },
                { I , I , N , N , N , I , I },
                { I , N , I , P2, I , P1, I },
                { P2, I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill9(mill9);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result = strategy.computeMove();

        assertThat(result.getDest(), anyOf(is(new Position(6,3)), is(new Position(4,3))));

    }

    @Test
    public void computeMoveShouldNotOpenMill() throws InterruptedException {

        Options.Color[][] mill9 =
                {{P1, I , I , N , I , I , N },
                { I , N , I , P2, I , P1, I },
                { I , I , N , N , N , I , I },
                { N , N , N , I , P2, P1, N },
                { I , I , N , N , N , I , I },
                { I , N , I , P2, I , P1, I },
                { P2, I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill9(mill9);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategy = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        Move result = strategy.computeMove();

        assertEquals(result.getSrc(), new Position(0,0));

    }

    @Test
    public void computeMoveShouldPreventCornerMillOnGameStartOnHigherDifficulties() throws InterruptedException {

        //workaround to skip easier difficulties
        if(mPlayer1.getDifficulty().equals(Options.Difficulties.EASIEST)
                || mPlayer1.getDifficulty().equals(Options.Difficulties.EASY)){
            return;
        }

        Options.Color[][] mill9 =
                {{N , I , I , N , I , I , N },
                { I , N , I , P2, I , N , I },
                { I , I , N , N , N , I , I },
                { N , N , N , I , N , P2, N },
                { I , I , N , N , N , I , I },
                { I , N , I , N , I , N , I },
                { N , I , I , P1, I , I , N}};

        GameBoard gameBoard = new Mill9(mill9);
        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);
        Strategy strategyP2 = new Strategy(gameBoard, mPlayer2, updater, nThreads);

        mPlayer1.setSetCount(4);
        mPlayer2.setSetCount(3);

        for(int i = 0; i<3; i++){

            Move result1 = strategyP1.computeMove();
            gameBoard.executeCompleteTurn(result1, mPlayer1);

            Move result2 = strategyP2.computeMove();
            gameBoard.executeCompleteTurn(result2, mPlayer2);

        }

        assertEquals(4, gameBoard.getPositions(mPlayer1.getColor()).size());

    }

    @Test
    public void computeMoveDoesNotAlterPassedObjects() throws InterruptedException {

        GameBoard gameBoard = new Mill9();

        mPlayer1.setSetCount(9);
        mPlayer2.setSetCount(9);

        Player mPlayer1Before = new Player(mPlayer1);
        Player mPlayer2Before = new Player(mPlayer2);

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());
        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);
        Strategy strategyP2 = new Strategy(gameBoard, mPlayer2, updater, nThreads);

        for(int i = 0; i<30; i++){

            GameBoard gameBoardBefore1 = gameBoard.getCopy();

            //test if computeMove makes unallowed changed to gameBoard or players now
            Move result1 = strategyP1.computeMove();
            gameBoard.executeCompleteTurn(result1, mPlayer1);
            if(!gameBoard.getState(mPlayer1).equals(GameBoard.GameState.RUNNING)){
                break;
            }

            //computeMove should not alter anything but the last move of course

            //do the same but this time manually without computeMove to check if its the same
            gameBoardBefore1.executeCompleteTurn(result1, mPlayer1Before);
            assertEquals("round " + i, gameBoardBefore1.toString(), gameBoard.toString());

            //computeMove should not have altered anything but the setCount of P1 (was done in executeCompleteTurn)
            assertEquals("round " + i, mPlayer1Before, mPlayer1);
            assertEquals("round " + i, mPlayer2Before, mPlayer2);

            GameBoard gameBoardBefore2 = gameBoard.getCopy();
            Move result2 = strategyP2.computeMove();
            gameBoard.executeCompleteTurn(result2, mPlayer2);
            if(!gameBoard.getState(mPlayer2).equals(GameBoard.GameState.RUNNING)){
                break;
            }

            gameBoardBefore2.executeCompleteTurn(result2, mPlayer2Before);

            assertEquals("round " + i, gameBoardBefore2.toString(), gameBoard.toString());

            assertEquals("round " + i, mPlayer1Before, mPlayer1);
            assertEquals("round " + i, mPlayer2Before, mPlayer2);

        }

    }

    //test for same evaluation, as resulting move may be different for different nThreads
    @Test
    public void computeMoveShouldHaveSameEvaluationForAnyNumberOfThreads () throws InterruptedException {

        if(nThreads > 1){
            //do this test only once, as this test runs for all nThreads already
            return;
        }

        GameBoard gameBoard = new Mill9();

        mPlayer1.setSetCount(9);
        mPlayer2.setSetCount(9);

        //make 30 rounds and check if the results on all possible thread counts are the same
        for(int i = 0; i<30; i++){
            computeMoveShouldHaveSameEvaluationForAnyNumberOfThreads_Turn(i, gameBoard, mPlayer1);
            if(!gameBoard.getState(mPlayer1).equals(GameBoard.GameState.RUNNING)){
                break;
            }
            computeMoveShouldHaveSameEvaluationForAnyNumberOfThreads_Turn(i, gameBoard, mPlayer2);
            if(!gameBoard.getState(mPlayer2).equals(GameBoard.GameState.RUNNING)){
                break;
            }
        }

    }


    public void computeMoveShouldHaveSameEvaluationForAnyNumberOfThreads_Turn (int round, GameBoard gameBoard, Player player) throws InterruptedException{

        int maxThreads = 16;

        int prevResultEval = 0;
        int resultEval = 0;
        Move resultMove = null;

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());

        for(int j = 0; j < maxThreads; j++) {
            int nThreads = j+1;
            Strategy strategy = new Strategy(gameBoard, player, updater, nThreads);
            resultMove = strategy.computeMove();
            resultEval = strategy.getResultEvaluation();
            if(j > 0) {
                assertEquals("round " + round + " nTreads: " + nThreads +
                        "; resultEval was different from previous one\n previous result: " +
                        prevResultEval + "\n result: " + resultEval, prevResultEval, resultEval);
            }
            prevResultEval = resultEval;
        }

        gameBoard.executeCompleteTurn(resultMove, player);
    }

    //if this test fails may (but probably not) be because of a tiny chance that not all possible moves were chosen
    @Test
    public void computeMoveShouldReturn16DifferentMovesOverTime () throws InterruptedException {

        int nPos = 16;

        Options.Color[][] mill5 =
                {{N , I , I , N , I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , N , N , N , I , I },
                { N , I , N , I , N , I , N },
                { I , I , N , N , N , I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , N , I , I , N}};

        GameBoard gameBoard = new Mill5(mill5);

        mPlayer1.setSetCount(9);
        mPlayer2.setSetCount(9);

        LinkedList<Move> list = new LinkedList<Move>();

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());

        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        for(int i = 0; i < 1000; i++) {
            Move result = strategyP1.computeMove();
            if(!list.contains(result)) {
                list.add(result);
            }
            //check after 100 iterations if list contains enough (or too much) elements and break
            if(i % 100 == 0 && list.size() >= nPos){
                break;
            }
        }

        assertEquals(nPos, list.size());

    }

    @Test
    public void computeMoveShouldReturn5DifferentKillMovesOverTime () throws InterruptedException {

        Options.Color[][] mill9 =
                {{P2, I , I , N , I , I , N },
                { I , P1, I , N , I , P2, I },
                { I , I , N , N , N , I , I },
                { P1, N , N , I , N , N , P2},
                { I , I , P2, N , N , I , I },
                { I , P1, I , N , I , N , I },
                { P2, I , I , N , I , I , N }};

        GameBoard gameBoard = new Mill9(mill9);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        LinkedList<Move> list = new LinkedList<Move>();

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());

        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        for(int i = 0; i < 1000; i++) {
            Move result = strategyP1.computeMove();
            if(!list.contains(result)) {
                list.add(result);
            }
            //check after 50 iterations if list contains enough (or too much) elements and break
            if(i % 50 == 0 && list.size() >= 5){
                break;
            }
        }

        assertEquals(5, list.size());
    }

    @Test
    public void shuffleListShouldHaveKillsAtBeginning(){

        Options.Color[][] mill9 =
                {{N , I , I , N , I , I , P2},
                { I , N , I , N , I , N , I },
                { I , I , N , P1, P1, I , I },
                { P2, N , P1, I , P2, N , N },
                { I , I , N , P1, N , I , I },
                { I , N , I , N , I , P2, I },
                { N , I , I , P2, I , I , N }};

        GameBoard gameBoard = new Mill9(mill9);

        mPlayer1.setSetCount(0);
        mPlayer2.setSetCount(0);

        ProgressBar progBar = new ProgressBar(new MockContext());
        ProgressUpdater updater = new ProgressUpdater(progBar, new GameModeActivity());

        Strategy strategyP1 = new Strategy(gameBoard, mPlayer1, updater, nThreads);

        LinkedList<Move> result = strategyP1.shuffleListOfPossMoves();

        assertEquals(5 + 7, result.size());

        assertTrue(result.get(0).getKill() != null);
        assertTrue(result.get(1).getKill() != null);
        assertTrue(result.get(2).getKill() != null);
        assertTrue(result.get(3).getKill() != null);
        assertTrue(result.get(4).getKill() != null);

    }

}
