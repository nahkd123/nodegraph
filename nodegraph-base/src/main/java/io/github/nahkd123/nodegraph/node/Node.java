package io.github.nahkd123.nodegraph.node;

import java.util.Collection;
import java.util.List;

import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;
import io.github.nahkd123.nodegraph.socket.Socket;

/**
 * <p>
 * {@link Node} object represent a type of node, which then can be created into
 * node instances that holds the internal states.
 * </p>
 * 
 * @param <S> Type of the object that hold internal node states.
 * @param <E> Type of environment that the node can interact while processing.
 */
public interface Node<S, E> {
	/**
	 * <p>
	 * Get a list of declared sockets. The sockets will be displayed in GUI based on
	 * its order in this list.
	 * </p>
	 * 
	 * @return An ordered list of sockets.
	 */
	List<Socket<?>> getSockets();

	@SuppressWarnings("unchecked")
	default Collection<InputSocket<?>> getInputSockets() {
		return (List<InputSocket<?>>) (Object) getSockets().stream()
			.map(s -> s instanceof InputSocket<?> input ? input : null)
			.filter(s -> s != null)
			.toList();
	}

	@SuppressWarnings("unchecked")
	default Collection<OutputSocket<?>> getOutputSockets() {
		return (List<OutputSocket<?>>) (Object) getSockets().stream()
			.map(s -> s instanceof OutputSocket<?> input ? input : null)
			.filter(s -> s != null)
			.toList();
	}

	/**
	 * <p>
	 * Initialize a new node. The method returns the object that holds the internal
	 * states for processing.
	 * </p>
	 */
	S initialize();

	/**
	 * <p>
	 * Check whether the result of this node should be cached to avoid
	 * recomputation. If true, {@link #process(NodeProcessContext)} will only be
	 * called once every evaluation. If false, {@link #process(NodeProcessContext)}
	 * will be called on every requests (for example, connecting the output to 2
	 * different inputs of another node will call
	 * {@link #process(NodeProcessContext)} twice).
	 * </p>
	 * 
	 * @return Whether the result should be cached.
	 */
	default boolean shouldCache() {
		return true;
	}

	/**
	 * <p>
	 * Process this node. During processing, to indicate the node failed while
	 * processing, just throw an exception.
	 * </p>
	 * 
	 * @param context The context that allows this node to access its internal
	 *                states, environment, inputs and return outputs back to
	 *                requester.
	 */
	void process(NodeProcessContext<S, E> context);
}
