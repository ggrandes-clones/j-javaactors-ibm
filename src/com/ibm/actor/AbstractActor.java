package com.ibm.actor;

// TODO: add this to all others
/*
 * Copyright (C) IBM Corportation, 2102.  All rights reserved.
 * Copyright (C) Barry Feigenbaum, 2102.  All rights reserved.
 */




import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.actor.utils.Utils;

/**
 * A partial implementation of an Actor for running in a DefaultActorManager. 
 * 
 * @author BFEIGENB
 *
 */
public abstract class AbstractActor extends Utils implements Actor {
	public static final int DEFAULT_MAX_MESSAGES = 100;
	protected DefaultActorManager manager;

	public ActorManager getManager() {
		return manager;
	}

	public void setManager(DefaultActorManager manager) {
		if (this.manager != null && manager != null) {
			throw new IllegalStateException(
					"cannot change manager of attached actor");
		}
		this.manager = manager;
	}

	protected String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		if (manager != null) {
			throw new IllegalStateException("cannot change name if manager set");
		}
		this.name = name;
	}

	protected String category = DEFAULT_CATEGORY;

	@Override
	public String getCategory() {
		return category;
	}

	@Override
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * Process a message conditionally. If testMessage() returns null no message
	 * will be consumed.
	 * 
	 * @see AbstractActor#testMessage()
	 */
	@Override
	public boolean receive() {
		Message m = testMessage();
		boolean res = m != null;
		if (res) {
			boolean f = remove(m);
			if (!f) {
				logger.warning("receive message not removed: %s", m);
			}
			DefaultMessage dm = (DefaultMessage) m;
			try {
				dm.fireMessageListeners(new MessageEvent(this, dm, MessageEvent.MessageStatus.DELIVERED));
				//logger.trace("receive %s processing %s", this.getName(), m);
				loopBody(m);
				dm.fireMessageListeners(new MessageEvent(this, dm, MessageEvent.MessageStatus.COMPLETED));
			} catch (Exception e) {
				dm.fireMessageListeners(new MessageEvent(this, dm, MessageEvent.MessageStatus.FAILED));
				logger.error("loop exception", e);
			}
		}
		manager.awaitMessage(this);
		return res;
	}

	/**
	 * Test to see if a message should be processed. Subclasses should override
	 */
	@Override
	public boolean willReceive(String subject) {
		return !isEmpty(subject); // default receive all subjects
	}

	/** Test the current message. Default action is to accept all. */
	protected Message testMessage() {
		return getMatch(null, false);
	}

	/** Process the accepted subject. */
	abstract protected void loopBody(Message m);

	/** Test a message against a defined subject pattern. */
	protected DefaultMessage getMatch(String subject, boolean isRegExpr) {
		DefaultMessage res = null;
		synchronized (messages) {
			res = (DefaultMessage) peekNext(subject, isRegExpr);
		}
		return res;
	}

	protected List<DefaultMessage> messages = new LinkedList<DefaultMessage>();

	public DefaultMessage[] getMessages() {
		return messages.toArray(new DefaultMessage[messages.size()]);
	}

	@Override
	public int getMessageCount() {
		synchronized (messages) {
			return messages.size();
		}
	}

	/**
	 * Limit the number of messages that can be received.  Subclasses should override.
	 */
	@Override
	public int getMaxMessageCount() {
		return DEFAULT_MAX_MESSAGES;
	}

	/** Queue a messaged to be processed later. */
	public void addMessage(DefaultMessage message) {
		if (message != null) {
			synchronized (messages) {
				if (messages.size() < getMaxMessageCount()) {
					messages.add(message);
					// messages.notifyAll();
				} else {
					throw new IllegalStateException("too many messages, cannot add");
				}
			}
		} 
	}

	@Override
	public Message peekNext() {
		return peekNext(null);
	}

	@Override
	public Message peekNext(String subject) {
		return peekNext(subject, false);
	}

	/** 
	 * See if a message exists that meets the selection criteria. 
	 **/
	@Override
	public Message peekNext(String subject, boolean isRegExpr) {
		Message res = null;
		if (isActive) {
			Pattern p = subject != null ? (isRegExpr ? Pattern.compile(subject)
					: null) : null;
			long now = new Date().getTime();
			synchronized (messages) {
				for (DefaultMessage m : messages) {
					if (m.getDelayUntil() <= now) {
						boolean match = subject == null
								|| (isRegExpr ? m.subjectMatches(p) : m
										.subjectMatches(subject));
						if (match) {
							res = m;
							break;
						}
					}
				}
			}
		}
		// logger.trace("peekNext %s, %b: %s", subject, isRegExpr, res);
		return res;
	}

	@Override
	public boolean remove(Message message) {
		synchronized (messages) {
			return messages.remove(message);
		}
	}

	protected boolean isActive;

	public boolean isActive() {
		return isActive;
	}

	@Override
	public void activate() {
		isActive = true;
	}

	@Override
	public void deactivate() {
		isActive = false;
	}

	/** Do startup processing. */
	protected void runBody() {
		DefaultMessage m = new DefaultMessage("init");
		getManager().send(m, null, this);
	}

	@Override
	public void run() {
		runBody();
		((DefaultActorManager) getManager()).awaitMessage(this);
	}

	protected boolean hasThread;

	public boolean getHasThread() {
		return hasThread;
	}

	protected void setHasThread(boolean hasThread) {
		this.hasThread = hasThread;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bodyString() + "]";
	}

	protected String bodyString() {
		return "name=" + name + ", category=" + category + ", messages="
				+ messages.size();
	}

	volatile protected boolean shutdown;

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public void shutdown() {
		shutdown = true;
	}

	volatile protected boolean suspended;

	@Override
	public void setSuspended(boolean f) {
		suspended = f;
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}
}
