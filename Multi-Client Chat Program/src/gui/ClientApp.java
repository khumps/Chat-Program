package gui;

import java.io.IOException;
import java.util.Optional;

import backend.Client;
import backend.Server;
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

public class ClientApp extends Application {

	private Stage primaryStage;
	private HBox gui;
	private TextArea chatArea;
	private TextArea typingArea;
	private Client chatClient;
	private String host;
	private int port;

	private void initUI() {
		chatArea = new TextArea();
		chatArea.setEditable(false);
		typingArea = new TextArea();
		typingArea.setMaxHeight(100);
		typingArea.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (!typingArea.getText().isEmpty()) {
					System.out.println("sent message: " + typingArea.getText());
					chatClient.onMessageSent(chatClient.socketEvent(typingArea.getText()));
					typingArea.setText("");
				}
				event.consume();
			}
		});
	}

	/**
	 * Init after the UI has been created. Creates the Chat object and overrides
	 * necessary methods to link it with the GUI
	 */
	private void postInit() {
		try {
			chatArea.appendText("Attempting to connect to " + host + ":" + port + "\n");
			chatClient = new Client(host, port) {
				public void onMessageReceived(ThreadedSocketEvent event) {
					if (event.initiator == null) {
						chatArea.appendText("[Server] " + event.message + "\n");
						return;
					}
					boolean isQuery = doQuery(event.message);
					if (!isQuery) {
						chatArea.appendText(event.message + "\n");
					}
				}

				public void onMessageSent(ThreadedSocketEvent event) {
					super.onMessageSent(event);
					chatArea.appendText("[" + event.initiator + "] " + event.message + "\n");
				}
			};
			chatArea.appendText("Connected.\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getConnectionInfo() {
		TextInputDialog dialog = new TextInputDialog("localhost");

		// Traditional way to get the response value.
		dialog.setContentText("Please enter the server IP:");
		Optional<String> ip = dialog.showAndWait();
		dialog = new TextInputDialog(Server.PORT + "");
		dialog.setContentText("Please enter the server Port:");
		Optional<String> port = dialog.showAndWait();

		// The Java 8 way to get the response value (with lambda expression).
		ip.ifPresent(resp -> this.host = resp);
		port.ifPresent(resp -> this.port = Integer.parseInt(resp));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		getConnectionInfo();
		this.primaryStage = primaryStage;
		primaryStage.setTitle("Client");
		primaryStage.setMinHeight(500);
		BorderPane bp = new BorderPane();
		gui = new HBox();
		initUI();
		postInit();
		bp.setTop(gui);
		bp.setCenter(chatArea);
		bp.setBottom(typingArea);
		primaryStage.setResizable(true);
		primaryStage.setScene(new Scene(bp));
		primaryStage.show();
	}

	public void stop() {
		chatClient.onSocketClose(null);
	}

	public static void main(String[] args) {
		Application.launch(args);
	}
}
