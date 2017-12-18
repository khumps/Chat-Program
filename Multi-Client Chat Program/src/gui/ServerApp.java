package gui;

import java.io.IOException;
import java.util.Optional;

import backend.Server;
import backend.ThreadedSocket;
import backend.ThreadedSocketEvent;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class ServerApp extends Application {

	private Stage primaryStage;
	private HBox gui;
	private TextArea chatArea;
	private Server chatServer;
	private String host;
	private int port;

	private void initUI() {
		chatArea = new TextArea();
		chatArea.setEditable(false);
	}

	private void postInit() throws IOException {
		chatArea.appendText("Creating server...\n");
		chatServer = new Server() {
			public void onMessageReceived(ThreadedSocketEvent event) {
				super.onMessageReceived(event);
				chatArea.appendText("[" + event.initiator + "] " + event.message + "\n");
			}

			protected boolean doQuery(ThreadedSocket initiator, String message) {
				if (message.startsWith("%%")) {
					message = message.replaceFirst("%%", "");
					if (message.equals("DISCONNECT")) {
						chatArea.appendText("Disconnecting " + initiator + "\n");
						initiator.finalize();
					}
					return true;
				}
				return false;
			}

			public void onClientJoin(ThreadedSocket client) {
				super.onClientJoin(client);
				chatArea.appendText(client + " has joined.\n");
			}

			public void onMessageSent(ThreadedSocketEvent event) {
				super.onMessageSent(event);
				chatArea.appendText("[" + event.initiator + "] " + event.message + "\n");
			}
		};
		chatArea.appendText("Server listening on port " + port + "\n");
	}

	/**
	 * Inital prompt asking the server created to set the port that the server will
	 * listen on
	 */
	private void getServerPort() {
		TextInputDialog dialog = new TextInputDialog("localhost");

		// Traditional way to get the response value.
		dialog = new TextInputDialog(Server.PORT + "");
		dialog.setContentText("Please enter the server Port:");
		Optional<String> port = dialog.showAndWait();

		// The Java 8 way to get the response value (with lambda expression).
		port.ifPresent(resp -> this.port = Integer.parseInt(resp));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		getServerPort();
		this.primaryStage = primaryStage;
		primaryStage.setTitle("Server");
		primaryStage.setMinHeight(500);
		BorderPane bp = new BorderPane();
		gui = new HBox();
		initUI();
		postInit();
		bp.setTop(gui);
		bp.setCenter(chatArea);
		primaryStage.setResizable(true);
		primaryStage.setScene(new Scene(bp));
		primaryStage.show();
	}

	public void stop() {
		chatServer.finalize();
	}

	public static void main(String[] args) {
		Application.launch(args);
	}
}
