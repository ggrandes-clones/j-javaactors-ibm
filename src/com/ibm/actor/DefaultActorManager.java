package com.ibm.actor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.actor.utils.Utils;

/**
 * Default ActorManager implementation. 
 * 
 * @author BFEIGENB
 *
 */
public class DefaultActorManager extends Utils implements ActorManager {

	public static final int DEFAULT_ACTOR_THREAD_COUNT = 10;

	protected static DefaultActorManager instance;

	/**
	 * Get the default instance. Uses ActorManager.properties for configuration.
	 * 
	 * @return shared instance
	 */
	public static DefaultActorManager getDefaultInstance() {
		if (instance == null) {
			instance = new DefaultActorManager();
			Map<String, Object> options = null;
			// ConfigUtils configUtils = new ConfigUtils();
			// Properties p = configUtils
			// .loadProperties("ActorManager.properties");
			Properties p = new Properties();
			try {
				p.load(new FileInputStream("ActorManager.properties"));
			} catch (IOException e) {
				try {
					p.load(new FileInputStream("/resource/ActorManager.properties"));
				} catch (IOException e1) {
					logger.warning("DefaultActorManager: no configutration: " + e);
				}
			}
			if (!isEmpty(p)) {
				options = new HashMap<String, Object>();
				for (Object key : p.keySet()) {
					String skey = (String) key;
					options.put(skey, p.getProperty(skey));
				}
			}
			instance.initialize(options);
		}
		return instance;
	}

	/** Configuration key for thread count. */
	public static final String ACTOR_THREAD_COUNT = "threadCount";

	protected Map<String, AbstractActor> actors = new LinkedHashMap<String, AbstractActor>();

	protected Map<String, AbstractActor> runnables = new LinkedHashMap<String, AbstractActor>();

	protected Map<String, AbstractActor> waiters = new LinkedHashMap<String, AbstractActor>();

	/**
	 * Detach an actor.
	 */
	@Override
	public void detachActor(Actor actor) {
		if (((AbstractActor) actor).getManager() != this) {
			throw new IllegalStateException("actor not owned by this manager");
		}
		String name = actor.getName();
		synchronized (actors) {
			if (actors.containsKey(name)) {
				((AbstractActor) actor).setManager(null);
				actors.remove(name);
				runnables.remove(name);
				waiters.remove(name);
			} else {
				actor = null;
			}
		}
		if (actor != null) {
			actor.deactivate();
		}
	}

	/**
	 * Detach all actors.
	 */
	public void detachAllActors() {
		Set<String> xkeys = new HashSet<String>();
		xkeys.addAll(actors.keySet());
		Iterator<String> i = xkeys.iterator();
		while (i.hasNext()) {
			detachActor(actors.get(i.next()));
		}
		synchronized (actors) {
			actors.clear();
			runnables.clear();
			waiters.clear();
		}
	}

	protected Random rand = new Random();

	/**
	 * Create a list of actors in a pseudo-random order. 
	 * 
	 */
	public void randomizeActors() {
		synchronized (actors) {
			AbstractActor[] xactors = getActors();
			List<AbstractActor> zactors = new ArrayList<AbstractActor>(xactors.length);
			for (AbstractActor a : xactors) {
				zactors.add(rand.nextInt(zactors.size() + 1), a);
			}
			actors.clear();
			for (AbstractActor a : zactors) {
				actors.put(a.getName(), a);
			}
		}
	}

	/**
	 * Count the number of actors of a given type.
	 * 
	 * @param type the class to count (also its subclasses)
	 */
	@Override
	public int getActorCount(Class type) {
		int res = 0;
		if (type != null) {
			synchronized (actors) {
				for (String key : actors.keySet()) {
					Actor a = actors.get(key);
					if (type.isAssignableFrom(a.getClass())) {
						res++;
					}
				}
			}
		} else {
			synchronized (actors) {
				res = actors.size();
			}
		}
		return res;
	}

	/**
	 * Get actors managed by this manager.
	 * 
	 * @return actors
	 */
	public AbstractActor[] getActors() {
		AbstractActor[] res = new AbstractActor[actors.size()];
		copyMembers(res);
		return res;
	}

	protected void copyMembers(AbstractActor[] res) {
		int count = 0;
		synchronized (actors) {
			for (String key : actors.keySet()) {
				res[count++] = actors.get(key);
			}
		}
	}

	protected Map<String, List<Message>> sentMessages = new HashMap<String, List<Message>>();

	protected boolean recordSentMessages = true;

	public boolean getRecordSentMessages() {
		return recordSentMessages;
	}

	public void setRecordSentMessages(boolean recordSentMessages) {
		this.recordSentMessages = recordSentMessages;
	}

	/**
	 * Get a list of pending messages and then clear it.
	 * 
	 * @param actor
	 *            receiving actor
	 * @return
	 */
	public Message[] getAndClearSentMessages(Actor actor) {
		List<Message> res = null;
		synchronized (sentMessages) {
			List<Message> l = sentMessages.get(actor.getName());
			if (!isEmpty(l)) {
				res = new LinkedList<Message>();
				res.addAll(l);
				l.clear();
			}
		}
		return res != null ? res.toArray(new Message[res.size()]) : null;
	}

	volatile protected long lastSendTime, lastDispatchTime;

	public long getLastSendTime() {
		return lastSendTime;
	}

	public long getLastDispatchTime() {
		return lastDispatchTime;
	}

	volatile protected int sendCount, lastSendCount;
	volatile protected int dispatchCount, lastDispatchCount;

	/** Get most recent sends/second count. */
	public int getSendPerSecondCount() {
		return lastSendCount;
	}

	/** Get most recent thread dispatches/second count. */
	public int getDispatchPerSecondCount() {
		synchronized (actors) {
			return lastDispatchCount;
		}
	}

	protected void incDispatchCount() {
		synchronized (actors) {
			dispatchCount += 1;
			lastDispatchTime = new Date().getTime();
			// logger.info("incDispatchCount: dc=%d", dispatchCount);
		}
	}

	protected void clearDispatchCount() {
		synchronized (actors) {
			dispatchCount = 0;
			lastDispatchCount = 0;
			// logger.info("clearDispatchCount: dc=%d, ldc=%d", dispatchCount,
			// lastDispatchCount);
		}
	}

	protected void updateLastDispatchCount() {
		synchronized (actors) {
			lastDispatchCount = dispatchCount;
			dispatchCount = 0;
			// logger.info("updateLastDispatchCount: dc=%d, ldc=%d",
			// dispatchCount, lastDispatchCount);
		}
	}

	/**
	 * Send a message.
	 * 
	 * @param message
	 *            message to
	 * @param from
	 *            source actor
	 * @param to
	 *            target actor
	 * @return number of receiving actors
	 */
	@Override
	public int send(Message message, Actor from, Actor to) {
		int count = 0;
		if (message != null) {
			AbstractActor aa = (AbstractActor) to;
			if (aa != null) {
				if (!aa.isShutdown() && !aa.isSuspended() && aa.willReceive(message.getSubject())) {
					DefaultMessage xmessage = (DefaultMessage) ((DefaultMessage) message).assignSender(from);
					// logger.trace(" %s to %s", xmessage, to);
					aa.addMessage(xmessage);
					xmessage.fireMessageListeners(new MessageEvent(aa, xmessage, MessageEvent.MessageStatus.SENT));
					sendCount++;
					lastSendTime = new Date().getTime();
					if (recordSentMessages) {
						synchronized (sentMessages) {
							String aname = aa.getName();
							List<Message> l = sentMessages.get(aname);
							if (l == null) {
								l = new LinkedList<Message>();
								sentMessages.put(aname, l);
							}
							// keep from getting too big
							if (l.size() < 100) {
								l.add(xmessage);
							}
						}
					}
					count++;
					synchronized (actors) {
						actors.notifyAll();
					}
				}
			}
		}
		return count;
	}

	/**
	 * Send a message.
	 * 
	 * @param message
	 *            message to
	 * @param from
	 *            source actor
	 * @param to
	 *            target actors
	 * @return number of receiving actors
	 */
	@Override
	public int send(Message message, Actor from, Actor[] to) {
		int count = 0;
		for (Actor a : to) {
			count += send(message, from, a);
		}
		return count;
	}

	/**
	 * Send a message.
	 * 
	 * @param message
	 *            message to
	 * @param from
	 *            source actor
	 * @param to
	 *            target actors
	 * @return number of receiving actors
	 */
	@Override
	public int send(Message message, Actor from, Collection<Actor> to) {
		int count = 0;
		for (Actor a : to) {
			count += send(message, from, a);
		}
		return count;
	}

	/**
	 * Send a message.
	 * 
	 * @param message
	 *            message to
	 * @param from
	 *            source actor
	 * @param category
	 *            target actor category
	 * @return number of receiving actors
	 */
	@Override
	public int send(Message message, Actor from, String category) {
		int count = 0;
		Map<String, Actor> xactors = cloneActors();
		List<Actor> catMembers = new LinkedList<Actor>();
		for (String key : xactors.keySet()) {
			Actor to = xactors.get(key);
			if (category.equals(to.getCategory()) && (to.getMessageCount() < to.getMaxMessageCount())) {
				catMembers.add(to);
			}
		}
		// find an actor with lowest message count
		int min = Integer.MAX_VALUE;
		Actor amin = null;
		for (Actor a : catMembers) {
			int mcount = a.getMessageCount();
			if (mcount < min) {
				min = mcount;
				amin = a;
			}
		}
		if (amin != null) {
			count += send(message, from, amin);
			// } else {
			// throw new
			// IllegalStateException("no capable actors for category: " +
			// category);
		}
		return count;
	}

	/**
	 * Send a message to all actors.
	 * 
	 * @param message
	 *            message to
	 * @param from
	 *            source actor
	 * @return number of receiving actors
	 */
	@Override
	public int broadcast(Message message, Actor from) {
		int count = 0;
		Map<String, Actor> xactors = cloneActors();
		for (String key : xactors.keySet()) {
			Actor to = xactors.get(key);
			count += send(message, from, to);
		}
		return count;
	}

	/**
	 * Get the current categories.
	 * 
	 * @return categories
	 */
	@Override
	public Set<String> getCategories() {
		Map<String, Actor> xactors = cloneActors();
		Set<String> res = new TreeSet<String>();
		for (String key : xactors.keySet()) {
			Actor a = xactors.get(key);
			res.add(a.getCategory());
		}
		return res;
	}
	
	/**
	 * Get the number of actors in a category. 
	 * 
	 * @param name
	 * @return
	 */
	public int getCategorySize(String name) {
		Map<String, Actor> xactors = cloneActors();
		int res = 0;
		for (String key : xactors.keySet()) {
			Actor a = xactors.get(key);
			if (a.getCategory().equals(name)) {
				res ++;
			}
		}
		return res;
	}

	protected Map<String, Actor> cloneActors() {
		Map<String, Actor> xactors;
		synchronized (actors) {
			xactors = new HashMap<String, Actor>(actors);
		}
		return xactors;
	}

	/**
	 * Suspend an actor until it has a read message.
	 * 
	 * @param actor
	 *            receiving actor
	 */
	public void awaitMessage(AbstractActor actor) {
		synchronized (actors) {
			waiters.put(actor.getName(), actor);
			// actors.notifyAll();
			// logger.trace("awaitMessage waiters=%d: %s",waiters.size(), a);
		}
	}

	protected Map<String, ActorRunnable> trunnables = new HashMap<String, ActorRunnable>();

	/**
	 * Get the Runnable by name.
	 * 
	 * @param name
	 *            thread name
	 * @return runnable
	 */
	public ActorRunnable getRunnable(String name) {
		return trunnables.get(name);
	}

	/**
	 * Get the number of busy runnables (equivalent to threads).
	 * @return
	 */
	public int getActiveRunnableCount() {
		int res = 0;
		synchronized (actors) {
			for (String key : trunnables.keySet()) {
				if (trunnables.get(key).hasThread) {
					res++;
				}
			}
		}
		return res;

	}

	/**
	 * Add a dynamic thread. 
	 * 
	 * @param name
	 * @return
	 */
	public Thread addThread(String name) {
		Thread t = null;
		synchronized (actors) {
			if (trunnables.containsKey(name)) {
				throw new IllegalStateException("already exists: " + name);
			}
			ActorRunnable r = new ActorRunnable();
			trunnables.put(name, r);
			t = new Thread(threadGroup, r, name);
			threads.add(t);
			//System.out.printf("addThread: %s", name);
		}
		t.setDaemon(true);
		t.setPriority(getThreadPriority());
		return t;
	}

	/**
	 *  Remove a dynamic thread. 
	 * 
	 * @param name
	 */
	public void removeThread(String name) {
		synchronized (actors) {
			if (!trunnables.containsKey(name)) {
				throw new IllegalStateException("not running: " + name);
			}
			//System.out.printf("removeThread: %s", name);
			trunnables.remove(name);
			Iterator<Thread> i = threads.iterator();
			while(i.hasNext() ) {
				Thread xt = i.next();
				if(xt.getName().equals(name)) {
					i.remove();
					xt.interrupt();
					break;
				}
			}
		}
	}

	protected ThreadGroup threadGroup;

	public ThreadGroup getThreadGroup() {
		return threadGroup;
	}

	protected void createThread(int i) {
		addThread("actor" + i);
	}

	/**
	 * Initialize this manager. Call only once.
	 */
	@Override
	public void initialize() {
		initialize(null);
	}

	private boolean initialized;
	
	/**
	 * Initialize this manager. Call only once.
	 * 
	 * @param options
	 *            map of options
	 */
	@Override
	public void initialize(Map<String, Object> options) {
		if (!initialized) {
			initialized = true;
			int count = getThreadCount(options);
			ThreadGroup tg = new ThreadGroup("ActorManager" + groupCount++);
			threadGroup = tg;
			for (int i = 0; i < count; i++) {
				createThread(i);
			}
			running = true;
			for (Thread t : threads) {
				// logger.trace("procesNextActor starting %s", t);
				t.start();
			}

			Thread Counter = new Thread(new Runnable() {
				@Override
				public void run() {
					while (running) {
						try {
							trendValue = sendCount - dispatchCount;
							// logger.trace("Counter thread: sc=%d, dc=%d, t=%d",
							// sendCount, dispatchCount, trendValue);
							lastSendCount = sendCount;
							sendCount = 0;
							updateLastDispatchCount();
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							break;
						}
					}
					sendCount = lastSendCount = 0;
					clearDispatchCount();
				}
			});
			Counter.setDaemon(true);
			lastDispatchTime = lastSendTime = new Date().getTime();
			Counter.start();
		}
	}

	/**
	 * Get the thread priority to use. Default is 1 less than current.
	 * 
	 * @return priority value
	 */
	public int getThreadPriority() {
		return Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 1);
	}

	protected int getThreadCount(Map<String, Object> options) {
		Integer count = null;
		Object xcount = options != null ? options.get(ACTOR_THREAD_COUNT) : null;
		if (xcount != null) {
			if (xcount instanceof Integer) {
				count = (Integer) xcount;
			} else {
				count = Integer.parseInt((String) xcount.toString());
			}
		}
		if (count == null) {
			count = DEFAULT_ACTOR_THREAD_COUNT;
		}
		return count;
	}

	/** public intended only for "friend" access. */
	public class ActorRunnable implements Runnable {
		public boolean hasThread;
		public AbstractActor actor;

		public void run() {
			// logger.trace("procesNextActor starting");
			int delay = 1;
			while (running) {
				try {
					if (!procesNextActor()) {
						// logger.trace("procesNextActor waiting on actor");
						// sleep(delay * 1000);
						synchronized (actors) {
							// TOOD: adjust this delay; possible parameter
							// we want to minizmize overhead (make bigger);
							// but it has a big impact on message processing
							// rate (makesmaller)
							// actors.wait(delay * 1000);
							actors.wait(100);
						}
						delay = Math.max(5, delay + 1);
					} else {
						delay = 1;
					}
				} catch (InterruptedException e) {
				} catch (Exception e) {
					logger.error("procesNextActor exception", e);
				}
			}
			// logger.trace("procesNextActor ended");
		}

		protected boolean procesNextActor() {
			boolean run = false, wait = false, res = false;
			actor = null;
			synchronized (actors) {
				for (String key : runnables.keySet()) {
					actor = runnables.remove(key);
					break;
				}
			}
			if (actor != null) {
				// first run never started
				run = true;
				actor.setHasThread(true);
				hasThread = true;
				try {
					actor.run();
				} finally {
					actor.setHasThread(false);
					hasThread = false;
				}
			} else {
				synchronized (actors) {
					for (String key : waiters.keySet()) {
						actor = waiters.remove(key);
						break;
					}
				}
				if (actor != null) {
					// then waiting for responses
					wait = true;
					actor.setHasThread(true);
					hasThread = true;
					try {
						res = actor.receive();
						if (res) {
							incDispatchCount();
						}
					} finally {
						actor.setHasThread(false);
						hasThread = false;
					}
				}
			}
			// if (!(!run && wait && !res) && a != null) {
			// logger.trace("procesNextActor %b/%b/%b: %s", run, wait, res, a);
			// }
			return run || res;
		}
	}

	protected static int groupCount;

	protected List<Thread> threads = new LinkedList<Thread>();

	/**
	 * Get the actor threads. 
	 * 
	 * @return
	 */
	public Thread[] getThreads() {
		return threads.toArray(new Thread[threads.size()]);
	}

	/**
	 * Terminate processing and wait for all threads to stop.
	 */
	@Override
	public void terminateAndWait() {
		logger.trace("terminateAndWait waiting on termination of %d threads", threads.size());
		terminate();
		waitForThreads();
	}

	/**
	 * Wait for all threads to stop. Must have issued terminate.
	 */
	public void waitForThreads() {
		if (!terminated) {
			throw new IllegalStateException("not terminated");
		}
		for (Thread t : threads) {
			try {
				// logger.info("terminateAndWait waiting for %s...", t);
				t.join();
			} catch (InterruptedException e) {
				// logger.info("terminateAndWait interrupt");
			}
		}
	}

	boolean running, terminated;

	/**
	 * Terminate processing.
	 */
	@Override
	public void terminate() {
		terminated = true;
		running = false;
		for (Thread t : threads) {
			t.interrupt();
		}
		synchronized (actors) {
			for (String key : actors.keySet()) {
				actors.get(key).deactivate();
			}
		}
		sentMessages.clear();
		sendCount = lastSendCount = 0;
		clearDispatchCount();
	}

	/**
	 * Create an actor and associate it with this manager.
	 * 
	 * @param clazz
	 *            the actor class
	 * @param the
	 *            actor name; must be unique
	 */
	@Override
	public Actor createActor(Class<? extends Actor> clazz, String name) {
		return createActor(clazz, name, null);
	}

	/**
	 * Create an actor and associate it with this manager then start it
	 * 
	 * @param clazz
	 *            the actor class
	 * @param the
	 *            actor name; must be unique
	 */
	@Override
	public Actor createAndStartActor(Class<? extends Actor> clazz, String name) {
		return createAndStartActor(clazz, name, null);
	}

	/**
	 * Create an actor and associate it with this manager then start it.
	 * 
	 * @param clazz
	 *            the actor class
	 * @param the
	 *            actor name; must be unique
	 * @param options
	 *            actor options
	 */
	@Override
	public Actor createAndStartActor(Class<? extends Actor> clazz, String name, Map<String, Object> options) {
		Actor res = createActor(clazz, name, options);
		startActor(res);
		return res;
	}

	/**
	 * Create an actor and associate it with this manager.
	 * 
	 * @param clazz
	 *            the actor class
	 * @param the
	 *            actor name; must be unique
	 * @param options
	 *            actor options
	 */
	@Override
	public Actor createActor(Class<? extends Actor> clazz, String name, Map<String, Object> options) {
		AbstractActor a = null;
		synchronized (actors) {
			if (!actors.containsKey(name)) {
				try {
					a = (AbstractActor) clazz.newInstance();
					a.setName(name);
					a.setManager(this);
				} catch (Exception e) {
					throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(
							"mapped exception: " + e, e);
				}
			} else {
				throw new IllegalArgumentException("name already in use: " + name);
			}
		}
		return a;
	}

	/**
	 * Start an actor. Must have been created by this manager.
	 * 
	 * @param actor
	 *            the actor
	 */
	@Override
	public void startActor(Actor actor) {
		if (((AbstractActor) actor).getManager() != this) {
			throw new IllegalStateException("actor not owned by this manager");
		}
		String name = actor.getName();
		synchronized (actors) {
			if (actors.containsKey(name)) {
				throw new IllegalStateException("already started");
			}
			((AbstractActor) actor).shutdown = false;
			actors.put(name, (AbstractActor) actor);
			runnables.put(name, (AbstractActor) actor);
		}
		actor.activate();
	}

	protected int trendValue = 0, maxTrendValue = 10;

	public int getTrendValue() {
		return trendValue;
	}

	public void setTrendValue(int trendValue) {
		this.trendValue = trendValue;
	}

	public int getMaxTrendValue() {
		return maxTrendValue;
	}

	public void setMaxTrendValue(int maxTrendValue) {
		this.maxTrendValue = maxTrendValue;
	}
}
