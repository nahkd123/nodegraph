package io.github.nahkd123.nodegraph.graph;

import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;

public interface EvaluationRound<E> {
	E getEnvironment();

	/**
	 * <p>
	 * Issue a new evaluation request.
	 * </p>
	 * 
	 * @param instance The node instance.
	 * @return The output getter to obtain the results from the node. Usually this
	 *         return value is ignored in most applications, as there may be a
	 *         special node to gather values and apply them to environment.
	 */
	NodeOutputGetter eval(NodeInstance<?, E> instance);

	@FunctionalInterface
	public static interface NodeInputSupplier {
		<V> V supply(InputSocket<V> socket);
	}

	@FunctionalInterface
	public static interface NodeOutputGetter {
		<V> V get(OutputSocket<V> socket);
	}
}
