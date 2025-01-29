package io.github.nahkd123.nodegraph.graph;

import java.util.HashMap;
import java.util.Map;

import io.github.nahkd123.nodegraph.node.NodeProcessContext;
import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;
import io.github.nahkd123.nodegraph.socket.Socket;

class EvaluationRoundImpl<E> implements EvaluationRound<E> {
	private NodeGraph<E> domain;
	private E environment;
	private Map<NodeInstance<?, E>, ?> states = new HashMap<>();

	EvaluationRoundImpl(NodeGraph<E> domain, E environment) {
		this.domain = domain;
		this.environment = environment;
	}

	@Override
	public E getEnvironment() { return environment; }

	@Override
	public NodeOutputGetter eval(NodeInstance<?, E> instance) {
		Map<NodeInstance<?, E>, Map<Socket<?>, ?>> globalCache = new HashMap<NodeInstance<?, E>, Map<Socket<?>, ?>>();
		NodeInputSupplier input = inputOf(instance, globalCache);
		Map<Socket<?>, ?> result = eval(instance, input, globalCache);
		return new NodeOutputGetter() {
			@SuppressWarnings("unchecked")
			@Override
			public <V> V get(OutputSocket<V> socket) {
				return (V) result.get(socket);
			}
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <S> Map<Socket<?>, ?> eval(NodeInstance<S, E> instance, NodeInputSupplier inputSupplier, Map<NodeInstance<?, E>, Map<Socket<?>, ?>> globalCache) {
		Map<Socket<?>, ?> nodeResult;

		if (instance.getNode().shouldCache()) {
			nodeResult = globalCache.get(instance);
			if (nodeResult != null) return nodeResult;
		}

		globalCache.put(instance, nodeResult = new HashMap<>());
		Map<Socket<?>, ?> nodeResult2 = nodeResult;

		S states = (S) this.states.get(instance);
		if (states == null) ((Map) this.states).put(instance, states = instance.getNode().initialize());
		NodeProcessContextImpl<S> ctx = new NodeProcessContextImpl<S>(states, inputSupplier, new NodeOutputConsumer() {
			@Override
			public <V> void consume(Socket<V> socket, V value) {
				((Map) nodeResult2).put(socket, value);
			}
		});

		instance.getNode().process(ctx);
		return nodeResult;
	}

	private <S> NodeInputSupplier inputOf(NodeInstance<S, E> instance, Map<NodeInstance<?, E>, Map<Socket<?>, ?>> globalCache) {
		return new NodeInputSupplier() {
			@SuppressWarnings("unchecked")
			@Override
			public <V> V supply(InputSocket<V> socket) {
				NodeSocketRef<?, E, ?> src = domain.dstToSrc.get(new NodeSocketRef<>(instance, socket));
				if (src == null) return instance.getInitialValue(socket);
				Map<Socket<?>, ?> nodeResult = eval(src.node(), inputOf(src.node(), globalCache), globalCache);
				return (V) nodeResult.get(src.socket());
			}
		};
	}

	@FunctionalInterface
	private interface NodeOutputConsumer {
		<V> void consume(Socket<V> socket, V value);
	}

	private class NodeProcessContextImpl<S> implements NodeProcessContext<S, E> {
		private S states;
		private NodeInputSupplier supplier;
		private NodeOutputConsumer consumer;

		public NodeProcessContextImpl(S states, NodeInputSupplier supplier, NodeOutputConsumer consumer) {
			this.states = states;
			this.supplier = supplier;
			this.consumer = consumer;
		}

		@Override
		public S getStates() { return states; }

		@Override
		public E getEnvironment() { return environment; }

		@Override
		public <V> V get(InputSocket<V> socket) {
			return supplier.supply(socket);
		}

		@Override
		public <V> void set(OutputSocket<V> socket, V value) {
			consumer.consume(socket, value);
		}
	}
}
