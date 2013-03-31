package com.ibm.actor.test;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.ibm.actor.Actor;
import com.ibm.actor.ActorManager;
import com.ibm.actor.DefaultActorManager;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;

// Incomplete - ignore, quicksort not appropriate for Map/Reduce, need merge Sort instead
public class QuicksortActor extends TestableActor {

	protected Map<String, Integer> completionCounts = new Hashtable<String, Integer>();

	protected static int actorCount, idCount;

	protected class PendingMerge {
		public String id;
		public Comparable pivot;
		public List<Comparable> less;
		public List<Comparable> more;
		public List<Comparable> merge;
		volatile public boolean lessNeeded, moreNeeded;
		volatile public int count;

		public PendingMerge(List<Comparable> less, List<Comparable> more) {
			this.id = nextId();
			this.less = less;
			this.more = more;
			this.merge = new LinkedList<Comparable>();
			pendingMerges.put(id, this);
		}

		public PendingMerge() {
			this(new LinkedList<Comparable>(), new LinkedList<Comparable>());
		}

		public void merge() {
			merge.clear();
			merge.addAll(less);
			merge.add(pivot);
			merge.addAll(more);
		}
	}

	protected Map<String, PendingMerge> pendingMerges = new HashMap<String, PendingMerge>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void loopBody(Message m) {
		try {
			ActorManager manager = getManager();

			String subject = m.getSubject();
			if ("sort".equals(subject)) {
				doSort(m, manager);
			} else if ("merge".equals(subject)) {
				doMerge(m);
			} else if ("sortComplete".equals(subject)) {
				doSortComplete(m);
			} else if ("init".equals(subject)) {
				doInit(m, manager);
			} else {
				logger.warning("QuicksortActor:%s loopBody unknown subject: %s", getName(), subject);
			}
		} catch (Exception e) {
			logger.error("sort exception", e);
		}
	}

	private void doSort(Message m, ActorManager manager) {
		Object[] params = (Object[]) m.getData();
		Integer nest = params.length > 0 ? (Integer) params[0] : null;
		List<Comparable> values = params.length > 1 ? (List<Comparable>) params[1] : null;
		String id = params.length > 2 ? (String) params[2] : null;
		PendingMerge pm = params.length > 3 ? (PendingMerge) params[3] : null;
		if (values != null) {
			logger.info("receive sort values %s(%d): %s, %s", id, nest, values);
			doSortWorker(manager, nest, values, id, pm);
		} else if (pm != null) {
			logger.info("receive sort pm %s(%d): count=%d/%b/%b, %s<%s>%s", id, nest, pm.count, pm.lessNeeded,
					pm.moreNeeded, pm.less, pm.pivot, pm.more);
			if (pm.count > 0) {
				if (pm.lessNeeded) {
					logger.info("receive sort less %s(%d): %s, %s", id, nest, pm.less);
					pm.lessNeeded = false;
					pm.count--;
					doSortWorker(manager, nest + 1, pm.less, pm.id, pm);
				} else if (pm.moreNeeded) {
					logger.info("receive sort more %s(%d): %s, %s", id, nest, pm.more);
					pm.moreNeeded = false;
					pm.count--;
					doSortWorker(manager, nest + 1, pm.more, pm.id, pm);
				}
				if (pm.count == 0) {
					DefaultMessage dm = new DefaultMessage("merge", new Object[] { nest, pm, id });
					logger.info("send merge %s-%s(%d): %s<%s>%s", id, pm.id, nest, pm.less, pm.pivot, pm.more);
					recordSend(id, manager.send(dm, this, getCategoryName()));
				}
			}
		} else {
			logger.error("receive empty sort %s(%d)", id, nest);
		}
		recordComplete(id, 1);
	}

	protected void doSortWorker(ActorManager manager, int nest, List<Comparable> values, String id, PendingMerge pm) {
		sleep(1000);
		if (values.size() <= 1) {
			DefaultMessage dm = new DefaultMessage("sortComplete", new Object[] { nest, values, id });
			logger.info("send size complete %s(%d): %s", id, nest, values);
			recordSend(id, manager.send(dm, this, getCategoryName()));
		} else {
			pm = new PendingMerge();
			Comparable pivot = values.remove(0);
			pm.pivot = pivot;
			for (Comparable c : values) {
				if (c.compareTo(pivot) <= 0) {
					pm.less.add(c);
				} else {
					pm.more.add(c);
				}
			}
			if (pm.less.size() > 0) {
				pm.lessNeeded = true;
				pm.count++;
			}
			if (pm.more.size() > 0) {
				pm.moreNeeded = true;
				pm.count++;
			}
			logger.info("send pm sort %s(%d): count=%d, %s<%s>%s", id, nest, pm.count, pm.less, pm.pivot, pm.more);
			if (pm.count > 0) {
				if (pm.lessNeeded) {
					DefaultMessage lm = new DefaultMessage("sort", new Object[] { nest + 1, null, pm.id, pm });
					logger.info("send sort less %s: %s", pm.id, pm.less);
					recordSend(id, manager.send(lm, this, getCategoryName()));
				}
				if (pm.moreNeeded) {
					DefaultMessage mm = new DefaultMessage("sort", new Object[] { nest + 1, null, pm.id, pm });
					logger.info("send sort more %s: %s", pm.id, pm.more);
					recordSend(id, manager.send(mm, this, getCategoryName()));
				}
			}
		}
	}

	protected void doMerge(Message m) {
		sleep(1000);
		Object[] params = (Object[]) m.getData();
		Integer nest = params.length > 0 ? (Integer) params[0] : null;
		PendingMerge pm = params.length > 1 ? (PendingMerge) params[1] : null;
		String id = params.length > 2 ? (String) params[2] : null;
		pm.merge();
		logger.info("merge %s(%d): %s <= %s<%s>%s", pm.id, nest, pm.merge, pm.less, pm.pivot, pm.more);
		DefaultMessage dm = new DefaultMessage("sortComplete", new Object[] { nest, pm.merge, pm.id });
		logger.info("send sort complete %s(%d)", pm.id, nest);
		recordSend(pm.id, manager.send(dm, this, getCategoryName()));
		recordComplete(id, 1);
	}

	protected void doSortComplete(Message m) {
		Object[] params = (Object[]) m.getData();
		Integer nest = params.length > 0 ? (Integer) params[0] : null;
		List<Comparable> values = params.length > 1 ? (List<Comparable>) params[1] : null;
		String id = params.length > 2 ? (String) params[2] : null;

		logger.info("receive complete %s(%d): %s", id, nest, values);
		recordComplete(id, 1);
		if (isComplete(id)) {
			if (nest == 0) {
				logger.trace("**** Sort %s complete: %s", id, values);
			}
		}
	}

	protected void doInit(Message m, ActorManager manager) {
		Object[] params = (Object[]) m.getData();
		Integer max = !isEmpty(params) ? (Integer) params[0] : 50;
		// ensure enough actors exist
		int count = manager.getActorCount(QuicksortActor.class);
		for (int i = count; i < max; i++) {
			createQuicksortActor(manager);
		}
	}

	protected void resetPending(String id) {
		setPending(id, 0);
	}

	protected void setPending(String id, int count) {
		completionCounts.put(id, count);
		// logger.info("setPending %s: %d", id, count);
	}

	protected int recordSend(String id, int count) {
		int res = 0;
		if (!completionCounts.containsKey(id)) {
			resetPending(id);
		}
		completionCounts.put(id, res = completionCounts.get(id) + count);
		// logger.info("recordSend %s: %d", id, res);
		return res;
	}

	protected int recordComplete(String id, int count) {
		int res = 0;
		if (!completionCounts.containsKey(id)) {
			resetPending(id);
		}
		res = completionCounts.get(id) - count;
		if (res < 0) {
			logger.error("**** recordComplete %s count < 0: %d", id, res);
		} else {
			completionCounts.put(id, res);
		}
		// logger.info("recordComplete %s: %d", id, res);
		return res;
	}

	protected boolean isComplete(String id) {
		boolean res = completionCounts.containsKey(id) ? completionCounts.get(id) <= 0 : true;
		logger.info("isCommplete %s: %b", id, res);
		return res;
	}

	protected static QuicksortActor createQuicksortActor(ActorManager manager) {
		QuicksortActor a = (QuicksortActor) manager.createAndStartActor(QuicksortActor.class,
				QuicksortActor.class.getSimpleName() + actorCount++);
		a.setCategory(getCategoryName());
		return a;
	}

	Random rand = new Random();

	// test case

	public void run(ActorManager am, int count) {
		Actor a = createQuicksortActor(am);
	}

	public static String nextId() {
		return "id_" + idCount++;
	}

	public static String getCategoryName() {
		return "quicksortActor";
	}

	public static void main(String[] args) {
		try {
			ActorManager am = DefaultActorManager.getDefaultInstance();
			QuicksortActor qa = new QuicksortActor();
			qa.run(am, args.length > 0 ? Integer.parseInt(args[0]) : 1000);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}
}
