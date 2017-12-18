package backend;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client implements ThreadedSocketHandler {
	private ThreadedSocket socket;

	public Client(String host, int port) throws IOException {
		Socket s = new Socket(host, port);
		socket = new ThreadedSocket(this, s);
	}

	public Client() throws IOException {
		this(Server.HOST, Server.PORT);
	}

	@Override
	public void onMessageReceived(ThreadedSocketEvent event) {
		// If initiator is null then it is a message from the server
		if (event.initiator == null) {
			System.out.println("[Server] " + event.message);
			return;
		}
		boolean isQuery = doQuery(event.message);
		if (!isQuery) {
			System.out.println(event.message);
		}
	}

	/**
	 * Checks if the message received is a query message from the server
	 */
	protected boolean doQuery(String message) {
		if (message.startsWith("%%")) {
			message = message.replaceFirst("%%", "");
			if (message.startsWith("setname ")) {
				message = message.replaceFirst("setname ", "");
				socket.setNameClient(message.trim());
				return true;
			}
			if (message.startsWith("kick")) {
				message = message.replaceFirst("kick", "");

				// If no kick message is specified, use the default
				if (message.isEmpty()) {
					onMessageReceived(new ThreadedSocketEvent(null, "You have been kicked"));
				}

				// Use specified kick message
				else {
					onMessageReceived(new ThreadedSocketEvent(null, message));
				}
				onSocketClose(socket);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onMessageSent(ThreadedSocketEvent event) {
		socket.sendMessage(event.message);
	}

	/**
	 * Creates a ThreadedSocketEvent using the enclosed ThreadedSocket
	 * 
	 * @param message
	 *            The message to attach to the ThreadedSocketEvent
	 */
	public ThreadedSocketEvent socketEvent(String message) {
		return new ThreadedSocketEvent(socket, message);
	}

	@Override
	public void onSocketClose(ThreadedSocket socket) {
		if (socket == null) {
			this.socket.sendMessage("%%DISCONNECT");
			this.socket.finalize();
		}
	}
}
