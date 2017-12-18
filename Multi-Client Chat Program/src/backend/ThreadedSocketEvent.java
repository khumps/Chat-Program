package backend;
/**
 * An event is created every time a message is sent or received over a
 * ThreadedSocket
 * 
 * @author Kevin Humphreys
 *
 */
public class ThreadedSocketEvent {
	/**
	 * The ThreadedSocket that created the event
	 */
	public final ThreadedSocket initiator;
	/**
	 * The message that was sent
	 */
	public final String message;

	public ThreadedSocketEvent(ThreadedSocket initiator, String message) {
		this.initiator = initiator;
		this.message = message;
	}
}
