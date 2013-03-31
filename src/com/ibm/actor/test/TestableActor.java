package com.ibm.actor.test;

import com.ibm.actor.AbstractActor;
import com.ibm.actor.DefaultMessage;

/**
 * An Actor superclass that provided access to a runtime helper. 
 * 
 * @author BFEIGENB
 *
 */
abstract public class TestableActor extends AbstractActor {
	DefaultActorTest actorTest;

	public DefaultActorTest getActorTest() {
		return actorTest;
	}

	public void setActorTest(DefaultActorTest actorTest) {
		this.actorTest = actorTest;
	}

	public TestableActor() {
	}

}
