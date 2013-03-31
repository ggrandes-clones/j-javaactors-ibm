package com.ibm.actor.test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;

/**
 * An actor that can execute a command as a method on a supplied class.
 *  
 * @author BFEIGENB
 *
 */
public class CommandActor extends TestableActor {

	@Override
	protected void loopBody(Message m) {
		String subject = m.getSubject();
		if ("execute".equals(subject)) {
			excuteMethod(m, false);
		} else if ("executeStatic".equals(subject)) {
			excuteMethod(m, true);
		} else if ("init".equals(subject)) {
			// nothing to do
		} else {
			logger.warning("CommandActor:%s loopBody unknown subject: %s",
					getName(), subject);
		}
	}

	private void excuteMethod(Message m, boolean fstatic) {
		Object res = null;
		Object id = null;
		try {
			Object[] params = (Object[]) m.getData();
			id = params[0];
			String className = (String) params[1];
			params = params.length > 2 ? (Object[]) params[2] : null;
			Class<?> clazz = Class.forName(className);
			Method method = clazz.getMethod(fstatic ? "executeStatic"
					: "execute", new Class[] { Object.class });
			if (Modifier.isStatic(method.getModifiers()) == fstatic) {
				Object target = fstatic ? null : clazz.newInstance();
				res = method.invoke(target, params);
			}
		} catch (Exception e) {
			res = e;
		}

		DefaultMessage dm = new DefaultMessage("executeComplete", new Object[] {
				id, res });
		getManager().send(dm, this, m.getSource());
	}
}
