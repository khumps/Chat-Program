package backend;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;

/**
 * A container for a Socket Able to concurrently send and receive from the
 * enclosed Socket. Allows buffering multiple messages to be sent. Sending and
 * receiving is event based.
 * 
 * @author Kevin Humphreys
 *
 */
public class ThreadedSocket {
	private String name;
	private final ThreadedSocketHandler socketHandler;
	private final Socket socket; // The actual Socket that handles communication
	private final DataInputStream inputStream; // Receives data from the socket
	private final DataOutputStream outputStream; // Sends data over the socket
	private Thread inputThread;
	private Thread outputThread;
	private boolean messageToSend; // True if there are messages to be sent over the Socket
	private ArrayDeque<String> messagesToSend; // Queue of all messages to be sent over the Socket
	private byte failedSends; // Counts how many times a message has failed to send since the last successful
								// message sent

	public ThreadedSocket(ThreadedSocketHandler socketHandler, Socket socket) throws IOException {
		name = "Guest";
		this.socket = socket;
		this.socketHandler = socketHandler;
		inputStream = new DataInputStream(socket.getInputStream());
		outputStream = new DataOutputStream(socket.getOutputStream());
		messagesToSend = new ArrayDeque<>();
		failedSends = 0;
		initalizeThreads();
	}

	private void initalizeThreads() {
		initalizeInputThread();
		initializeOutputThread();
	}

	/**
	 * All logic relating to messages being sent through the Socket. Sends all
	 * messages that are in the Queue
	 */
	private void initializeOutputThread() {
		messageToSend = false;
		outputThread = new Thread(() -> {
			while (!socket.isClosed()) {
				while (messageToSend) {
					try {
						String message = messagesToSend.peekFirst();
						// Pull the first message from the Queue, attempt to send it, if it is
						// successful remove it from the Queue
						outputStream.writeUTF(message);
						messagesToSend.removeFirst();
						failedSends = 0;
					} catch (IOException e) {
						System.err.println("Message failed to send");
						failedSends++;
						if (failedSends == 3) { // If a message fails to send 3 times in a row, discard it from the
												// messagesToSend Queue and reset the counter
							messagesToSend.removeFirst();
							failedSends = 0;
						}
					}
					if (messagesToSend.isEmpty()) {
						messageToSend = false;
					}
				}
			}
		});
		// outputThread.setDaemon(true);
		outputThread.start();
	}

	/**
	 * All logic relating to messages being received by the Socket.
	 */
	private void initalizeInputThread() {

		inputThread = new Thread(() -> {
			while (!socket.isClosed()) {
				try {
					String message = inputStream.readUTF();
					onMessageReceived(message);
				} catch (IOException e) {
				}
			}
		});
		// inputThread.setDaemon(true);
		inputThread.start();
	}

	/**
	 * Ran whenever the Socket receives a message
	 */
	private void onMessageReceived(String message) {
		socketHandler.onMessageReceived(new ThreadedSocketEvent(this, message));
	}

	/**
	 * Called externally to send a message through the Socket
	 */
	public void sendMessage(String message) {
		messagesToSend.addLast(message);
		messageToSend = true;
	}

	/**
	 * Changes the name associated with the Socket
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void setNameClient(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}

	public void finalize() {
		try {
			while (messageToSend) {
				System.out.println("messages to send");
			}
			socketHandler.onSocketClose(this);
			inputStream.close();
			outputStream.close();
			socket.close();
			inputThread.stop();
			outputThread.stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
