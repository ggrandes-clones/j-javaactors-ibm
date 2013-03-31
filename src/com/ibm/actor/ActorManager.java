package com.ibm.actor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * An ActorManager manages a set of actors. Managers route messages to actors
 * and assign threads to actors to process messages.
 * 
 * @author bfeigenb
 * @See Actor
 * @See Message
 * 
 */
public interface ActorManager {

	/**
	 * Create an actor.
	 * 
	 * @param clazz
	 *            class of the actor. must have no argument constructor
	 * @param name
	 *            name to assign to the actor; must be unique for this manager
	 * @return the actor
	 */
	Actor createActor(Class<? extends Actor> clazz, String name);

	/**
	 * Create an actor then start it.
	 * 
	 * @param clazz
	 *            class of the actor. must have no argument constructor
	 * @param name
	 *            name to assign to the actor; must be unique for this manager
	 * @return the actor
	 */
	Actor createAndStartActor(Class<? extends Actor> clazz, String name);

	/**
	 * Create an actor.
	 * 
	 * @param clazz
	 *            class of the actor. must have no argument constructor
	 * @param name
	 *            name to assign to the actor; must be unique for this manager
	 * @param manager
	 *            dependent parameters
	 * @return the actor
	 */
	Actor createActor(Class<? extends Actor> clazz, String name, Map<String, Object> options);

	/**
	 * Create an actor then start it.
	 * 
	 * @param clazz
	 *            class of the actor. must have no argument constructor
	 * @param name
	 *            name to assign to the actor; must be unique for this manager
	 * @param manager
	 *            dependent parameters
	 * @return the actor
	 */
	Actor createAndStartActor(Class<? extends Actor> clazz, String name, Map<String, Object> options);

	/**
	 * Start an actor.
	 * 
	 * @param a
	 *            the actor
	 */
	void startActor(Actor a);

	/**
	 * Detach an actor. This actor is no longer managed by this manger and will
	 * not receive and more messages.
	 * 
	 * @param a
	 *            the actor
	 */
	void detachActor(Actor actor);

	/**
	 * Send a message to an actor. The message will be processed at a later time.
	 * 
	 * @param message the message
	 * @param from the source actor; may be null
	 * @param to the target actor
	 * @return number of actors that accepted the send
	 */
	int send(Message message, Actor from, Actor to);

	/**
	 * Send a message to a set of actors. The message will be processed at a later time.
	 * 
	 * @param message the message
	 * @param from the source actor; may be null
	 * @param to the target actors
	 * @return number of actors that accepted the send
	 */
	int send(Message message, Actor from, Actor[] to);

	/**
	 * Send a message to a set of actors. The message will be processed at a later time.
	 * 
	 * @param message the message
	 * @param from the source actor; may be null
	 * @param to the target actors
	 * @return number of actors that accepted the send
	 */
	int send(Message message, Actor from, Collection<Actor> to);

	/**
	 * Send a message to an actor in the category. The message will be processed at a later time.
	 * 
	 * @param message the message
	 * @param from the source actor; may be null
	 * @param category category of the target actor
	 * @return number of actors that accepted the send
	 */
	int send(Message message, Actor from, String category);

	/**
	 * Send a message to all actors. The message will be processed at a later time.
	 * 
	 * @param message the message
	 * @param from the source actor; may be null
	 * @return number of actors that accepted the send
	 */
	int broadcast(Message message, Actor from);

	/**
	 * Get the categories that currently have actors. 
	 * 
	 * @return category list
	 */
	Set<String> getCategories();

	/**
	 * Initialize this manager.  Should only be done once.
	 */
	void initialize();

	/**
	 * Initialize this manager.  Should only be done once.
	 * 
	 * @param options manger dependent parameters
	 */
	void initialize(Map<String, Object> options);

	/**
	 * Terminate this manager. Wait until all processing threads have stopped.
	 */
	void terminateAndWait();

	/**
	 * Terminate this manager. Do not wait until all processing threads have stopped.
	 */
	void terminate();
	
	/** 
	 * Get the count of actors.
	 * 
	 * @param type actor type; all if null
	 * 
	 * @return count of actors
	 */
	int getActorCount(Class type);
}
