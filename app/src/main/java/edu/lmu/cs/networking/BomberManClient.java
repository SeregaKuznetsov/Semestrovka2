package edu.lmu.cs.networking;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;

import javax.swing.*;

public class BomberManClient {

    private JFrame frame = new JFrame("BomberMan");
    private JLabel messageLabel = new JLabel("");

    private final int size = 100;
    private final int sideSize = (int) Math.sqrt(size);
    private Square[] board = new Square[size];
    private Square[] opponentBoard = new Square[size];
    private int currentLocation;
    private int opponentCurrentLocation;

    //Images
    private ImageIcon icon;
    private ImageIcon opponentIcon;
    private ImageIcon empty = createImageIcon("emptyField.png", "EmptyField image");
    private ImageIcon barrier = createImageIcon("brick.png", "Brick image");
    private ImageIcon granite = createImageIcon("granite.png", "Granite image");
    private ImageIcon destroyed = createImageIcon("destroyedWall.png", "DestroyedWall image");

    private Socket socket;
    private int PORT = 8901;
    private BufferedReader in;
    private PrintWriter out;


    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */

    private BomberManClient(String serverAddress) throws Exception {

        try {
            socket = new Socket(serverAddress, PORT);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }  catch (ConnectException e) {
            JOptionPane.showConfirmDialog(frame,
                    "Sorry, server is not running",
                    "Error",
                    JOptionPane.CLOSED_OPTION);
        }

        messageLabel.setBackground(Color.black);
        frame.getContentPane().add(messageLabel, "South");

        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if ((key == KeyEvent.VK_LEFT)) {
                    out.println("MOVE LEFT");
                }
                if ((key == KeyEvent.VK_RIGHT)) {
                    out.println("MOVE RIGHT");
                }
                if ((key == KeyEvent.VK_UP)) {
                    out.println("MOVE UP");
                }
                if ((key == KeyEvent.VK_DOWN)) {
                    out.println("MOVE DOWN");
                }
                if ((key == KeyEvent.VK_ENTER)){
                    out.println("CHANGE");
                    messageLabel.setText("CHANGE TURN");
                }
            }
        });

        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(sideSize, sideSize, 4, 4));

        JPanel opponentBoardPanel = new JPanel();
        opponentBoardPanel.setLayout(new GridLayout(sideSize, sideSize, 4, 4));

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));

        JLabel label1 = new JLabel("Use <- -> to move\n");
        JLabel label2 = new JLabel("Left mouse button - Plant the bomb\n");
        JLabel label3 = new JLabel("Right mouse button - Check field\n");
        JLabel label4 = new JLabel("Enter - Miss turn\n");
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label3.setAlignmentX(Component.CENTER_ALIGNMENT);
        label4.setAlignmentX(Component.CENTER_ALIGNMENT);

        labelPanel.add(label1);
        labelPanel.add(label2);
        labelPanel.add(label3);
        labelPanel.add(label4);
        frame.getContentPane().add(labelPanel, "Center");

        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Square();
            board[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (j == currentLocation - 1)
                            out.println("BOMB LEFT");
                        else if (j == currentLocation + 1)
                            out.println("BOMB RIGHT");
                        else if (j == currentLocation - sideSize)
                            out.println("BOMB UP");
                        else if (j == currentLocation + sideSize)
                            out.println("BOMB DOWN");
                    }

                    /*if (e.getButton() == MouseEvent.BUTTON3) {
                        if (j == currentLocation - 1)
                            out.println("SHOW LEFT");
                        else if (j == currentLocation + 1)
                            out.println("SHOW RIGHT");
                        else if (j == currentLocation - sideSize)
                            out.println("SHOW UP");
                        else if (j == currentLocation + sideSize)
                            out.println("SHOW DOWN");
                    }*/
                }
            });
            boardPanel.add(board[i]);
        }
        frame.getContentPane().add(boardPanel, "West");

        for (int i = 0; i < opponentBoard.length; i++) {
            opponentBoard[i] = new Square();
            opponentBoardPanel.add(opponentBoard[i]);
        }
        frame.getContentPane().add(opponentBoardPanel, "East");
    }

    /**
     * The main thread of the client will listen for messages
     * from the server.  The first message will be a "WELCOME"
     * message in which we receive our mark.  Then we go into a
     * loop listening for "VALID_MOVE", "OPPONENT_MOVED", "VICTORY",
     * "DEFEAT", "TIE", "OPPONENT_QUIT or "MESSAGE" messages,
     * and handling each message appropriately.  The "VICTORY",
     * "DEFEAT" and "TIE" ask the user whether or not to play
     * another game.  If the answer is no, the loop is exited and
     * the server is sent a "QUIT" message.  If an OPPONENT_QUIT
     * message is recevied then the loop will exit and the server
     * will be sent a "QUIT" message also.
     */

    private void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                if (mark == 'X') {
                    icon = createImageIcon("Terrorist.png", "X image");
                    opponentIcon = createImageIcon("ContrTerrorist.png", "O image");
                    frame.setTitle("BomberMan - Player: Terrorist");
                }
                else {
                    icon = createImageIcon("ContrTerrorist.png", "O image");
                    opponentIcon = createImageIcon("Terrorist.png", "X image");
                    frame.setTitle("BomberMan - Player: Counter-Terrorist");
                }

            }
            while (true) {
                response = in.readLine();

                if (response.startsWith("START")) {
                    startLocation(board, icon);

                } else if (response.startsWith("OPPONENT_START")) {
                    startLocation(opponentBoard, opponentIcon);

                } else if (response.startsWith("OPEN")) {
                    openLocation(response, board, 5, true);

                } else if (response.startsWith("OPPONENT_OPEN")) {
                    openLocation(response, opponentBoard, 14, false);

                } else if (response.startsWith("VALID_MOVE")) {
                    moveLocation(response, board, 11, icon, true);

                } else if (response.startsWith("OPPONENT_MOVED")) {
                    moveLocation(response, opponentBoard, 15, opponentIcon, false);

                } else if (response.startsWith("DESTROYED")) {
                    destroyedLocation(response, board, 10, true);

                } else if (response.startsWith("OPPONENT_DESTROYED")) {
                    destroyedLocation(response, opponentBoard, 19, false);

                } else if (response.startsWith("NOT_DESTROYED")) {
                    notDestroyedLocation(response, board, 14, true);

                } else if (response.startsWith("OPPONENT_NOT_DESTROYED")) {
                    notDestroyedLocation(response, opponentBoard, 23, false);

                } else if (response.startsWith("THROW_INTO_THE_VOID")) {
                    throwIntoTheVoid(response, 20, true);

                } else if (response.startsWith("OPPONENT_THROW_INTO_THE_VOID")) {
                    throwIntoTheVoid(response, 29, false);

                } else if (response.startsWith("VICTORY")) {
                    messageLabel.setText("You win! :)");
                    break;

                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("You lose :(");
                    break;

                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("QUIT");
            messageLabel.setText("QUIT");
        } finally {
            socket.close();
        }
    }

    private int getLocationByDirection(String direction, boolean current) {
        int location;
        if (current)
            location = currentLocation;
        else
            location = opponentCurrentLocation;

        if (direction.startsWith("LEFT"))
            return location - 1;
        else if (direction.startsWith("RIGHT"))
            return location + 1;
        else if (direction.startsWith("UP"))
            return location - sideSize;
        else if (direction.startsWith("DOWN"))
            return location + sideSize;
        else
            return -1;
    }

    private void startLocation(Square[] board, ImageIcon icon) {
        int startLocation = (size - 1)/2;
        if (icon.equals(this.icon)) {
            currentLocation = startLocation;
        } else {
            opponentCurrentLocation = startLocation;
        }
        board[startLocation].setIcon(icon);
        board[startLocation].repaint();
    }

    private void openLocation(String response, Square[] board, int n, boolean current) {
        int code = Integer.parseInt(response.substring(n, n + 1));
        String turn = response.substring(n + 2);
        int playerLocation;

        if (current)
            playerLocation = currentLocation;
        else
            playerLocation = opponentCurrentLocation;

        if (playerLocation % sideSize == 0 && turn.equals("LEFT")) {
            //NOTHING

        } else if ((playerLocation + 1) % sideSize == 0 && turn.equals("RIGHT")) {
            //NOTHING

        } else if (playerLocation <= sideSize && turn.equals("UP")) {
            //NOTHING

        } else if (size - playerLocation <= sideSize && turn.equals("DOWN")) {
            //NOTHING

        } else {

            int location = getLocationByDirection(turn, current); //определяем координату по направлению

            if (code == 0) {
                board[location].setIcon(empty);
            }
            if (code == 1 && board[location].getIcon() != granite) {
                board[location].setIcon(barrier);
            }
            board[location].repaint();

            if (current)
                messageLabel.setText("OPEN " + turn);
            else
                messageLabel.setText("OPPONENT OPEN " + turn);
        }
    }

    private void moveLocation(String response, Square[] board, int n, ImageIcon icon, boolean current) {

        String turn = response.substring(n);
        int location = getLocationByDirection(turn, current); //определяем координату по направлению
        board[location].setIcon(icon);
        board[location].repaint();

        if (icon.equals(this.icon)) {
            board[currentLocation].setIcon(empty);
            board[currentLocation].repaint();
            currentLocation = location;
        } else {
            board[opponentCurrentLocation].setIcon(empty);
            board[opponentCurrentLocation].repaint();
            opponentCurrentLocation = location;
        }
    }

    private void destroyedLocation(String response, Square[] board, int n, boolean current) {
        String turn = response.substring(n);
        int location = getLocationByDirection(turn, current); //определяем координату по направлению
        board[location].setIcon(destroyed);
        board[location].repaint();

        if (current)
            messageLabel.setText("WALL DESTROYED " + turn);
        else
            messageLabel.setText("OPPONENT DESTROYED WALL " + turn);
    }

    private void notDestroyedLocation(String response, Square[] board, int n, boolean current) {
        String turn = response.substring(n);
        int location = getLocationByDirection(turn, current); //определяем координату по направлению
        board[location].setIcon(granite);
        board[location].repaint();

        if (current)
            messageLabel.setText("WALL NOT DESTROYED " + turn);
        else
            messageLabel.setText("OPPONENT NOT DESTROYED WALL " + turn);

    }

    private void throwIntoTheVoid(String response, int n, boolean current) {
        String turn = response.substring(n);

        if (current)
            messageLabel.setText("THROW INTO THE VOID " + turn);
        else
            messageLabel.setText("OPPONENT THROW INTO THE VOID " + turn);
    }

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
                "Want to play again?",
                "Let's go!",
                JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    /**
     * Graphical square in the client window.  Each square is
     * a white panel containing.  A client calls setIcon() to fill
     * it with an Icon, presumably an X or O.
     */

    private static class Square extends JPanel {
        JLabel label = new JLabel((Icon) null);

        Square() {
            setBackground(Color.gray);
            add(label);
        }

        void setIcon(Icon icon) {
            label.setIcon(icon);
        }

        Icon getIcon() {
            return label.getIcon();
        }
    }

    /**
     * Runs the client as an application.
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[1];
            BomberManClient client = new BomberManClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(700, 300);
            client.frame.setLocation(300, 300);
            client.frame.setVisible(true);
            client.frame.setResizable(true);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    private ImageIcon createImageIcon(String path,
                                      String description) {

        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        System.out.println(imgURL);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);

        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
}