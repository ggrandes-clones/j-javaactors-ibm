package com.ibm.actor.test;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ibm.actor.AbstractActor;
import com.ibm.actor.Actor;
import com.ibm.actor.ActorManager;
import com.ibm.actor.DefaultActorManager;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;
import com.ibm.actor.MessageEvent;
import com.ibm.actor.MessageListener;
import com.ibm.actor.logging.DefaultLogger;
import com.ibm.actor.utils.Utils;

/** 
 * A set of runtime services for testing actors and a test case driver. 
 * 
 * @author BFEIGENB
 *
 */
public class DefaultActorTest extends Utils {

	public static final int MAX_IDLE_SECONDS = 10;

	// public static final int STEP_COUNT = 3 * 60;
	public static final int TEST_VALUE_COUNT = 1000; // TODO: make bigger

	public DefaultActorTest() {
		super();
	}

	private Map<String, Actor> testActors = new ConcurrentHashMap<String, Actor>();

	static Random rand = new Random();

	public static int nextInt(int limit) {
		return rand.nextInt(limit);
	}

	protected DefaultActorManager getManager() {
		DefaultActorManager am = actorManager != null ? actorManager : new DefaultActorManager();
		return am;
	}

	protected int stepCount = 120;

	public void setStepCount(int stepCount) {
		this.stepCount = stepCount;
	}

	public int getStepCount() {
		return stepCount;
	}

	protected int threadCount = 10;

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public void setTestActors(Map<String, Actor> testActors) {
		this.testActors = testActors;
	}

	public Map<String, Actor> getTestActors() {
		return testActors;
	}

	public static final int COMMON_ACTOR_COUNT = 10;
	public static final int TEST_ACTOR_COUNT = 25;
	public static final int PRODUCER_ACTOR_COUNT = 25;

	public static void sleeper(int seconds) {
		int millis = seconds * 1000 + -50 + nextInt(100); // a little
															// variation
		// logger.trace("sleep: %dms", millis);
		sleep(millis);
	}

	public static void dumpMessages(List<DefaultMessage> messages) {
		synchronized (messages) {
			if (messages.size() > 0) {
				for (DefaultMessage m : messages) {
					logger.info("%s", m);
				}
			}
		}
	}

	protected List<ChangeListener> listeners = new LinkedList<ChangeListener>();

	public void addChangeListener(ChangeListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}

	protected void fireChangeListeners(ChangeEvent e) {
		for (ChangeListener l : listeners) {
			l.stateChanged(e);
		}
	}

	protected static String[] types = new String[] { "widget", "framit", "frizzle", "gothca", "splat" };

	public static String[] getItemTypes() {
		return types;
	}

	public static void main(String[] args) {
		DefaultActorTest at = new DefaultActorTest();
		at.run(args);
		logger.trace("Done");
	}

	protected String title;

	public String getTitle() {
		return title;
	}

	public class ActorX extends AbstractActor {

		public ActorX() {
			super();
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void loopBody(Message m) {
			// logger.trace("ActorX:%s loopBody %s: %s", getName(), m, this);
			sleeper(1);
			String subject = m.getSubject();
			if ("repeat".equals(subject)) {
				int count = (Integer) m.getData();
				logger.trace("ActorX:%s repeat(%d) %s: %s", getName(), count, m, this);
				if (count > 0) {
					m = new DefaultMessage("repeat", count - 1);
					// logger.trace("TestActor loopBody send %s: %s", m, this);
					String toName = "actor" + nextInt(TEST_ACTOR_COUNT);
					Actor to = getTestActors().get(toName);
					if (to != null) {
						getManager().send(m, this, to);
					} else {
						logger.warning("repeat:%s to is null: %s", getName(), toName);
					}
				}
			} else if ("init".equals(subject)) {
				int count = (Integer) m.getData();
				count = nextInt(count) + 1;
				logger.trace("ActorX:%s init(%d): %s", getName(), count, this);
				for (int i = 0; i < count; i++) {
					sleeper(1);
					m = new DefaultMessage("repeat", count);
					// logger.trace("TestActor runBody send %s: %s", m, this);
					String toName = "actor" + nextInt(TEST_ACTOR_COUNT);
					Actor to = getTestActors().get(toName);
					if (to != null) {
						getManager().send(m, this, to);
					} else {
						logger.warning("init:%s to is null: %s", getName(), toName);
					}
					DefaultMessage dm = new DefaultMessage("repeat", count);
					dm.setDelayUntil(new Date().getTime() + (nextInt(5) + 1) * 1000);
					getManager().send(dm, this, this.getClass().getSimpleName());
				}
			} else {
				logger.warning("ActorX:%s loopBody unknown subject: %s", getName(), subject);
			}
		}

	}

	volatile protected boolean done;

	public void terminateRun() {
		done = true;
	}

	public static String[] getTestNames() {
		return new String[] { "Countdown", "Producer Consumer", /* "Quicksort", */"MapReduce", "Virus Scan", "All" };
	}

	DefaultActorManager actorManager;

	public DefaultActorManager getActorManager() {
		return actorManager;
	}

	public void setActorManager(DefaultActorManager actorManager) {
		this.actorManager = actorManager;
	}

	public void run(String[] args) {
		done = false;
		// DefaultLogger.getDefaultInstance().setIncludeDate(false);
		DefaultLogger.getDefaultInstance().setIncludeContext(false);
		DefaultLogger.getDefaultInstance().setIncludeCaller(false);
		// DefaultLogger.getDefaultInstance().setIncludeThread(false);
		DefaultLogger.getDefaultInstance().setLogToFile(false);
		DefaultLogger.getDefaultInstance().setThreadFieldWidth(10);

		int sc = stepCount;
		int tc = threadCount;
		boolean doTest = false, doProduceConsume = false, doQuicksort = false, doMapReduce = false, doVirusScan = false;
		title = "";
		for (int i = 0; i < args.length; i++) {
			String arg = args[i].toLowerCase();
			if (arg.startsWith("-")) {
				arg = arg.substring(1);
				if (arg.toLowerCase().startsWith("stepcount:")) {
					sc = Integer.parseInt(arg.substring("stepcount:".length()));
				} else if (arg.startsWith("sc:")) {
					sc = Integer.parseInt(arg.substring("sc:".length()));
				} else if (arg.toLowerCase().startsWith("threadcount:")) {
					tc = Integer.parseInt(arg.substring("threadcount:".length()));
				} else if (arg.startsWith("tc:")) {
					tc = Integer.parseInt(arg.substring("tc:".length()));
				} else {
					System.out.printf("Unknown switch: %s%n", arg);
				}
			} else {
				if (arg.equalsIgnoreCase("test") || arg.equalsIgnoreCase("countdown") || arg.equalsIgnoreCase("cd")) {
					doTest = true;
				} else if (arg.equalsIgnoreCase("producerconsumer") || arg.equalsIgnoreCase("pc")) {
					doProduceConsume = true;
				} else if (arg.equalsIgnoreCase("quicksort") || arg.equalsIgnoreCase("qs")) {
					doQuicksort = true;
				} else if (arg.equalsIgnoreCase("mapreduce") || arg.equalsIgnoreCase("mr")) {
					doMapReduce = true;
				} else if (arg.equalsIgnoreCase("virusscan") || arg.equalsIgnoreCase("vs")) {
					doVirusScan = true;
				} else if (arg.equalsIgnoreCase("all")) {
					doProduceConsume = true;
					doTest = true;
					doMapReduce = true;
					doQuicksort = true;
					doVirusScan = true;
				} else {
					System.out.printf("Unknown parameter: %s%n", arg);
				}
			}
		}
		if (!doTest && !doProduceConsume && !doQuicksort && !doMapReduce && !doVirusScan) {
			doTest = true;
		}
		if (doTest) {
			if (title.length() > 0) {
				title += " ";
			}
			title += "(Countdown Test)";
		}
		if (doProduceConsume) {
			if (title.length() > 0) {
				title += " ";
			}
			title += "(Producer+Consumer)";
		}
		if (doQuicksort) {
			if (title.length() > 0) {
				title += " ";
			}
			title += "(Quicksort)";
		}
		if (doMapReduce) {
			if (title.length() > 0) {
				title += " ";
			}
			title += "(MapReduce)";
		}
		if (doVirusScan) {
			if (title.length() > 0) {
				title += " ";
			}
			title += "(VirusScan)";
		}

		DefaultActorManager am = getManager();
		try {
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(DefaultActorManager.ACTOR_THREAD_COUNT, tc);
			am.initialize(options);
			if (doTest) {
				for (int i = 0; i < COMMON_ACTOR_COUNT; i++) {
					Actor a = am.createActor(TestActor.class, String.format("common%02d", i));
					if (a instanceof TestableActor) {
						TestableActor ta = (TestableActor) a;
						ta.setActorTest(this);
					}
					a.setCategory(TestActor.class.getSimpleName());
					getTestActors().put(a.getName(), a);
					// logger.trace("created: %s", a);
				}
				for (int i = 0; i < TEST_ACTOR_COUNT; i++) {
					Actor a = am.createActor(TestActor.class, String.format("actor%02d", i));
					if (a instanceof TestableActor) {
						TestableActor ta = (TestableActor) a;
						ta.setActorTest(this);
					}
					getTestActors().put(a.getName(), a);
					// logger.trace("created: %s", a);
				}
			}

			if (doProduceConsume) {
				for (int i = 0; i < PRODUCER_ACTOR_COUNT; i++) {
					Actor a = am.createActor(ProducerActor.class, String.format("producer%02d", i));
					getTestActors().put(a.getName(), a);
					// logger.trace("created: %s", a);
				}
			}

			if (doVirusScan) {
				VirusScanActor.createVirusScanActor(am);

				DefaultMessage dm = new DefaultMessage("init", "/downloads");
				am.send(dm, null, VirusScanActor.getCategoryName());
			}
			
			if (doMapReduce) {
				BigInteger[] values = new BigInteger[TEST_VALUE_COUNT];
				for (int i = 0; i < values.length; i++) {
					values[i] = new BigInteger(Long.toString((long) rand.nextInt(values.length)));
				}
				BigInteger[] targets = new BigInteger[Math.max(1, values.length / 10)];

				BigInteger res = new BigInteger("0");
				for (int i = 0; i < values.length; i++) {
					res = res.add(values[i].multiply(values[i]));
				}

				String id = MapReduceActor.nextId();
				logger.trace("**** MapReduce %s (expected=%d) start: %s", id, res, values);

				// start at least 5 actors
				MapReduceActor.createMapReduceActor(am, 10);
				MapReduceActor.createMapReduceActor(am, 10);
				MapReduceActor.createMapReduceActor(am, 10);
				MapReduceActor.createMapReduceActor(am, 10);
				MapReduceActor.createMapReduceActor(am, 10);
				// getTestActors().put(mra.getName(), mra);

				DefaultMessage dm = new DefaultMessage("init", new Object[] { values, targets,
						SumOfSquaresReducer.class });
				am.send(dm, null, MapReduceActor.getCategoryName());
			}

			for (String key : getTestActors().keySet()) {
				am.startActor(getTestActors().get(key));
			}

			for (int i = sc; i > 0; i--) {
				if (done) {
					break;
				}
				// see if idle a while
				long now = new Date().getTime();
				if (am.getActiveRunnableCount() == 0) {
					if (now - am.getLastDispatchTime() > MAX_IDLE_SECONDS * 1000
							&& now - am.getLastSendTime() > MAX_IDLE_SECONDS * 1000) {
						break;
					}
				}
				setStepCount(i);
				fireChangeListeners(new ChangeEvent(this));
				if (i < 10 || i % 10 == 0) {
					logger.trace("main waiting: %d...", i);
				}
				sleeper(1);
			}
			setStepCount(0);
			fireChangeListeners(new ChangeEvent(this));

			// logger.trace("main terminating");
			am.terminateAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
