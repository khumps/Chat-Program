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
	private final Socket socket;
	private final DataInputStream inputStream; // Receives data from the socket
	private final DataOutputStream outputStream; // Sends data over the socket
	private Thread inputThread;
	private Thread outputThread;
	private boolean messageToSend;
	private ArrayDeque<String> messagesToSend;

	public ThreadedSocket(ThreadedSocketHandler socketHandler, Socket socket) throws IOException {
		name = "Guest";
		this.socket = socket;
		this.socketHandler = socketHandler;
		inputStream = new DataInputStream(socket.getInputStream());
		outputStream = new DataOutputStream(socket.getOutputStream());
		messagesToSend = new ArrayDeque<>();
		initalizeThreads();
	}

	private void initalizeThreads() {
		initalizeInputThread();
		initializeOutputThread();
	}

	private void initializeOutputThread() {
		messageToSend = false;
		outputThread = new Thread(() -> {
			while (!socket.isClosed()) {
				while (messageToSend) {
					try {
						String message = messagesToSend.peekFirst();
						outputStream.writeUTF(message);
						messagesToSend.removeFirst();
					} catch (IOException e) {
						System.err.println("Message failed to send");
					}
					if (messagesToSend.isEmpty()) {
						messageToSend = false;
					}
				}
			}
		});
		outputThread.start();
	}

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
		inputThread.start();
	}

	private void onMessageReceived(String message) {
		socketHandler.onMessageReceived(new ThreadedSocketEvent(this, message));
	}

	public void sendMessage(String message) {
		messagesToSend.addLast(message);
		messageToSend = true;
	}

	public void setName(String name) {
		sendMessage(this.name + " has changed their name to " + name);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}
}
