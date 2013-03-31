package com.ibm.actor;

/**
 * An Actor is a (repeatedly) dispatchable unit of execution. Actors accept
 * messages and perform the operation (subject) they request. In response to
 * messages actors can perform business logic and/or send messages to other
 * actors (including themselves).
 * 
 * Actors can be grouped into categories. This allows senders to direct messages
 * to a category and have them processed by an arbitrary actor supporting that
 * category. Often some load balancing scheme is used to distribute messages
 * between actors if multiple actors belong to a category.
 * 
 * Since an actor consumes a thread while processing a message and threads are a
 * constrained resource, the actor should keep message processing as brief as
 * possible. If possible, long running operations should be broken down into a
 * sequence of messages.
 * 
 * Actors should not take much time to process a message - in particular they
 * should not block on long running actions, such as waiting for human input and
 * sometime I/O; if they must do this, some form of polling (repeated message
 * sends checking for completion, perhaps after a time delay) should be used to
 * implement a wait.
 * 
 * Messages have subjects and optional data. Subjects (AKA commands) determine
 * what the message is requesting and the data (often an array, list or map) are
 * any parameters to the subject.
 * 
 * The run() method is called once when the actor registers with a manager. The
 * loop() message is called whenever a message is received; actors can provide
 * implementations to allow filtering of messages before reception.
 * 
 * @author bfeigenb
 * @see ActorManager
 * 
 */
public interface Actor extends Runnable {
	String DEFAULT_CATEGORY = "default";

	/** Get an actor's name. Must not be null once assigned to a manager */
	String getName();

	/**
	 * Set an actors name. Use with caution as the name must not change after
	 * being assigned to a manager.
	 */
	void setName(String name);

	/**
	 * Get the actor's category. The category is used to group actors as send
	 * targets.
	 */
	String getCategory();

	/** Set the actor's category. Do not set to null (use DEFAULT_CATEGORY). */
	void setCategory(String category);

	/** Process the next incoming message. */
	boolean receive();

	/** Test if this actor will receive a send with a specified subject. */
	boolean willReceive(String subject);

	/** See if there is a pending message. */
	Message peekNext();

	/**
	 * See if there is a pending message for a subject using exact match.
	 * 
	 * @param subject
	 *            target subject of the message
	 */
	Message peekNext(String subject);

	/**
	 * See if there is a pending message for a subject using reg expr match.
	 * 
	 * @param subject
	 *            target subject of the message
	 * @param isRegExpr
	 *            the subject is a reg expr (vs. simple string)
	 * 
	 */
	Message peekNext(String subject, boolean isRegExpr);

	/**
	 * Remove (cancel) an unprocessed message.
	 * 
	 * @param message
	 *            to remove
	 * @return true if removed
	 */
	boolean remove(Message message);

	/**
	 * Tell this actor it is active. Messages may be received before this call
	 * but they should be ignored.
	 **/
	void activate();

	/**
	 * Tell this actor it is no longer active. Messages may be received after
	 * this call but they should be ignored.
	 **/
	void deactivate();

	/**
	 * Temporarily suspend the reception of messages.
	 * 
	 * @param f
	 */
	void setSuspended(boolean f);

	/** Test to see if suspended. */
	boolean isSuspended();

	/**
	 * End reception of new messages. Typically used before removing an actor.
	 * Actors with pending messages should not be removed.
	 * 
	 * */
	void shutdown();

	/** Has shutdown() been called. */
	boolean isShutdown();

	/** Get the number of pending messages. */
	int getMessageCount();

	/**
	 * Get the maximum number of allowed pending messages. Should be &gt; 0;
	 * rarely more than (say) 100.
	 */
	int getMaxMessageCount();

}
