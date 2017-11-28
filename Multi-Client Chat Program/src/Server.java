import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Server extends ThreadedSocketHandler {

	public static final int PORT = 6969;
	public static final String HOST = "localhost";
	private boolean isRunning = true;
	public String helpMessage = "Commands: \n " + "/setname [New Name]";

	private ArrayList<ThreadedSocket> clients;

	public Server() {
		clients = new ArrayList<>();
		start();
	}

	public void run() {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(PORT);
		} catch (IOException e1) {
			System.err.println("Failed to create server.");
			e1.printStackTrace();
			System.exit(-1);
		}
		System.out.println("Waiting for clients on port " + PORT + "...");
		while (isRunning) {
			try {
				Socket newConnection = socket.accept();
				clients.add(new ThreadedSocket(this, newConnection));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Connected: " + clients.get(clients.size() - 1));
		}
		try {
			socket.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Called whenever a client sends a message, relays it to all clients unless it
	 * is a commmand in which case it executes it
	 */
	@Override
	public void onMessageReceived(ThreadedSocketEvent event) {
		boolean isCommand = doCommands(event.initiator, event.message);
		System.out.println("[" + event.initiator + "] " + event.message);
		if (!isCommand) {
			for (ThreadedSocket toSendTo : clients) {
				if (toSendTo != event.initiator) {
					toSendTo.sendMessage("[" + event.initiator + "] " + event.message);
				}
			}
		}
	}

	@Override
	public void onMessageSent(ThreadedSocketEvent event) {
	}

	private boolean doCommands(ThreadedSocket initiator, String message) {
		if (message.startsWith("/")) { // All commands start with '/'
			message = message.replaceFirst("/", "");

			// Start setname
			if (message.startsWith("setname ")) {
				message = message.replaceFirst("setname ", "");
				if (clientByName(message) == null) {
					onMessageSent(
							new ThreadedSocketEvent(initiator, initiator + " has changed their name to " + message));
					initiator.setName(message);
				} else {
					initiator.sendMessage("Sorry, " + message + "is already in use");
				}
				return true;
			}
			// End setname

			// Start help
			if (message.startsWith("help")) {
				initiator.sendMessage("[Server] " + helpMessage);
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

	public static void main(String[] args) {
		Server server = new Server();
	}

}
