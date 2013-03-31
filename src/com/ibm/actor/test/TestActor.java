package com.ibm.actor.test;

import java.util.Date;

import com.ibm.actor.Actor;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;

/**
 * An actor that sends messages while counting down a send count. 
 * 
 * @author BFEIGENB
 *
 */
public class TestActor extends TestableActor {

	@Override
	public void activate() {
		logger.trace("TestActor activate: %s", this);
		super.activate();
	}

	@Override
	public void deactivate() {
		logger.trace("TestActor deactivate: %s", this);
		super.deactivate();
	}

	@Override
	protected void runBody() {
		// logger.trace("TestActor:%s runBody: %s", getName(), this);
		DefaultActorTest.sleeper(1);
		DefaultMessage m = new DefaultMessage("init", 8);
		getManager().send(m, null, this);
	}

	@Override
	protected void loopBody(Message m) {
		// logger.trace("TestActor:%s loopBody %s: %s", getName(), m, this);
		DefaultActorTest.sleeper(1);
		String subject = m.getSubject();
		if ("repeat".equals(subject)) {
			int count = (Integer) m.getData();
			logger.trace("TestActor:%s repeat(%d) %s: %s", getName(), count, m,
					this);
			if (count > 0) {
				m = new DefaultMessage("repeat", count - 1);
				// logger.trace("TestActor loopBody send %s: %s", m, this);
				String toName = "actor"
						+ DefaultActorTest
								.nextInt(DefaultActorTest.TEST_ACTOR_COUNT);
				Actor to = actorTest.getTestActors().get(toName);
				if (to != null) {
					getManager().send(m, this, to);
				} else {
					logger.warning("repeat:%s to is null: %s", getName(),
							toName);
				}
			}
		} else if ("init".equals(subject)) {
			int count = (Integer) m.getData();
			count = DefaultActorTest.nextInt(count) + 1;
			logger.trace("TestActor:%s init(%d): %s", getName(), count, this);
			for (int i = 0; i < count; i++) {
				DefaultActorTest.sleeper(1);
				m = new DefaultMessage("repeat", count);
				// logger.trace("TestActor runBody send %s: %s", m, this);
				String toName = "actor"
						+ DefaultActorTest
								.nextInt(DefaultActorTest.TEST_ACTOR_COUNT);
				Actor to = actorTest.getTestActors().get(toName);
				if (to != null) {
					getManager().send(m, this, to);
				} else {
					logger.warning("init:%s to is null: %s", getName(), toName);
				}
				DefaultMessage dm = new DefaultMessage("repeat", count);
				dm.setDelayUntil(new Date().getTime()
						+ (DefaultActorTest.nextInt(5) + 1) * 1000);
				getManager().send(dm, this, this.getClass().getSimpleName());
			}
		} else {
			logger.warning("TestActor:%s loopBody unknown subject: %s",
					getName(), subject);
		}
	}

}