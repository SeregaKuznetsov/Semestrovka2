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
    private int columnLength = (int) Math.sqrt(size);
    private Object[] board;

    /**
     * The current player.
     */

    Player currentPlayer;

    public void setBoard(Object[] board) {

        this.board = board;

        currentPlayer.previousLocation = (int) (Math.random() * board.length);
        currentPlayer.opponent.previousLocation = (int) (Math.random() * board.length);

        currentPlayer.setStartPosition(currentPlayer.previousLocation);
        System.out.println(currentPlayer + " - " + currentPlayer.previousLocation);
        currentPlayer = currentPlayer.opponent;

        currentPlayer.setStartPosition(currentPlayer.previousLocation);
        System.out.println(currentPlayer + " - " + currentPlayer.previousLocation);
        currentPlayer.setOtherStartPosition(currentPlayer.opponent.previousLocation);

        currentPlayer = currentPlayer.opponent;
        currentPlayer.setOtherStartPosition(currentPlayer.opponent.previousLocation);
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

    private synchronized int direction(String subCommand) {
        if (subCommand.startsWith("LEFT"))
            return currentPlayer.previousLocation - 1;
        else if (subCommand.startsWith("RIGHT"))
            return currentPlayer.previousLocation + 1;
        else if (subCommand.startsWith("UP"))
            return currentPlayer.previousLocation - columnLength;
        else if (subCommand.startsWith("DOWN"))
            return currentPlayer.previousLocation + columnLength;
        else
            return -1;
    }

    private synchronized boolean validShow(Player player, String command, PrintWriter output) {
        if (player == currentPlayer) { //если сейчас ходит правильный игрок
            String turn = command.substring(5);
            int location = direction(turn); //определяем координату по направлению
            int code = -1; //код клетки (0 - пусто, 1 - препятствие)

            try {
                if (currentPlayer.previousLocation % columnLength == 0 && turn.equals("LEFT")) { //проверка для одномерного массива
                    throw new ArrayIndexOutOfBoundsException();
                } else if ((currentPlayer.previousLocation + 1) % columnLength == 0 && turn.equals("RIGHT")) { //проверка для одномерного массива
                    throw new ArrayIndexOutOfBoundsException();
                }

                if (board[location].equals(currentPlayer.opponent)) {
                    code = 0;
                } else if (board[location].equals(brick) || board[location].equals(granite)) {
                    code = 1;
                }

            } catch (NullPointerException e) { //если клетка пустая
                code = 0;
            } catch (ArrayIndexOutOfBoundsException e) { //если вышли за пределы поля
                code = 1;
            }
            output.println("OPEN " + code + " " + turn); //команда отдает состояние нужной клетки пользователю
            currentPlayer.opponent.otherPlayerOpened(code, turn); //уведомляем оппонента о результате хода
            currentPlayer.wasShow = true;
            return true;
        }
        return false;
    }

    private synchronized boolean validMove(Player player, String command, PrintWriter output) {
        if (player == currentPlayer) { //если сейчас ходит правильный игрок
            String turn = command.substring(5); //достаем направление
            int location = direction(turn); //определяем координату по направлению

            board[currentPlayer.previousLocation] = null; //удаляем предыдушую точку игрока

            try {
                if (currentPlayer.previousLocation % columnLength == 0 && turn.equals("LEFT")) { //проверка для одномерного массива
                    throw new ArrayIndexOutOfBoundsException();
                } else if ((currentPlayer.previousLocation + 1) % columnLength == 0 && turn.equals("RIGHT")) { //проверка для одномерного массива
                    throw new ArrayIndexOutOfBoundsException();
                }

                if (board[location].equals(brick) || board[location].equals(granite)) { //если препятствие
                    int code = 1; //если препятствие то это код 1
                    output.println("OPEN " + code + " " + turn); //команда открыть клетку по заданному направлению с определенным кодом
                    currentPlayer.opponent.otherPlayerOpened(code, turn); //уведомляем оппонента о результате своего хода
                    currentPlayer.wasShow = true;

                } else if (board[location].equals(currentPlayer.opponent)) { //если на клетке пусто, то идем
                    board[location] = currentPlayer; //игрок встает на клетку
                    //int from = currentPlayer.previousLocation; //запоминаем предыдущую клетку
                    output.println("VALID_MOVE " + turn); //команда разрешает ход
                    currentPlayer.opponent.otherPlayerMoved(turn); //уведомляем оппонента о результате своего хода
                    currentPlayer.previousLocation = location; //запоминаем позицию игрока
                    currentPlayer.setAction(); //ход переходит оппоненту
                }

            } catch (ArrayIndexOutOfBoundsException e) { //если вышли за клетку
                e.printStackTrace();
                int code = 1; //все что за полем - препятствие
                output.println("OPEN " + code + " " + turn); //команда для клиента нарисовать стену (т.к. код 1)
                currentPlayer.opponent.otherPlayerOpened(code, turn); //уведомляем оппонента о результате своего хода
                currentPlayer.wasShow = true;

            } catch (NullPointerException e) { //если на клетке пусто, то идем
                e.printStackTrace();
                board[location] = currentPlayer; //игрок встает на клетку
                output.println("VALID_MOVE " + turn); //команда разрешает ход
                currentPlayer.opponent.otherPlayerMoved(turn); //уведомляем оппонента о результате своего хода
                currentPlayer.previousLocation = location; //запоминаем позицию игрока
                currentPlayer.setAction(); //ход переходит оппоненту
            }
            return true;
        }
        return false;
    }

    private synchronized boolean validThrow(Player player, String command, PrintWriter output) {
        if (player == currentPlayer) {
            String turn = command.substring(5); //достаем направление
            int location = direction(turn); //определяем координату по направлению
            String message = null; //сообщение для оппонента о результате своего хода

            try {
                if (currentPlayer.previousLocation % columnLength == 0 && turn.equals("LEFT")) { //проверка для одномерного массива
                    throw new ArrayIndexOutOfBoundsException();
                } else if ((currentPlayer.previousLocation + 1) % columnLength == 0 && turn.equals("RIGHT")) { //проверка для одномерного массива
                    throw new ArrayIndexOutOfBoundsException();
                }

                if (hasWinner(location)) { //проверка на попадание
                    board[location] = null;
                    output.println("VICTORY");

                    message = "DEFEAT";

                } else if (board[location].equals(brick)) { //если кирпич, то ломаем его
                    board[location] = null;
                    output.println("DESTROYED " + turn);

                    message = "OPPONENT_DESTROYED " + turn;

                } else if (board[location].equals(granite)) { //если гранит
                    output.println("NOT_DESTROYED " + turn);

                    message = "OPPONENT_NOT_DESTROYED " + turn;
                }
            } catch (NullPointerException e) { //если попали в пустоту
                e.printStackTrace();
                if (player == currentPlayer) {
                    output.println("THROW_INTO_THE_VOID " + turn);

                    message = "OPPONENT_THROW_INTO_THE_VOID " + turn;
                }
            } catch (ArrayIndexOutOfBoundsException e) { //если стреляем дальше границы поля
                e.printStackTrace();
                output.println("NOT_DESTROYED " + turn);

                message = "OPPONENT_NOT_DESTROYED " + turn;
            }
            currentPlayer.opponent.otherPlayerThrowed(message); //уведомляем оппонента о результате своего хода
            currentPlayer.setAction(); // ход переходит оппоненту
            return true;
        }
        return false;
    }

    /**
     * The class for the helper threads in this multithreaded server
     * application.  A Player is identified by a character mark
     * which is either 'X' or 'O'.  For communication with the
     * client the player has a socket with its input and output
     * streams.  Since only text is being communicated we use a
     * reader and a writer.
     */

    private class Brick {
    }

    private class Granite {
    }

    Brick brick = new Brick();
    Granite granite = new Granite();

    class Player extends Thread {
        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        int currentLocation;
        int previousLocation;

        boolean firstMove = true;

        //       boolean firstAction = true;
        //       boolean secondAction = false;

        boolean wasShow = false;
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
            if (firstMove) {
                board[location] = currentPlayer.opponent;
                output.println("OPPONENT_START");
                currentPlayer.firstMove = false;
            }
        }

        void otherPlayerOpened(int code, String turn) {
            output.println("OPPONENT_OPEN " + code + " " + turn);
        }

        void otherPlayerMoved(String turn) {
            output.println("OPPONENT_MOVED " + turn);
        }

        void otherPlayerThrowed(String message) {
            output.println(message);
        }

        void setStartPosition(int location) {
            if (firstMove) {
                board[location] = currentPlayer; //ставим игрока на стартовую позицию
                output.println("START");
            }
        }

        void setAction() {
            /*
            if (firstAction && !secondAction) {
                firstAction = false;
                secondAction = true;
            } else if (!firstAction && secondAction) {
                firstAction = true;
                secondAction = false;
            */
            wasShow = false;
            wasMove = false;
            wasThrow = false;

            changePlayer();
        }
        //}

        /**
         * The run method of this thread.
         */

        public void run() {
            try {
                // The thread is only started after everyone connects.
                output.println("MESSAGE All players connected");

                // Tell the first player that it is her turn.
                if (mark == 'X') {
                    output.println("MESSAGE Your move");
                }

                // Repeatedly get commands from the client and process them.
                while (true) {
                    String command = input.readLine();
                    if (command != null) {
                        if (command.startsWith("SHOW") && !wasShow) {
                            if (!validShow(this, command, output)) {
                                output.println("MESSAGE It's not your turn");
                            } else {
                                //wasShow = true;
                                //setAction();
                            }

                        } else if (command.startsWith("MOVE") && !wasMove) {
                            if (!validMove(this, command, output)) {
                                output.println("MESSAGE It's not your turn");
                            } else {
                                //wasMove = true;
                                //setAction();
                            }

                        } else if (command.startsWith("BOMB") && !wasThrow) {
                            if (!validThrow(this, command, output)) {
                                output.println("MESSAGE It's not your turn");
                            } else {
                                //wasThrow = true;
                                //setAction();
                            }

                        } else if (command.startsWith("CHANGE")) {
                            setAction();

                        } else if (command.startsWith("QUIT")) {
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