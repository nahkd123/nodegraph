package io.github.nahkd123.nodegraph.node;

import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;

/**
 * <p>
 * The context that will be provided to node when processing.
 * </p>
 * 
 * @param <S> Type of the object that hold internal node states.
 * @param <E> Type of environment that the node can interact while processing.
 */
public interface NodeProcessContext<S, E> {
	/**
	 * <p>
	 * Get the internal states of current node instance.
	 * </p>
	 * 
	 * @return The node internal states.
	 */
	S getStates();

	/**
	 * <p>
	 * Get the environment, allowing the node to get or modify data in environment.
	 * </p>
	 * 
	 * @return The environment.
	 */
	E getEnvironment();

	/**
	 * <p>
	 * Get a value from input socket. Value is obtained based on this priority
	 * (whichever found first will be used):
	 * </p>
	 * <ol>
	 * <li>Input from connected node;</li>
	 * <li>User-controlled input (eg: from text box);</li>
	 * <li>Default value declared in {@link InputSocket}.</li>
	 * </ol>
	 * 
	 * @param <V>    Type of value.
	 * @param socket The input socket.
	 * @return The value.
	 */
	<V> V get(InputSocket<V> socket);

	/**
	 * <p>
	 * Set a value to output socket. All declared output sockets must be populated,
	 * otherwise a {@link IllegalStateException} will be thrown after processing is
	 * completed.
	 * </p>
	 * 
	 * @param <V>    Type of value.
	 * @param socket The output socket.
	 * @param value  The value.
	 */
	<V> void set(OutputSocket<V> socket, V value);
}
