package com.github.aedge90.nmm;

import java.util.LinkedList;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

public class StrategyRunnable implements Runnable{

    //runnable will only work with local copies of the global gameboard and player objects
    private final GameBoard globalGameBoard;
    private GameBoard localGameBoard;
    private final Player globalMaxPlayer;
    private Player localMaxPlayer;

    private final Strategy strategy;
    private final ProgressUpdater up;
    //not Int.Max as the evaluation function would create overflows
    static final double MAX = (int) Math.pow(10,25);
    static final double MIN = - (int) Math.pow(10,25);
    private LinkedList<Move> movesToEvaluate;
    private Move prevMove;

    private final int threadNr;

    StrategyRunnable(final GameBoard gameBoard, final Player maxPlayer, final ProgressUpdater up, Strategy strategy, final int threadNr) {
        this.globalGameBoard = gameBoard;
        this.globalMaxPlayer = maxPlayer;
        this.strategy = strategy;
        this.up = up;
        this.movesToEvaluate = new LinkedList<Move>();
        this.threadNr = threadNr;
    }

    public void updateState(){
        //copy these so every thread has its own one to avoid concurrency problems
        this.localGameBoard = globalGameBoard.getCopy();
        //copy BOTH!!! the maxPlayer and the other player
        this.localMaxPlayer = new Player(globalMaxPlayer);
        Player other = new Player(globalMaxPlayer.getOtherPlayer());
        other.setOtherPlayer(localMaxPlayer);
        this.localMaxPlayer.setOtherPlayer(other);
    }

    @Override
    public void run() {
        try {
            updateState();
            computeMove();
        }catch ( InterruptedException e ) {
            Log.d("computeMove Thread " + threadNr, "Interrupted!");
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    // maximizing player has got to return higher values for better situations
    // minimizing player has got to return lower values the better his situation
    @VisibleForTesting
    double evaluation(Player player, LinkedList<Move> moves, int depth) {

        double ret = 0;

        if (moves.size() == 0) {
            if(movesToEvaluate.size() > 1) {
                localGameBoard.reverseCompleteTurn(movesToEvaluate.getLast(), player.getOtherPlayer());
                //check if the loosing player, prevented a mill in his last move (which is the size-2th move)
                if (localGameBoard.preventedMill(movesToEvaluate.get(movesToEvaluate.size() - 2).getDest(), player)) {
                    //evaluate this better, as it looks stupid if he does not try to prevent one mill even if the other player
                    //can close another mill despite that
                    ret = 1;
                }
                localGameBoard.executeCompleteTurn(movesToEvaluate.getLast(), player.getOtherPlayer());
            }
            //worst case: player can not make any moves --> game is lost
            //or player has less than 3 pieces and has no pieces left to set --> game is lost
            if (player.equals(localMaxPlayer)) {
                //multiply with an number (which is >= 1) depending on depth
                //necessary as the evaluation has to be higher if the player can win after fewer moves
                //you may think depth is always 0 here, but it can be higher
                ret = MIN * (depth + 1) + ret;
            }else{
                ret = MAX * (depth + 1) - ret;
            }
            return ret;
        }

        //evaluate how often the players can kill, and prefer kills that are in the near future
        int i = 0;
        double weight = 1;
        // it is important that never a evaluation of a subsequent move is better than the one of the current move
        // a then this path will be chosen, but it contains a move that may not be good
        for(Move move : movesToEvaluate){
            if(i % 2 == 0) {    //even numbers are moves of the maximizing player
                ret += move.getEvaluation() / weight;
            }else{
                ret -= move.getEvaluation() / weight;
            }
            weight = weight*10;
            i++;
        }

        //evaluate undoing a move, as its probably of no use. If it is, the other evaluation should overwrite this
        //this should break endless undoing and redoing of moves if all have the same evaluation so far
        if(prevMove != null){
            // closing and opening a mill should not be downgraded. Ignore setting phase
            if (prevMove.getKill() != null || movesToEvaluate.get(0).getKill() != null || prevMove.getSrc() == null){
                //do nothing
            }else if (prevMove.getSrc().equals(movesToEvaluate.get(0).getDest())
                    && prevMove.getDest().equals(movesToEvaluate.get(0).getSrc())) {
                ret -= 1;
            }
        }

        return ret;

    }

    private void evaluateMove(Move move, Player player) {
        double eval = 0;
        if (move.getKill() != null) {
            // next weight will be half the weight
            // this has to be done so players wont do the same move over and over again
            // as they would not choose a path in which they kill but the other player kills in a
            // distant future (which is seen in higher difficulties, when he can make jump moves)
            // thus lowers the evaluation drastically and the game is stalled
            // also this prefers kills in the near future, so they are done now and not later
            // as could be the case if all were weighted equally
            eval += 9000000000.;
            move.setEvaluation(eval);
        }
        else if(localGameBoard.preventedMill(move.getDest(), player)){
            // preventedMill is needed to downgrade opening a mill if the enemy can prevent it in his next move
            // still it has to be lower than every kill move, as a sure kill anytime in the future is still
            // better than preventing a mill now
            eval += 5;
        }
        if(player.getSetCount() >= 1){      //only makes sense if there are still more pieces to set
            int n = localGameBoard.isInNPotentialMills(move.getDest(), player.getColor());
            if(n > 0) {
                // evaluate having a potential future mill better, as otherwise the bot will just randomly place pieces
                // this causes the bot to be weaker especially on bigger gameboards as he does not really try to build a mill.
                eval += n*2;
            }
        }
        if(move.getSrc() == null) {     // do this only for moves during the setting phase
            // evaluate having more space to move better, as it is an important strategy in merels
            eval += localGameBoard.nEmptyNeighbors(move.getDest());
        }

        move.setEvaluation(eval);
    }

    private double max(int depth, double alpha, double beta, Player player) throws InterruptedException {
        if(Thread.interrupted()){
            throw new InterruptedException("Computation of Bot " + player + " was interrupted!");
        }
        LinkedList<Move> moves = localGameBoard.possibleMoves(player);
        //end reached or no more moves available, maybe because he is trapped or because he lost
        if (depth == 0 || moves.size() == 0){
            return evaluation(player, moves, depth);
        }
        double maxWert = alpha;
        for (Move z : moves) {
            localGameBoard.executeCompleteTurn(z, player);
            movesToEvaluate.addLast(z);
            evaluateMove(z, player);
            double wert = min(depth-1, maxWert, beta, player.getOtherPlayer());
            movesToEvaluate.removeLast();
            localGameBoard.reverseCompleteTurn(z, player);
            if (wert > maxWert) {
                maxWert = wert;
                if (maxWert >= beta) {
                    break;
                }
            }
        }
        return maxWert;
    }

    //same as max, but slightly modified to distribute work among threads
    private void maxKickoff(int depth, Player player) throws InterruptedException{

        while(true) {

            Move z;
            synchronized (strategy) {
                if(strategy.possibleMovesKickoff.size() > 0) {
                    z = strategy.possibleMovesKickoff.removeFirst();
                }else{
                    break;
                }
            }

            localGameBoard.executeCompleteTurn(z, player);
            movesToEvaluate.addLast(z);
            evaluateMove(z, player);
            double wert = min(depth - 1, strategy.maxWertKickoff, Double.MAX_VALUE, player.getOtherPlayer());
            movesToEvaluate.removeLast();
            localGameBoard.reverseCompleteTurn(z, player);

            synchronized (strategy) {
                if (wert > strategy.maxWertKickoff) {
                    strategy.maxWertKickoff = wert;
                    strategy.resultMove = z;
                    strategy.resultEvaluation = wert;
                }
            }

            up.increment();
        }
    }

    private double min(int depth, double alpha, double beta, Player player) throws InterruptedException {
        if(Thread.interrupted()){
            throw new InterruptedException("Computation of Bot " + player + " was interrupted!");
        }
        LinkedList<Move> moves = localGameBoard.possibleMoves(player);
        if (depth == 0 || moves.size() == 0){
            return evaluation(player, moves, depth);
        }
        double minWert = beta;
        for (Move z : moves) {
            localGameBoard.executeCompleteTurn(z, player);
            movesToEvaluate.addLast(z);
            evaluateMove(z, player);
            double wert = max(depth-1, alpha, minWert, player.getOtherPlayer());
            movesToEvaluate.removeLast();
            localGameBoard.reverseCompleteTurn(z, player);
            if (wert < minWert) {
                minWert = wert;
                if (minWert <= alpha){ 
                    break;
                }
            }
        }
        return minWert;

    }

    private void computeMove() throws InterruptedException {
                
        int startDepth = localMaxPlayer.getDifficulty().ordinal() + 1;

        startDepth = lowerDepthOnGameStart(startDepth);

        startDepth = lowerDepthOnGameEnd(startDepth);

        maxKickoff(startDepth, localMaxPlayer);
    }

    private int lowerDepthOnGameStart (int startDepth) {
        if(localMaxPlayer.getSetCount() == 0){
            return startDepth;
        }
        int nPositions = localGameBoard.getPositions(localMaxPlayer.getColor()).size();
        if(nPositions > 3){
            return startDepth;
        }else if(nPositions == 0){
            return Math.min(startDepth, 4);
        }else if (nPositions == 1){
            //4 is minimun, as otherwise high difficulty player will look dumb if they dont prevent mills
            return Math.min(startDepth, 5);
        }else if (nPositions == 2){
            return Math.min(startDepth, 5);
        }else if (nPositions == 3){
            return Math.min(startDepth, 6);
        }else if (nPositions == 4){
            return Math.min(startDepth, 6);
        }
        return startDepth;
    }

    private int lowerDepthOnGameEnd (int startDepth) {
        if(localMaxPlayer.getSetCount() > 0){
            return startDepth;
        }
        int nPlayerPos = localGameBoard.getPositions(localMaxPlayer.getColor()).size() +
                localGameBoard.getPositions(localMaxPlayer.getOtherPlayer().getColor()).size();
        if(nPlayerPos == 7 || nPlayerPos == 8){
            return Math.min(startDepth, 5);
        }
        if(nPlayerPos == 6){
            return Math.min(startDepth, 4);
        }
        return startDepth;
    }


    public void setPreviousMove(Move prevMove) {
       this.prevMove = prevMove;
    }

}
