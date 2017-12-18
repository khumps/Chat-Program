package backend;

/**
 * A container class to handle events from one or more ThreadedSockets's
 * 
 * @author Kevin Humphreys
 *
 */
public interface ThreadedSocketHandler {

	/**
	 * Called whenever a contained ThreadedSocket receives a message
	 * 
	 * @param socket
	 * @param message
	 */
	public abstract void onMessageReceived(ThreadedSocketEvent event);

	/**
	 * Called whenever a contained ThreadedSocket sends a message
	 * 
	 * @param socket
	 *            The socket sending the message
	 * @param message
	 *            The message that was sent
	 */
	public abstract void onMessageSent(ThreadedSocketEvent event);

	public abstract void onSocketClose(ThreadedSocket socket);
}
