package io.github.nahkd123.nodegraph.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;

/**
 * <p>
 * A node graph is a collection of node and connection between node's sockets. A
 * {@link NodeGraph} object only store definitions; all the processing and
 * internal states are handled by {@link EvaluationRound}.
 * </p>
 * 
 * @param <E> Type of environment object.
 */
public class NodeGraph<E> {
	Map<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>> dstToSrc = new HashMap<>();
	Set<NodeInstance<?, E>> instances = new HashSet<>();

	/**
	 * <p>
	 * Add a new node instance of this graph.
	 * </p>
	 * 
	 * @param <S>      Type of internal node states.
	 * @param instance The node instance.
	 * @return The same node instance from parameter, for chaining purpose.
	 */
	public <S> NodeInstance<S, E> addInstance(NodeInstance<S, E> instance) {
		instances.add(instance);
		return instance;
	}

	public <S> boolean removeInstance(NodeInstance<S, E> instance) {
		if (instances.remove(instance)) {
			Iterator<Entry<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>>> iter = dstToSrc.entrySet().iterator();

			while (iter.hasNext()) {
				Entry<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>> entry = iter.next();

				if (entry.getKey().node() == instance || entry.getValue().node() == instance) {
					iter.remove();
					continue;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public <V> boolean connect(NodeSocketRef<?, E, V> from, NodeSocketRef<?, E, V> to) {
		if (!(from.socket() instanceof OutputSocket))
			throw new IllegalArgumentException("outgoing socket (from) is not output");
		if (!(to.socket() instanceof InputSocket))
			throw new IllegalArgumentException("incoming socket (to) is not input");
		return dstToSrc.putIfAbsent(to, from) == null;
	}

	public <V> boolean connect(NodeInstance<?, E> fromNode, OutputSocket<V> fromSocket, NodeInstance<?, E> toNode, InputSocket<V> toSocket) {
		return connect(new NodeSocketRef<>(fromNode, fromSocket), new NodeSocketRef<>(toNode, toSocket));
	}

	public <V> boolean disconnect(NodeSocketRef<?, E, V> from, NodeSocketRef<?, E, V> to) {
		if (!(from.socket() instanceof OutputSocket))
			throw new IllegalArgumentException("outgoing socket (from) is not output");
		if (!(to.socket() instanceof InputSocket))
			throw new IllegalArgumentException("incoming socket (to) is not input");
		return dstToSrc.remove(to, from);
	}

	public <V> boolean disconnect(NodeInstance<?, E> fromNode, OutputSocket<V> fromSocket, NodeInstance<?, E> toNode, InputSocket<V> toSocket) {
		return disconnect(new NodeSocketRef<>(fromNode, fromSocket), new NodeSocketRef<>(toNode, toSocket));
	}

	/**
	 * <p>
	 * Disconnect socket from a single source/input (if any).
	 * </p>
	 * 
	 * @param to The socket that may have a connection coming to it to remove.
	 * @return Whether the connection is removed successfully.
	 */
	public boolean disconnectFromSource(NodeSocketRef<?, E, ?> to) {
		return dstToSrc.remove(to) != null;
	}

	/**
	 * <p>
	 * Disconnect socket from connecting to one or more sockets.
	 * </p>
	 * 
	 * @param from The socket that might be connected to multiple sockets.
	 * @return The number of connections removed.
	 */
	public int disconnectFromDestinations(NodeSocketRef<?, E, ?> from) {
		Iterator<Entry<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>>> iter = dstToSrc.entrySet().iterator();
		int counter = 0;

		while (iter.hasNext()) {
			Entry<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>> entry = iter.next();

			if (entry.getValue().equals(from)) {
				iter.remove();
				counter++;
			}
		}

		return counter;
	}

	public Set<NodeInstance<?, E>> getInstances() { return Collections.unmodifiableSet(instances); }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void forEachConnection(ForEachConnectionCallback<E> callback) {
		dstToSrc.forEach((to, from) -> {
			((ForEachConnectionCallback) callback).callback(from, to);
		});
	}

	public Set<Map.Entry<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>>> getConnections() {
		Set<Map.Entry<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>>> out = new HashSet<>();
		forEachConnection(new ForEachConnectionCallback<>() {
			@Override
			public <V> void callback(NodeSocketRef<?, E, V> from, NodeSocketRef<?, E, V> to) {
				out.add(Map.entry(from, to));
			}
		});
		return out;
	}

	/**
	 * <p>
	 * Create a new evaluation round. Each round have its own set of internal node
	 * states. You can reuse the same round in one processing batch (like drawing a
	 * stroke from multiple points for example), but you might not want to reuse it
	 * indefinitely, as the node instances in this domain may be altered by user.
	 * </p>
	 * <p>
	 * In other words, you can reuse the same round if the node graph is not
	 * changed, and you have to create a new one if something changed.
	 * </p>
	 * 
	 * @return A new evaluation round.
	 */
	public EvaluationRound<E> newEvalRound(E environment) {
		return new EvaluationRoundImpl<>(this, environment);
	}

	/**
	 * <p>
	 * Make a copy of this node graph.
	 * </p>
	 */
	public NodeGraph<E> copy() {
		NodeGraph<E> newGraph = new NodeGraph<>();
		Map<NodeInstance<?, E>, NodeInstance<?, E>> currToNew = new HashMap<>();
		instances.forEach(i -> currToNew.put(i, newGraph.addInstance(i.copy())));

		forEachConnection(new ForEachConnectionCallback<>() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public <V> void callback(NodeSocketRef<?, E, V> from, NodeSocketRef<?, E, V> to) {
				NodeInstance<?, E> fromNode = currToNew.get(from.node());
				OutputSocket<?> fromSocket = (OutputSocket<?>) from.socket();
				NodeInstance<?, E> toNode = currToNew.get(to.node());
				InputSocket<?> toSocket = (InputSocket<?>) to.socket();
				((NodeGraph) newGraph).connect(fromNode, fromSocket, toNode, toSocket);
			}
		});

		return newGraph;
	}

	@FunctionalInterface
	public static interface ForEachConnectionCallback<E> {
		<V> void callback(NodeSocketRef<?, E, V> from, NodeSocketRef<?, E, V> to);
	}
}
