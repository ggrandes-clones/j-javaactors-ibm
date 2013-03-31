package com.ibm.actor;

/**
 * An actor with exception trapping. 
 * 
 * @author BFEIGENB
 *
 */
abstract public class SafeActor extends AbstractActor {

	@Override
	protected void loopBody(Message m) {
		try {
			logger.trace("SafeActor loopBody: %s", m);
			doBody((DefaultMessage) m);
		} catch (Exception e) {
			logger.error("SafeActor: exception", e);
		}
	}

	@Override
	protected void runBody() {
		// by default, nothing to do
	}

	/**
	 * Override to define message reception behavior. 
	 * 
	 * @param m
	 * @throws Exception
	 */
	abstract protected void doBody(DefaultMessage m) throws Exception;

}