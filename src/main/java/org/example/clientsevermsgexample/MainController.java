package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

import static java.lang.Thread.sleep;

public class MainController implements Initializable {

    @FXML
    private ComboBox dropdownPort;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dropdownPort.getItems().addAll("7",     // ping
                "13",     // daytime
                "21",     // ftp
                "23",     // telnet
                "71",     // finger
                "80",     // http
                "119",     // nntp (news)
                "161"      // snmp);
        );
    }

    @FXML
    private Button clearBtn;

    @FXML
    private TextArea resultArea;

    @FXML
    private Label server_lbl;

    @FXML
    private Button testBtn;

    @FXML
    private Label test_lbl;

    @FXML
    private TextField urlName;

    @FXML
    private Button user1_client;

    @FXML
    private Button user2_server;

    Socket socket1;
    Label lb122, lb12;
    TextField msgText;

    @FXML
    void checkConnection(ActionEvent event) {

        String host = urlName.getText();
        int port = Integer.parseInt(dropdownPort.getValue().toString());

        try {
            Socket sock = new Socket(host, port);
            resultArea.appendText(host + " listening on port " + port + "\n");
            sock.close();
        } catch (UnknownHostException e) {
            resultArea.setText(String.valueOf(e) + "\n");
            return;
        } catch (Exception e) {
            resultArea.appendText(host + " not listening on port "
                    + port + "\n");
        }
    }

    @FXML
    void clearBtn(ActionEvent event) {
        resultArea.setText("");
        urlName.setText("");

    }

    @FXML
    void startServer(ActionEvent event) {
        Stage stage = new Stage();
        Group root = new Group();
        Label lb11 = new Label("Server");
        lb11.setLayoutX(100);
        lb11.setLayoutY(100);

        lb12 = new Label("info");
        lb12.setLayoutX(100);
        lb12.setLayoutY(200);
        root.getChildren().addAll(lb11, lb12);
        Scene scene = new Scene(root, 600, 350);
        stage.setScene(scene);
        lb12.setText("Server is running and waiting for a client...");

        stage.setTitle("Server");
        stage.show();


        new Thread(this::runServer).start();

    }

    String message;

    private void runServer() {
        try {

            ServerSocket serverSocket = new ServerSocket(6666);
            updateServer("Server is running and waiting for a client...");
            while (true) { // Infinite loop
                try {
                    Socket clientSocket = serverSocket.accept();
                    updateServer("Client connected!");

                    new Thread(() -> {
                        try {
                            sleep(3000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                    message = dis.readUTF();
                    updateServer("Message from client: " + message);

                    // Sending a response back to the client
                    dos.writeUTF("Received: " + message);

                    dis.close();
                    dos.close();

                } catch (IOException e) {
                    updateServer("Error: " + e.getMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (message.equalsIgnoreCase("exit")) break;

            }
        } catch (IOException e) {
            updateServer("Error: " + e.getMessage());
        }
    }

    private void updateServer(String message) {
        // Run on the UI thread
        javafx.application.Platform.runLater(() -> lb12.setText(message + "\n"));
    }

    @FXML
    void startClient(ActionEvent event) {
        Stage stage = new Stage();
        Group root = new Group();
        Button connectButton = new Button("Connect to server");
        connectButton.setLayoutX(100);
        connectButton.setLayoutY(300);
        connectButton.setOnAction(this::connectToServer);
        // new Thread(this::connectToServer).start();

        Label lb11 = new Label("Client");
        lb11.setLayoutX(100);
        lb11.setLayoutY(100);
        msgText = new TextField("msg");
        msgText.setLayoutX(100);
        msgText.setLayoutY(150);

        lb122 = new Label("info");
        lb122.setLayoutX(100);
        lb122.setLayoutY(200);
        root.getChildren().addAll(lb11, lb122, connectButton, msgText);


        Scene scene = new Scene(root, 600, 350);
        stage.setScene(scene);
        stage.setTitle("Client");
        stage.show();
    }

    private void connectToServer(ActionEvent event) {
        try {
            socket1 = new Socket("localhost", 6666);

            DataOutputStream dos = new DataOutputStream(socket1.getOutputStream());
            DataInputStream dis = new DataInputStream(socket1.getInputStream());

            dos.writeUTF(msgText.getText());
            String response = dis.readUTF();
            updateTextClient("Server response: " + response + "\n");

            dis.close();
            dos.close();
            socket1.close();
        } catch (Exception e) {
            updateTextClient("Error: " + e.getMessage() + "\n");
        }
    }

    private void updateTextClient(String message) {
        // Run on the UI thread
        javafx.application.Platform.runLater(() -> lb122.setText(message + "\n"));
    }

    @FXML private TextArea serverChatArea, clientChatArea;
    @FXML private TextField serverMsgField, clientMsgField;
    @FXML private Button serverSendBtn, clientSendBtn;

    private DataOutputStream serverOut, clientOut;
    private DataInputStream serverIn, clientIn;

    private Socket clientSocket, socketToServer;

    // Server chat functionality
    @FXML
    void startChatServer(ActionEvent event) {
        Stage stage = new Stage();
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        serverChatArea = new TextArea();
        serverChatArea.setEditable(false);
        serverMsgField = new TextField();
        serverMsgField.setPromptText("Enter message");
        serverSendBtn = new Button("Send");
        serverSendBtn.setOnAction(this::sendMessageFromServer);

        root.getChildren().addAll(new Label("Server"), serverChatArea, serverMsgField, serverSendBtn);

        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("Server");
        stage.show();

        new Thread(this::runChatServer).start();

    }

    private void runChatServer() {
        try (ServerSocket serverSocket = new ServerSocket(6666)) {
            updateServerUI("Server is running...");
            clientSocket = serverSocket.accept();
            updateServerUI("Client connected!");

            serverIn = new DataInputStream(clientSocket.getInputStream());
            serverOut = new DataOutputStream(clientSocket.getOutputStream());

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = serverIn.readUTF()) != null) {
                        updateServerUI("Client: " + msg);
                    }
                } catch (IOException e) {
                    updateServerUI("Client disconnected.");
                }
            }).start();
        } catch (IOException e) {
            updateServerUI("Error: " + e.getMessage());
        }
    }

    private void sendMessageFromServer(ActionEvent event) {
        String msg = serverMsgField.getText();
        if (msg.isEmpty()) return;
        try {
            serverOut.writeUTF(msg);
            updateServerUI("Server: " + msg);
            serverMsgField.clear();
        } catch (IOException e) {
            updateServerUI("Send failed: " + e.getMessage());
        }
    }

    private void updateServerUI(String message) {
        Platform.runLater(() -> serverChatArea.appendText(message + "\n"));
    }

    // Client chat functionality
    @FXML
    void startChatClient(ActionEvent event) {
        Stage stage = new Stage();
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        clientChatArea = new TextArea();
        clientChatArea.setEditable(false);
        clientMsgField = new TextField();
        clientMsgField.setPromptText("Enter message");
        clientSendBtn = new Button("Send");
        clientSendBtn.setOnAction(this::sendMessageFromClient);

        root.getChildren().addAll(new Label("Client"), clientChatArea, clientMsgField, clientSendBtn);

        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("Client");
        stage.show();

        new Thread(this::connectToChatServer).start();

    }

    private void connectToChatServer() {
        try {
            socketToServer = new Socket("localhost", 6666);
            clientOut = new DataOutputStream(socketToServer.getOutputStream());
            clientIn = new DataInputStream(socketToServer.getInputStream());

            updateClientUI("Connected to server!");

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = clientIn.readUTF()) != null) {
                        updateClientUI("Server: " + msg);
                    }
                } catch (IOException e) {
                    updateClientUI("Server disconnected.");
                }
            }).start();
        } catch (IOException e) {
            updateClientUI("Error: " + e.getMessage());
        }
    }

    private void sendMessageFromClient(ActionEvent event) {
        String msg = clientMsgField.getText();
        if (msg.isEmpty()) return;
        try {
            clientOut.writeUTF(msg);
            updateClientUI("Client: " + msg);
            clientMsgField.clear();
        } catch (IOException e) {
            updateClientUI("Send failed: " + e.getMessage());
        }
    }

    private void updateClientUI(String message) {
        Platform.runLater(() -> clientChatArea.appendText(message + "\n"));
    }

}
