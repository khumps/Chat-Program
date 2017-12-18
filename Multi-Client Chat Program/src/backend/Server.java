package backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * 
 * @author Kevin Humphreys
 *
 */
public class Server extends Thread implements ThreadedSocketHandler {

	public static final int PORT = 9999;
	public static final String HOST = "localhost";
	private boolean isRunning = true;
	public String helpMessage = "Commands: \n" + "/setname [New Name] (change your display name)\n"
			+ "/m [recipient] (send a private message)\n" + "/online (returns a count of how many users are online)\n"
			+ "/online -list (same as /online but also lists all online users)";

	protected ArrayList<ThreadedSocket> clients;
	private ServerSocket serverSocket;

	public Server() {
		clients = new ArrayList<>();
		start();
	}

	public void run() {
		serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e1) {
			System.err.println("Failed to create server.");
			e1.printStackTrace();
			System.exit(-1);
		}
		System.out.println("Waiting for clients on port " + PORT + "...");
		while (isRunning) {
			try {
				Socket newConnection = serverSocket.accept();
				ThreadedSocket newClient = new ThreadedSocket(this, newConnection);
				clients.add(newClient);
				onClientJoin(newClient);
				sendJoinMessage(clients.get(clients.size() - 1));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Called whenever a client sends a message, relays it to all clients unless it
	 * is a commmand in which case it executes it
	 */
	@Override
	public void onMessageReceived(ThreadedSocketEvent event) {
		boolean isCommand = doCommands(event.initiator, event.message);
		boolean isQuery = doQuery(event.initiator, event.message);
		System.out.println("[" + event.initiator + "] " + event.message);
		if (!isCommand && !isQuery) {
			for (ThreadedSocket toSendTo : clients) {
				if (toSendTo != event.initiator) {
					toSendTo.sendMessage("[" + event.initiator + "] " + event.message);
				}
			}
		}
	}

	protected boolean doQuery(ThreadedSocket initiator, String message) {
		if (message.startsWith("%%")) {
			message = message.replaceFirst("%%", "");
			if (message.equals("DISCONNECT")) {
				System.out.println("Disconnecting " + initiator);
				initiator.finalize();
			}
			return true;
		}
		return false;
	}

	@Override
	public void onMessageSent(ThreadedSocketEvent event) {
	}

	public void onMessageSent(String message) {
		onMessageSent(new ThreadedSocketEvent(null, message));
	}

	/**
	 * Checks for and handles all commands run by the clients
	 * 
	 * @param initiator
	 *            User who sent the message/command
	 * @param message
	 *            The message/command
	 * @return Whether it was a command or not
	 */
	protected final boolean doCommands(ThreadedSocket initiator, String message) {
		if (message.startsWith("/")) { // All commands start with '/'
			message = message.replaceFirst("/", "");

			// Start setname
			if (message.startsWith("setname ")) {
				message = message.replaceFirst("setname ", "");
				if (clientByName(message) == null) {
					String oldName = initiator.getName();
					initiator.setName(message);
					messageAllClients(oldName + " has changed their name to " + message);
					initiator.sendMessage("%%setname " + message);
				} else {
					initiator.sendMessage("Sorry, " + message + " is already in use");
				}
				return true;
			}
			// End setname

			// Start help
			if (message.startsWith("help")) {
				initiator.sendMessage("[Server]\n" + helpMessage);
				return true;
			}
			// End help

			// Start private message (m)
			if (message.startsWith("m ")) {
				message = message.replaceFirst("m ", "");
				int afterName = message.indexOf(" ", 0);
				String name = message.substring(0, afterName);
				ThreadedSocket recipient = clientByName(name);
				if (recipient != null) {
					message = message.substring(afterName).trim();
					recipient.sendMessage("PM from [" + initiator + "] " + message);
				} else {
					initiator.sendMessage("Invalid recipient.");
				}
				return true;
			}
			// End private message (m)

			// Start list online (online)
			if (message.startsWith("online")) {
				initiator.sendMessage("Users online: [" + clients.size() + "]");
				if (message.equals("online -list"))
					for (ThreadedSocket client : clients) {
						initiator.sendMessage(" - " + client);
					}
				return true;
			}
			// Invalid command
			initiator.sendMessage("You entered an invalid command.");
			return true;
		}

		// Not a command
		return false;
	}

	private ThreadedSocket clientByName(String name) {
		for (ThreadedSocket client : clients) {
			if (client.getName().equals(name)) {
				return client;
			}
		}
		return null;
	}

	protected void doServerCommands(String expression) {
		if (expression.startsWith("kick ")) {
			expression = expression.replaceFirst("kick ", "");
			String clientToKick = expression.trim();
			ThreadedSocket toKick = clientByName(clientToKick);
			if (toKick != null) {
				toKick.sendMessage("%%kick");
			}
		}
	}

	/**
	 * Sends a message to all connected clients
	 */
	private void messageAllClients(String message) {
		for (ThreadedSocket client : clients) {
			client.sendMessage(message);
		}
	}

	/**
	 * Notifies all connected users that someone new has joined
	 * 
	 * @param joiningClient
	 *            The user that joined
	 */
	private void sendJoinMessage(ThreadedSocket joiningClient) {
		for (ThreadedSocket client : clients) {
			if (client != joiningClient) {
				client.sendMessage(joiningClient + " has connected");
			}
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
	}

	@Override
	public void onSocketClose(ThreadedSocket socket) {
		clients.remove(socket);
		messageAllClients(socket + " has disconnected");

	}

	public void onClientJoin(ThreadedSocket client) {
		System.out.println("Connected: " + clients.get(clients.size() - 1));
	}

	public void finalize() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			stop();
			System.out.println("here");
			System.out.println(clients);
			for (ThreadedSocket client : clients) {
				client.sendMessage("%%kick");
				client.finalize();
			}
			clients.clear();
		}
	}

}
