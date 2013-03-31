package com.ibm.actor.logging;

/**
 * A logging service.
 * 
 * @author bfeigenb
 * 
 */
public interface Logger {

	/**
	 * Log an INFO message.
	 * 
	 * @param message
	 *            content to log (may include substitution symbols)
	 * @param values
	 *            to substitute into the message
	 * 
	 * @see String#formatNames
	 */
	public abstract void info(String message, Object... values);

	/**
	 * Log a TRACE message.
	 * 
	 * @param message
	 *            content to log (may include substitution symbols)
	 * @param values
	 *            to substitute into the message
	 * 
	 * @see String#formatNames
	 */
	public abstract void trace(String message, Object... values);

	/**
	 * Log an WARNINGmessage.
	 * 
	 * @param message
	 *            content to log (may include substitution symbols)
	 * @param values
	 *            to substitute into the message
	 * 
	 * @see String#formatNames
	 */
	public abstract void warning(String message, Object... values);

	/**
	 * Log an ERROR message. If the values list ends in an Exception, then add
	 * an implied substitution at the end of the message.
	 * 
	 * @param message
	 *            content to log (may include substitution symbols)
	 * @param values
	 *            to substitute into the message
	 * 
	 * @see String#formatNames
	 */
	public abstract void error(String message, Object... values);

	/**
	 * Log an NOTIFY message. Like an ERROR message but also send an
	 * configuration defined email. If the values list ends in an Exception,
	 * then add an implied substitution at the end of the message.
	 * 
	 * @param message
	 *            content to log (may include substitution symbols)
	 * @param values
	 *            to substitute into the message
	 * 
	 * @see String#formatNames
	 */
	public abstract void notify(String message, Object... values);

}