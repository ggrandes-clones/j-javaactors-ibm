package com.ibm.actor.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.actor.Actor;
import com.ibm.actor.ActorManager;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;

/**
 * An actor that can do calculations via a suppled MapReducer. 
 * 
 * @author BFEIGENB
 *
 */
public class MapReduceActor extends TestableActor {

	protected int partitionSize = 10;

	public int getPartitionSize() {
		return partitionSize;
	}

	public void setPartitionSize(int partitionSize) {
		this.partitionSize = partitionSize;
	}

	public static final int MAX_ACTOR_COUNT = 25;

	// carries state as a message parameter.  
	protected static class MapReduceParameters implements Comparable<MapReduceParameters> {
		protected static int idCount;
		protected int id = idCount++;;
		public Object[] values;
		public Integer start;
		public Integer end;
		public Object[] target;
		public Integer posn;
		// depth && targetDepth currently unused
		// public Integer depth;
		// public Integer targetDepth;
		public MapReduceer mr;
		public Actor sender;
		protected String set;
		public static Map<String, Map<Integer, MapReduceParameters>> pendingResponses = new ConcurrentHashMap<String, Map<Integer, MapReduceParameters>>();

		public MapReduceParameters(String set, Object[] values, Object[] target, MapReduceer mr, Actor sender) {
			this(set, values, 0, values.length - 1, target, 0, 0, 5, mr, sender);
		}

		public MapReduceParameters(String set, Object[] values, Object[] target, Integer depth, Integer targetDepth,
				MapReduceer mr, Actor sender) {
			this(set, values, 0, values.length - 1, target, 0, depth, targetDepth, mr, sender);
		}

		public MapReduceParameters(String set, Object[] values, Integer start, Integer end, Object[] target, int posn,
				Integer depth, Integer targetDepth, MapReduceer mr, Actor sender) {
			this.set = set;
			this.values = values;
			this.start = start;
			this.end = end;
			this.target = target;
			this.posn = posn;
			// this.depth = depth;
			// this.targetDepth = targetDepth;
			this.mr = mr;
			this.sender = sender;
			// rememberReference();
		}

		public MapReduceParameters(MapReduceParameters p) {
			this.set = p.set;
			this.values = p.values;
			this.start = p.start;
			this.end = p.end;
			this.target = p.target;
			this.posn = p.posn;
			// this.depth = p.depth;
			// this.targetDepth = p.targetDepth;
			this.mr = p.mr;
			this.sender = p.sender;
			rememberReference();
		}

		private void rememberReference() {
			Map<Integer, MapReduceParameters> m = pendingResponses.get(set);
			if (m == null) {
				m = new ConcurrentHashMap<Integer, MapReduceParameters>();
				pendingResponses.put(set, m);
			}
			m.put(id, this);
		}

		public void complete() {
			Map<Integer, MapReduceParameters> m = pendingResponses.get(set);
			if (m != null) {
				m.remove(id);
				if (m.isEmpty()) {
					pendingResponses.remove(set);
				}
			}
		}

		public boolean isSetComplete() {
			Map<Integer, MapReduceParameters> m = pendingResponses.get(set);
			//logger.info("isSetComplete: %d", m != null ? m.size() : 0);
			return m == null || m.isEmpty();
		}

		@Override
		public int compareTo(MapReduceParameters o) {
			return this.id - o.id;
		}

		@Override
		public String toString() {
			return getClass().getName() + "[id=" + id + ", values.length=" + (values != null ? values.length : 0)
					+ ", start=" + start + ", end=" + end + ", target.length=" + (target != null ? target.length : 0)
					+ ", posn=" + posn + ", sender=" + sender + ", set=" + set + "]";
		}
	}

	@Override
	protected void loopBody(Message m) {
		ActorManager manager = getManager();
		String subject = m.getSubject();
		if ("mapReduce".equals(subject)) {
			try {
				MapReduceParameters p = (MapReduceParameters) m.getData();
				int index = 0;
				int count = (p.end - p.start + 1 + partitionSize - 1) / partitionSize;
				logger.info("mapReduce i=%d, c=%d, s=%d: %s", index, count, partitionSize, p);
				sleep(1000);
				// split up into partition size chunks
				while (p.end - p.start + 1 >= partitionSize) {
					MapReduceParameters xp = new MapReduceParameters(p);
					xp.end = xp.start + partitionSize - 1;
					DefaultMessage lm = new DefaultMessage("createPartition", new Object[] { xp, index, count });
					manager.send(lm, this, getCategory());
					p.start += partitionSize;
					index++;
				}
				if (p.end - p.start + 1 > 0) {
					DefaultMessage lm = new DefaultMessage("createPartition", new Object[] { p, index, count });
					manager.send(lm, this, getCategory());
				}
			} catch (Exception e) {
				triageException("mapFailed", m, e);
			}
		} else if ("createPartition".equals(subject)) {
			try {
				Object[] oa = (Object[]) m.getData();
				MapReduceParameters p = (MapReduceParameters) oa[0];
				int index = (Integer) oa[1];
				int count = (Integer) oa[2];
				logger.info("createPartition i=%d, c=%d: %s", index, count, p);
				sleep(500);
				createMapReduceActor(this);
				DefaultMessage lm = new DefaultMessage("mapWorker", new Object[] { p, index, count });
				manager.send(lm, this, getCategory());
			} catch (Exception e) {
				triageException("createPartitionFailed", m, e);
			}
		} else if ("mapWorker".equals(subject)) {
			try {
				Object[] oa = (Object[]) m.getData();
				MapReduceParameters p = (MapReduceParameters) oa[0];
				int index = (Integer) oa[1];
				int count = (Integer) oa[2];
				logger.info("mapWorker %d: %s", index, p);
				sleep(100);
				p.mr.map(p.values, p.start, p.end);
				DefaultMessage rm = new DefaultMessage("mapResponse", new Object[] { p, index, count });
				manager.send(rm, this, getCategoryName());
			} catch (Exception e) {
				triageException("mapWorkerFailed", m, e);
			}
		} else if ("mapResponse".equals(subject)) {
			try {
				Object[] oa = (Object[]) m.getData();
				MapReduceParameters p = (MapReduceParameters) oa[0];
				int index = (Integer) oa[1];
				int count = (Integer) oa[2];
				logger.info("mapResponse i=%d, c=%s: %s", index, count, p);
				sleep(100);
				p.complete();
				// if (p.isSetComplete()) {
				DefaultMessage rm = new DefaultMessage("reduce", new Object[] { p, index, count });
				manager.send(rm, this, getCategoryName());
				// }
			} catch (Exception e) {
				triageException("mapResponseFailed", m, e);
			}
		} else if ("reduce".equals(subject)) {
			try {
				MapReduceParameters p = null;
				int index = 0, count = 0;
				Object o = m.getData();
				if (o instanceof MapReduceParameters) {
					p = (MapReduceParameters) o;
				} else {
					Object[] oa = (Object[]) o;
					p = (MapReduceParameters) oa[0];
					index = (Integer) oa[1];
					count = (Integer) oa[2];
				}
				logger.info("reduce i=%d: %s", index, p);
				sleep(100);
				if (p.end - p.start + 1 > 0) {
					createMapReduceActor(this);
					MapReduceParameters xp = new MapReduceParameters(p);
					DefaultMessage lm = new DefaultMessage("reduceWorker", new Object[] { xp, index, count });
					manager.send(lm, this, getCategory());
				}
			} catch (Exception e) {
				triageException("reduceFailed", m, e);
			}
		} else if ("reduceWorker".equals(subject)) {
			try {
				Object[] oa = (Object[]) m.getData();
				MapReduceParameters p = (MapReduceParameters) oa[0];
				int index = (Integer) oa[1];
				int count = (Integer) oa[2];
				logger.info("reduceWorker i=%d, c=%d: %s", index, count, p);
				sleep(100);
				if (index >= 0) {
					p.mr.reduce(p.values, p.start, p.end, p.target, index);
					DefaultMessage rm = new DefaultMessage("reduceResponse", new Object[] { p, index, count });
					manager.send(rm, this, getCategory());
				} else {
					Object[] res = new Object[1];
					p.mr.reduce(p.target, 0, count - 1, res, 0);
					DefaultMessage rm = new DefaultMessage("done", new Object[] { p, res[0] });
					manager.send(rm, this, getCategory());
				}
			} catch (Exception e) {
				triageException("reduceWorkerFailed", m, e);
			}
		} else if ("reduceResponse".equals(subject)) {
			try {
				Object[] oa = (Object[]) m.getData();
				MapReduceParameters p = (MapReduceParameters) oa[0];
				int index = (Integer) oa[1];
				int count = (Integer) oa[2];
				logger.info("reduceResponse i=%d, c=%d: %s", index, count, p);
				sleep(100);
				p.complete();
				if (p.isSetComplete()) {
					if (count > 0) {
						createMapReduceActor(this);
						MapReduceParameters xp = new MapReduceParameters(p);
						DefaultMessage lm = new DefaultMessage("reduceWorker", new Object[] { xp, -1, count });
						manager.send(lm, this, getCategory());
					}
				}
			} catch (Exception e) {
				triageException("mapResponseFailed", m, e);
			}
		} else if ("done".equals(subject)) {
			try {
				Object[] oa = (Object[]) m.getData();
				MapReduceParameters p = (MapReduceParameters) oa[0];
				Object res = oa[1];
				logger.info("done %s: %s", res, p);
				sleep(100);
				logger.trace("**** mapReduce done with result %s", res);
			} catch (Exception e) {
				triageException("mapResponseFailed", m, e);
			}
		} else if ("init".equals(subject)) {
			try {
				Object[] params = (Object[]) m.getData();
				if (params != null) {
					Object[] values = (Object[]) params[0];
					Object[] targets = (Object[]) params[1];
					Class clazz = (Class) params[2];
					logger.info("init %d, %d, %s ", values.length, targets.length, clazz);
					MapReduceer mr = (MapReduceer) clazz.newInstance();

					sleep(2 * 1000);
					MapReduceParameters p = new MapReduceParameters("mrSet_" + setCount++, values, targets, mr, this);
					DefaultMessage rm = new DefaultMessage("mapReduce", p);
					manager.send(rm, this, getCategoryName());
				}
			} catch (Exception e) {
				triageException("initFailed", m, e);
			}
		} else {
			logger.warning("**** MapReduceActor:%s loopBody unexpected subject: %s", getName(), subject);
		}
	}

	protected void triageException(String subject, Message m, Exception e) {
		logger.error("triageException %s: %s", subject, m, e);
		Actor target = m.getSource();
		DefaultMessage dm = new DefaultMessage(subject, new Object[] { m.getData(), e });
		if (m.getData() instanceof MapReduceParameters) {
			MapReduceParameters p = (MapReduceParameters) m.getData();
			if (p != null && p.sender != null) {
				target = p.sender;
			}
		}
		getManager().send(dm, this, target);
	}

	public static MapReduceActor createMapReduceActor(MapReduceActor mra) {
		ActorManager manager = mra.getManager();
		return (MapReduceActor) (manager.getActorCount(MapReduceActor.class) < MAX_ACTOR_COUNT ? createMapReduceActor(
				manager, mra.getPartitionSize()) : mra);
	}

	public static MapReduceActor createMapReduceActor(ActorManager manager, int partitionSize) {
		MapReduceActor res = (MapReduceActor) manager.createAndStartActor(MapReduceActor.class, getActorName());
		res.setCategory(getCategoryName());
		res.setPartitionSize(partitionSize);
		return res;
	}

	public static String getCategoryName() {
		return "mapReduceActor";
	}

	protected static int setCount;

	protected static int idCount;

	public static String nextId() {
		return "mapReduce_" + idCount++;
	}

	protected static int actorCount;

	protected static String getActorName() {
		return MapReduceActor.class.getSimpleName() + '_' + actorCount++;
	}

}
