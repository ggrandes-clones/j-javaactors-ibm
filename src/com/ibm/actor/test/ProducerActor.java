package com.ibm.actor.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.actor.Actor;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;


/**
 * An actor that asks Consumer actors to produce items. 
 * 
 * @author BFEIGENB
 *
 */
public class ProducerActor extends TestableActor {

	@Override
	public void activate() {
		logger.trace("ProducerActor activate: %s", this);
		super.activate();
		// logger.trace("expected: %s", expected);
	}

	@Override
	public void deactivate() {
		logger.trace("ProducerActor deactivate: %s", this);
		if (messages.size() > 0) {
			logger.trace("expected: %s", expected);
			DefaultActorTest.dumpMessages(messages);
		}
		super.deactivate();
	}

	protected Map<String, Integer> expected = new ConcurrentHashMap<String, Integer>();

	@Override
	protected void loopBody(Message m) {
		String subject = m.getSubject();
		if ("produceN".equals(subject)) {
			Object[] input = (Object[]) m.getData();
			int count = (Integer) input[0];
			if (count > 0) {
				DefaultActorTest.sleeper(1); // this takes some time
				String type = (String) input[1];
				// logger.trace("ProducerActor:%s produceN %d x %s; pending=%d",
				// getName(), count, type, messages.size());
				logger.trace("ProducerActor:%s produceN %d x %s", getName(), count, type);
				// request the consumers to consume work (i.e., produce)
				Integer mcount = expected.get(type);
				if (mcount == null) {
					mcount = new Integer(0);
				}
				mcount += count;
				expected.put(type, mcount);

				DefaultMessage dm = new DefaultMessage("produce1", new Object[] { count, type });
				getManager().send(dm, this, this);
			}
		} else if ("produce1".equals(subject)) {
			Object[] input = (Object[]) m.getData();
			int count = (Integer) input[0];
			if (count > 0) {
				sleep(100); // take a little time
				String type = (String) input[1];
				// logger.trace("ProducerActor:%s produce1 %d x %s; pending=%d",
				// getName(), count, type, messages.size());
				logger.trace("ProducerActor:%s produce1 %d x %s", getName(), count, type);
				m = new DefaultMessage("construct", type);
				getManager().send(m, this, getConsumerCategory());

				m = new DefaultMessage("produce1", new Object[] { count - 1, type });
				getManager().send(m, this, this);
			}
		} else if ("constructionComplete".equals(subject)) {
			String type = (String) m.getData();
			// logger.trace("ProducerActor:%s constructionComplete %s; pending=%d",
			// getName(), type, messages.size());
			logger.trace("ProducerActor:%s constructionComplete %s from %s", getName(), type, m.getSource()
					.getName());
			Integer mcount = expected.get(type);
			if (mcount != null) {
				mcount--;
				expected.put(type, mcount);
			}
		} else if ("init".equals(subject)) {
			logger.trace("ProducerActor:%s init", getName());
			// create some consumers
			// 1 to 3 x consumers per producer
			for (int i = 0; i < DefaultActorTest.nextInt(3) + 1; i++) {
				Actor a = getManager().createAndStartActor(ConsumerActor.class,
						String.format("%s_consumer%02d", getName(), i));
				a.setCategory(getConsumerCategory());
				if (actorTest != null) {
					actorTest.getTestActors().put(a.getName(), a);
					// logger.trace("created: %s", a);
				}
			}
			// request myself create some work items
			for (int i = 0; i < DefaultActorTest.nextInt(10) + 1; i++) {
				m = new DefaultMessage("produceN", new Object[] { DefaultActorTest.nextInt(10) + 1,
						DefaultActorTest.getItemTypes()[DefaultActorTest.nextInt(DefaultActorTest.getItemTypes().length)] });
				getManager().send(m, this, this);
			}
		} else {
			logger.warning("ProducerActor:%s loopBody unknown subject: %s", getName(), subject);
		}
	}

	protected String getConsumerCategory() {
		return getName() + "_consumer";
	}
}