package com.ibm.actor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.actor.utils.Utils;

/**
 * A default message implementation. 
 * 
 * @author BFEIGENB
 *
 */
public class DefaultMessage extends Utils implements Message {

	@ForToString
	protected long delayUntil = -1; // works like Long.MIN_VALUE;

	/** Ge the delay value. */
	public long getDelayUntil() {
		return delayUntil;
	}

	/**
	 * Used to delay message execution until some moment in time has passed.
	 * 
	 * @param delayUntil
	 *            future time (in millis since epoch)
	 **/
	public void setDelayUntil(long delayUntil) {
		long now = new Date().getTime();
		if (delayUntil <= now) {
			throw new IllegalArgumentException("value should be in the future: " + delayUntil + " vs. " + now);
		}
		this.delayUntil = delayUntil;
	}

	protected Actor source;

	@Override
	public Actor getSource() {
		return source;
	}

	/** Sets the sender of this message; can be null. */
	protected void setSource(Actor source) {
		this.source = source;
	}

	protected String subject;

	@Override
	public String getSubject() {
		return subject;
	}

	/** Sets the subject (command) this message implies; can be null. */
	protected void setSubject(String subject) {
		this.subject = subject;
	}

	protected Object data;

	@Override
	public Object getData() {
		return data;
	}

	/** Sets data associated with this message; can be null. */
	protected void setData(Object data) {
		this.data = data;
	}

	public DefaultMessage(String subject, Object data) {
		this(subject);
		this.data = data;
	}

	public DefaultMessage(String subject) {
		this();
		this.subject = subject;
	}

	protected DefaultMessage() {
	}

	/** Set the sender of a clone of this message. */
	public Message assignSender(Actor sender) {
		DefaultMessage res = new DefaultMessage(subject, data);
		res.source = sender;
		return res;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bodyString() + "]";
	}

	protected String bodyString() {
		return "source=" + source + ", subject=" + subject + ", data=" + truncate(data) + ", delay=" + delayUntil;
	}

	/** Test if this message subject matches a string. */
	public boolean subjectMatches(String s) {
		return subject != null ? subject.equals(s) : false;
	}

	/** Test if this message subject matches a reg expr. */
	public boolean subjectMatches(Pattern p) {
		boolean res = false;
		if (p != null && subject != null) {
			Matcher m = p.matcher(subject);
			res = m.matches();
		}
		return res;
	}

	protected List<MessageListener> listeners = new LinkedList<MessageListener>();

	public void addMessageListener(MessageListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeMessageListener(MessageListener l) {
		listeners.remove(l);
	}
	
	public void fireMessageListeners(MessageEvent e) {
		for(MessageListener l : listeners) {
			l.onMessage(e);
		}
	}
}
