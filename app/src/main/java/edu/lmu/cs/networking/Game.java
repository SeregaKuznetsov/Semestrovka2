package edu.lmu.cs.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A two-player game.
 */

public class Game {

    /**
     * A board has nine squares.  Each square is either unowned or
     * it is owned by a player.  So we use a simple array of player
     * references.  If null, the corresponding square is unowned,
     * otherwise the array cell stores a reference to the player that
     * owns it.
     */

    private int size = 9;
    private int sideSize = (int) Math.sqrt(size);
    private Object[] board;

    /**
     * The current player.
     */

    Player currentPlayer;

    public void setBoard(Object[] board) {

        this.board = board;

        currentPlayer.playerLocation = (int) (Math.random() * board.length);
        currentPlayer.opponent.playerLocation = (int) (Math.random() * board.length);

        currentPlayer.setStartPosition(currentPlayer.playerLocation);
        System.out.println(currentPlayer + " - " + currentPlayer.playerLocation);
        currentPlayer.setOtherStartPosition(currentPlayer.opponent.playerLocation);
        changePlayer();

        currentPlayer.setStartPosition(currentPlayer.playerLocation);
        System.out.println(currentPlayer + " - " + currentPlayer.playerLocation);
        currentPlayer.setOtherStartPosition(currentPlayer.opponent.playerLocation);
        changePlayer();

    }

    private void changePlayer() {
        currentPlayer = currentPlayer.opponent;
    }

    /**
     * Returns whether the current state of the board is such that one
     * of the players is a winner.
     */

    private boolean hasWinner(int location) throws NullPointerException, ArrayIndexOutOfBoundsException {
        return board[location].equals(currentPlayer.opponent);
    }

    private synchronized int getLocationByDirection(String direction) {
        if (direction.startsWith("LEFT"))
            return currentPlayer.playerLocation - 1;
        else if (direction.startsWith("RIGHT"))
            return currentPlayer.playerLocation + 1;
        else if (direction.startsWith("UP"))
            return currentPlayer.playerLocation -  sideSize;
        else if (direction.startsWith("DOWN"))
            return currentPlayer.playerLocation +  sideSize;
        else
            return -1;
    }

    private int isBrick(int location) {

        if (board[location].equals(currentPlayer.opponent)) {
            return 0;
        }
        else if (board[location].equals(Blocks.BRICK) || board[location].equals(Blocks.GRANITE)) {
            return 1;
        }
        return -1;
    }

    /**
     * Called by the player threads when a player tries to make a
     * move.  This method checks to see if the move is legal: that
     * is, the player requesting the move must be the current player
     * and the square in which she is trying to move must not already
     * be occupied.  If the move is legal the game state is updated
     * (the square is set and the next player becomes current) and
     * the other player is notified of the move so it can update its
     * client.
     */

    private synchronized boolean legalMove(Player player, String command, PrintWriter output) {
        if (player == currentPlayer) {
            String direction = command.substring(5); // LEFT, RIGHT, UP, DOWN
            int location = getLocationByDirection(direction);

            board[currentPlayer.playerLocation] = null;

            try {

                checkArrayIndexOutOfBoundsException(direction);

                if (board[location].equals(Blocks.BRICK) || board[location].equals(Blocks.GRANITE)) { //если препятствие
                        throw new ArrayIndexOutOfBoundsException();
                }  else if (board[location].equals(currentPlayer.opponent)) { //если на клетке находится другой игрок
                        throw new NullPointerException();
            }

            } catch (ArrayIndexOutOfBoundsException e) { //если встретили препятсвие
                e.printStackTrace();
                int code = 1;
                output.println("OPEN " + code + " " +  direction); //команда для клиента нарисовать стену
                output.println("MESSAGE You can't go through the block " + direction);
                currentPlayer.opponent.otherPlayerOpened(code,  direction); // Говорим оппоненту о своем ходе

            } catch (NullPointerException e) { //если на клетке пусто, то идем
                e.printStackTrace();
                board[location] = currentPlayer; //игрок встает на клетку
                output.println("VALID_MOVE " + direction); //команда разрешает ход
                currentPlayer.opponent.otherPlayerMoved(direction); // Говорим оппоненту о своем ходе
                currentPlayer.playerLocation = location;
                currentPlayer.endTurn(); // ЗАканчиваем ход
            }
            return true;
        }
        return false;
    }

    private synchronized boolean legalThrow(Player player, String command, PrintWriter output) {
        if (player == currentPlayer) {
            String direction = command.substring(5);
            int location = getLocationByDirection(direction);
            String message = null; //сообщение для оппонента

            try {

                checkArrayIndexOutOfBoundsException(direction);

                if (hasWinner(location)) { //проверка на попадание в оппонента
                    board[location] = null;
                    output.println("VICTORY");
                    message = "DEFEAT";

                } else if (board[location].equals(Blocks.BRICK)) { //если стена
                    board[location] = null;
                    output.println("DESTROYED " + direction);
                    message = "OPPONENT_DESTROYED " + direction;

                } else if (board[location].equals(Blocks.GRANITE)) { //если гранит
                    output.println("NOT_DESTROYED " + direction);
                    message = "OPPONENT_NOT_DESTROYED " + direction;
                }
            } catch (NullPointerException e) { //если попали в пустоту
                e.printStackTrace();
                output.println("THROW_INTO_THE_VOID " + direction);
                message = "OPPONENT_THROW_INTO_THE_VOID " + direction;

            } catch (ArrayIndexOutOfBoundsException e) { //если бросаем дальше границы поля
                e.printStackTrace();
                output.println("NOT_DESTROYED " + direction);
                message = "OPPONENT_NOT_DESTROYED " + direction;
            }
            currentPlayer.opponent.otherPlayerThrowed(message);
            currentPlayer.endTurn();
            return true;
        }
        return false;
    }

    private void checkArrayIndexOutOfBoundsException(String direction) {
        if (currentPlayer.playerLocation % sideSize == 0 && direction.equals("LEFT")) {
            throw new ArrayIndexOutOfBoundsException();
        } else if ((currentPlayer.playerLocation + 1) % sideSize == 0 && direction.equals("RIGHT")) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * The class for the helper threads in this multithreaded server
     * application.  A Player is identified by a character mark
     * which is either 'X' or 'O'.  For communication with the
     * client the player has a socket with its input and output
     * streams.  Since only text is being communicated we use a
     * reader and a writer.
     */

    public enum Blocks {
            BRICK, GRANITE
    }

    class Player extends Thread {
        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;
        int playerLocation;

        // Available actions
        boolean wasMove = false;
        boolean wasThrow = false;

        /**
         * Constructs a handler thread for a given socket and mark
         * initializes the stream fields, displays the first two
         * welcoming messages.
         */

        Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Waiting for opponent to connect");
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
        }

        /**
         * Accepts notification of who the opponent is.
         */

        void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * Handles the otherPlayerMoved message.
         */

        void setOtherStartPosition(int location) {
                board[location] = currentPlayer.opponent;
                output.println("OPPONENT_START");
        }

        void otherPlayerOpened(int code, String turn) {
            output.println("OPPONENT_OPEN " + code + " " + turn);
        }

        void otherPlayerMoved(String turn) {
            output.println("OPPONENT_MOVED " + turn);
            output.println("MESSAGE OPPONENT_MOVED " + turn);
        }

        void otherPlayerThrowed(String message) {
            output.println(message);
        }

        void setStartPosition(int location) {
                board[location] = currentPlayer;
                output.println("START");
        }

        void endTurn() {
            wasMove = false;
            wasThrow = false;
            changePlayer();
            currentPlayer.output.println("YOUR_MOVE");
        }

        /**
         * The run method of this thread.
         */

        public void run() {
            try {
                // The thread is only started after everyone connects.
                output.println("MESSAGE All players connected");

                // Tell the first player that it is his turn.
                if (mark == 'X') {
                    output.println("MESSAGE Your move");
                }

                // Repeatedly get commands from the client and process them.
                while (true) {
                    String command = input.readLine();
                    if (command != null) {
                         if (command.startsWith("MOVE") && !wasMove) {
                            if (!legalMove(this, command, output))
                                output.println("MESSAGE It's not your turn");

                        } else if (command.startsWith("BOMB") && !wasThrow) {
                            if (!legalThrow(this, command, output))
                                output.println("MESSAGE It's not your turn");
                        }
                        else if (command.startsWith("CHANGE"))
                            endTurn();

                        else if (command.startsWith("QUIT")) {
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public String toString() {
        if (currentPlayer != null) {
            return currentPlayer.mark + " has won";
        } else {
            return "game in progress";
        }
    }
}