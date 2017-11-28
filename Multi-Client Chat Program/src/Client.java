import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client extends ThreadedSocketHandler {
	private ThreadedSocket socket;

	public Client() throws IOException {
		Socket s = new Socket(Server.HOST, Server.PORT);
		socket = new ThreadedSocket(this, s);
		Scanner input = new Scanner(System.in);
		while (true) {
			onMessageSent(new ThreadedSocketEvent(socket, input.nextLine()));
		}
	}

	@Override
	public void onMessageReceived(ThreadedSocketEvent event) {
		System.out.println(event.message);
	}

	@Override
	public void onMessageSent(ThreadedSocketEvent event) {
		socket.sendMessage(event.message);
	}

	public static void main(String[] args) {
		try {
			Client client = new Client();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
