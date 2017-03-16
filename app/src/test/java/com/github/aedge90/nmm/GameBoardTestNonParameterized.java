package com.github.aedge90.nmm;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


public class GameBoardTestNonParameterized {

    private GameBoard mGameBoard;
    private Player mPlayerWhite;
    private Player mPlayerBlack;

    private final Options.Color B = Options.Color.BLACK;
    private final Options.Color W = Options.Color.WHITE;
    private final Options.Color N = Options.Color.NOTHING;
    private final Options.Color I = Options.Color.INVALID;

    @Before
    public void setUp(){
        mGameBoard = new Mill9();
        mPlayerBlack = new Player(Options.Color.BLACK);
        mPlayerWhite = new Player(Options.Color.WHITE);
        mPlayerBlack.setOtherPlayer(mPlayerWhite);
        mPlayerWhite.setOtherPlayer(mPlayerBlack);
        mPlayerBlack.setSetCount(5);
        mPlayerWhite.setSetCount(5);
    }


    @Test
    public void executeCompleteTurn_WithSetMoveOnNonEmptyPosShouldThrowException(){

        GameBoard mGameBoard = new Mill9();

        Position p = new Position(6,6);
        try {
            mPlayerBlack.setSetCount(5);
            mPlayerWhite.setSetCount(5);
            mGameBoard.executeCompleteTurn(new Move(p, null, null), mPlayerBlack);
            mGameBoard.executeCompleteTurn(new Move(p, null, null), mPlayerWhite);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessage = "is trying to set to an occupied field by";
            if(!e.getMessage().contains(expectedMessage)){
                fail("expected message to contain: " + expectedMessage + "\n" + "but was: " + e.getMessage());
            }
        }


    }

    @Test
    public void executeCompleteTurn_WithMoveMoveOnNonEmptyPosShouldThrowException(){

        Position src1 = new Position(6,0);
        Position src2 = new Position(6,6);
        Position dest = new Position(6,3);

        try {
            mPlayerBlack.setSetCount(1);
            mPlayerWhite.setSetCount(1);
            mGameBoard.executeCompleteTurn(new Move(src1, null, null), mPlayerBlack);
            mGameBoard.executeCompleteTurn(new Move(src2, null, null), mPlayerWhite);
            mGameBoard.executeCompleteTurn(new Move(dest, src1, null), mPlayerBlack);
            mGameBoard.executeCompleteTurn(new Move(dest, src2, null), mPlayerWhite);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessage = "is trying to move to an occupied field by";
            if(!e.getMessage().contains(expectedMessage)){
                fail("expected message to contain: " + expectedMessage + "\n" + "but was: " + e.getMessage());
            }
        }

    }

    @Test
    public void executeCompleteTurn_KillOwnPieceShouldThrowException(){

        Position p = new Position(6,6);
        try {
            mPlayerBlack.setSetCount(5);
            mGameBoard.executeCompleteTurn(new Move(p, null, p), mPlayerBlack);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessageStart = "Trying to kill own piece of color";
            if(!e.getMessage().startsWith(expectedMessageStart)){
                fail("expected message to start with: " + expectedMessageStart + "\n" + "but was: " + e.getMessage());
            }
        }

    }


    @Test
    public void executeCompleteTurn_KillNotExistingPieceShouldThrowException(){

        Position set = new Position(0,0);
        Position kill = new Position(6,3);

        try {
            GameBoard emptyGameBoard = mGameBoard.getCopy();
            mPlayerBlack.setSetCount(5);
            emptyGameBoard.executeCompleteTurn(new Move(set, null, kill), mPlayerBlack);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessageEnd = "is trying to kill an empty field";
            if(!e.getMessage().endsWith(expectedMessageEnd)){
                fail("expected message to end with: " + expectedMessageEnd + "\n" + "but was: " + e.getMessage());
            }
        }

    }

    @Test
    public void getMill_ShouldBeNullForMill7(){

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, W, I, B, I},
                { I, I, I, I, I, I, I},
                { N, W, I, W, I, W, N},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        Position[] mill1 = gameBoard.getMill(new Position(1,3), W);
        assertNull(mill1);
        Position[] mill2 = gameBoard.getMill(new Position(3,3), W);
        assertNull(mill2);
        Position[] mill3 = gameBoard.getMill(new Position(5,3), W);
        assertNull(mill3);

        Position[] mill4 = gameBoard.getMill(new Position(3,1), W);
        assertNull(mill4);
        Position[] mill5 = gameBoard.getMill(new Position(3,5), W);
        assertNull(mill5);

    }

    @Test
    public void inMill_ShouldBeTrueForMill7 () {

        //Tests if inMill is true even if there are FOUR neighbouring pieces in one direction

        Options.Color[][] mill7 =

                {{N, I, I, W, I, I, B},
                { I, W, I, N, I, W, I},
                { I, I, I, I, I, I, I},
                { B, W, I, W, I, N, W},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, N, I},
                { B, I, I, N, I, I, B}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        Move m1 = new Move(new Position(5,3), new Position(5,1), new Position(0,6));

        gameBoard.executeSetOrMovePhase(m1, mPlayerWhite);

        assertTrue(gameBoard.inMill(m1.getDest(), mPlayerWhite.getColor()));

        Move m2 = new Move(new Position(3,1), new Position(1,1), new Position(6,6));

        gameBoard.executeSetOrMovePhase(m2, mPlayerWhite);

        assertTrue(gameBoard.inMill(m2.getDest(), mPlayerWhite.getColor()));

    }

    @Test
    public void inMill_ShouldBeFalseForMill7 () {

        //Tests if inMill is true even if there are FOUR neighbouring pieces in one direction

        Options.Color[][] mill7 =

                {{N, I, I, B, I, I, N},
                { I, N, I, N, I, N, I},
                { I, I, I, I, I, I, I},
                { B, W, I, W, I, N, N},
                { I, I, I, I, I, I, I},
                { I, N, I, B, I, N, I},
                { W, I, I, W, I, I, B}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(3);
        mPlayerWhite.setSetCount(3);

        Move m1 = new Move(new Position(5,3), null, null);

        assertFalse(gameBoard.inMill(m1.getDest(), mPlayerWhite.getColor()));

    }

    @Test
    public void getPositionsShouldbeOfCorrectSize () {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, W, I, B, I},
                { I, I, I, I, I, I, I},
                { N, W, I, N, I, W, N},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(1);
        mPlayerWhite.setSetCount(1);

        gameBoard.executeCompleteTurn(new Move(new Position(6,3), null, null), mPlayerWhite);
        assertEquals(5, gameBoard.getPositions(mPlayerWhite.getColor()).size());

        gameBoard.executeCompleteTurn(new Move(new Position(0,0), null, null), mPlayerBlack);
        assertEquals(5, gameBoard.getPositions(mPlayerBlack.getColor()).size());

        gameBoard.executeCompleteTurn(new Move(new Position(3,3), new Position(3,1), new Position(0,6)), mPlayerWhite);
        assertEquals(5, gameBoard.getPositions(mPlayerWhite.getColor()).size());
        assertEquals(4, gameBoard.getPositions(mPlayerBlack.getColor()).size());

        gameBoard.reverseCompleteTurn(new Move(new Position(3,3), new Position(3,1), new Position(0,6)), mPlayerWhite);
        assertEquals(5, gameBoard.getPositions(mPlayerWhite.getColor()).size());
        assertEquals(5, gameBoard.getPositions(mPlayerBlack.getColor()).size());

    }

    @Test
    public void preventedMillShouldReturnTrue1() {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, W, I, B, I},
                { I, I, I, I, I, I, I},
                { N, W, I, W, I, N, B},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        assertTrue(gameBoard.preventedMill(new Position(5,3), mPlayerWhite));

    }

    @Test
    public void preventedMillShouldReturnFalse2() {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, W, I, N, I},
                { I, I, I, I, I, I, I},
                { N, W, I, W, I, N, B},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        assertFalse(gameBoard.preventedMill(new Position(5,3), mPlayerWhite));

    }

    @Test
    public void preventedMillShouldReturnTrue2() {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, W, I, B, I},
                { I, I, I, I, I, I, I},
                { N, W, I, W, I, N, N},
                { I, I, I, I, I, I, I},
                { I, N, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        assertTrue(gameBoard.preventedMill(new Position(5,3), mPlayerWhite));

    }

    @Test
    public void preventedMillShouldReturnFalse3() {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, W, I, B, I},
                { I, I, I, I, I, I, I},
                { N, W, I, W, I, N, N},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        assertFalse(gameBoard.preventedMill(new Position(5,3), mPlayerWhite));

    }

    @Test
    public void inPotentialMillShouldBeTrue1 () {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, N, I, B, I},
                { I, I, I, I, I, I, I},
                { N, N, I, N, I, N, N},
                { I, I, I, I, I, I, I},
                { I, N, I, N, I, N, I},
                { N, I, I, W, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        assertTrue(gameBoard.inPotentialMill(new Position(6,6), mPlayerWhite.getColor()));

    }

    @Test
    public void inPotentialMillShouldBeTrue2 () {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, N, I, B, I},
                { I, I, I, I, I, I, I},
                { N, N, I, N, I, N, N},
                { I, I, I, I, I, I, I},
                { I, N, I, N, I, N, I},
                { N, I, I, N, I, I, W}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        assertTrue(gameBoard.inPotentialMill(new Position(3,6), mPlayerWhite.getColor()));

    }

    @Test
    public void inPotentialMillShouldBeTrue3 () {

        Options.Color[][] mill5 =
                {{N , I , I , N , I , I , N },
                { I , I , I , I , I , I , I },
                { I , I , N , N , N , I , I },
                { N , I , N , I , W , I , N },
                { I , I , N , N , N , I , I },
                { I , I , I , I , I , I , I },
                { N , I , I , B , I , I , N}};

        GameBoard gameBoard = new Mill5(mill5);

        mPlayerBlack.setSetCount(4);
        mPlayerWhite.setSetCount(4);

        assertTrue(gameBoard.inPotentialMill(new Position(4,2), mPlayerWhite.getColor()));

    }

    @Test
    public void inPotentialMillShouldBeFalse () {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, N, I, B, I},
                { I, I, I, I, I, I, I},
                { N, N, I, N, I, N, N},
                { I, I, I, I, I, I, I},
                { I, N, I, N, I, N, I},
                { B, I, I, W, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        assertFalse(gameBoard.inPotentialMill(new Position(6,6), mPlayerWhite.getColor()));

    }

    @Test
    public void inPotentialMillShouldBeFalseForMill7 () {

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, W},
                { I, N, I, N, I, N, I},
                { I, I, I, I, I, I, I},
                { N, N, I, N, I, N, N},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, B, I},
                { N, I, I, B, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(5);
        mPlayerWhite.setSetCount(5);

        assertFalse(gameBoard.inPotentialMill(new Position(3,3), mPlayerWhite.getColor()));

    }

    @Test
    public void getStateShouldBeDraw () {

        Options.Color[][] mill7 =

                {{B, I, I, N, I, I, N},
                { I, N, I, W, I, B, I},
                { I, I, I, I, I, I, I},
                { W, W, I, N, I, B, N},
                { I, I, I, I, I, I, I},
                { I, W, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        mPlayerBlack.setSetCount(0);
        mPlayerWhite.setSetCount(0);

        gameBoard.executeCompleteTurn(new Move(new Position(3,3), new Position(3,1), null), mPlayerWhite);
        gameBoard.executeCompleteTurn(new Move(new Position(3,6), new Position(0,6), null), mPlayerBlack);
        gameBoard.executeCompleteTurn(new Move(new Position(3,1), new Position(3,3), null), mPlayerWhite);
        gameBoard.executeCompleteTurn(new Move(new Position(0,6), new Position(3,6), null), mPlayerBlack);
        gameBoard.executeCompleteTurn(new Move(new Position(1,1), new Position(3,1), new Position(0,6)), mPlayerWhite);
        //now remisCount should be reset

        int i = 0;
        while(i <= GameBoard.REMISMAX){
            gameBoard.executeCompleteTurn(new Move(new Position(3,0), new Position(0,0), null), mPlayerBlack);
            i++;
            getStateShouldBeDraw_AssertDRAWOrRUNNING(i, gameBoard, mPlayerBlack);
            gameBoard.executeCompleteTurn(new Move(new Position(0,6), new Position(0,3), null), mPlayerWhite);
            i++;
            getStateShouldBeDraw_AssertDRAWOrRUNNING(i, gameBoard, mPlayerWhite);
            gameBoard.executeCompleteTurn(new Move(new Position(0,0), new Position(3,0), null), mPlayerBlack);
            i++;
            getStateShouldBeDraw_AssertDRAWOrRUNNING(i, gameBoard, mPlayerBlack);
            gameBoard.executeCompleteTurn(new Move(new Position(0,3), new Position(0,6), null), mPlayerWhite);
            i++;
            getStateShouldBeDraw_AssertDRAWOrRUNNING(i, gameBoard, mPlayerWhite);
        }

    }

    public void getStateShouldBeDraw_AssertDRAWOrRUNNING (int turnSinceLastKill, GameBoard gameBoard, Player executingPlayer) {
        if(turnSinceLastKill >= GameBoard.REMISMAX){
            assertEquals(GameBoard.GameState.REMIS, gameBoard.getState(executingPlayer));
        }else{
            assertEquals(GameBoard.GameState.RUNNING, gameBoard.getState(executingPlayer));
        }
    }
}
