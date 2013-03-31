package com.ibm.actor;

/**
 * A message between actors.
 * 
 * @author BFEIGENB
 *
 */
public interface Message {
	/** Get the sender of the message. */
	Actor getSource();

	/** Get the subject (AKA command) of the message. */
	String getSubject();

	/** Get any parameter data associated with the message. */
	Object getData();
}
