package io.github.nahkd123.nodegraph.dfucodec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import io.github.nahkd123.nodegraph.graph.EvaluationRound;
import io.github.nahkd123.nodegraph.graph.NodeGraph;
import io.github.nahkd123.nodegraph.graph.NodeInstance;
import io.github.nahkd123.nodegraph.node.Node;
import io.github.nahkd123.nodegraph.node.NodeProcessContext;
import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;
import io.github.nahkd123.nodegraph.socket.Socket;

class NodeGraphCodecsTest {
	static class AddNode implements Node<Object, Void> {
		InputSocket<Number> inputA = new InputSocket<>(Number.class, "inputA", 0);
		InputSocket<Number> inputB = new InputSocket<>(Number.class, "inputB", 0);
		OutputSocket<Number> output = new OutputSocket<>(Number.class, "output");

		@Override
		public List<Socket<?>> getSockets() { return List.of(inputA, inputB, output); }

		@Override
		public Object initialize() {
			return new Object();
		}

		@Override
		public void process(NodeProcessContext<Object, Void> context) {
			double a = context.get(inputA).doubleValue();
			double b = context.get(inputB).doubleValue();
			context.set(output, a + b);
		}
	}

	static final AddNode ADD_NODE = new AddNode();
	static final Function<Node<?, Void>, String> NODE_TO_ID = Map.of(ADD_NODE, "add")::get;
	static final Function<String, Node<?, Void>> ID_TO_NODE = Map.of("add", ADD_NODE)::get;
	static final Codec<Number> NUMBER_CODEC = Codec.DOUBLE.xmap(d -> d, n -> n.doubleValue());
	static final ValueCodecRegistry VALUE_CODECS = new ValueCodecRegistry() {
		@SuppressWarnings("unchecked")
		@Override
		public <V> Codec<V> getFromType(Class<V> type) {
			return type == Number.class ? (Codec<V>) NUMBER_CODEC : null;
		}
	};

	@Test
	void testInstance() {
		var codec = NodeGraphCodecs.createInstancesCodec(NODE_TO_ID, ID_TO_NODE, VALUE_CODECS).codec();
		var inst = new NodeInstance<>(ADD_NODE, null);
		inst.setInitialValue(ADD_NODE.inputA, 1);
		inst.setInitialValue(ADD_NODE.inputB, 2);
		JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, inst).getPartialOrThrow();
		NodeInstance<?, Void> decoded = codec
			.decode(JsonOps.INSTANCE, encoded)
			.map(Pair::getFirst).getPartialOrThrow();

		NodeGraph<Void> graph = new NodeGraph<>();
		graph.addInstance(decoded);
		assertEquals(1d + 2d, graph.newEvalRound(null).eval(decoded).get(ADD_NODE.output));
	}

	@Test
	void testGraph() {
		Codec<NodeGraph<Void>> codec = NodeGraphCodecs.createGraphCodec(NODE_TO_ID, ID_TO_NODE, VALUE_CODECS).codec();
		NodeGraph<Void> graph = new NodeGraph<>();
		NodeInstance<Object, Void> a = graph.addInstance(new NodeInstance<>(ADD_NODE, null));
		NodeInstance<Object, Void> b = graph.addInstance(new NodeInstance<>(ADD_NODE, null));
		a.setInitialValue(ADD_NODE.inputA, 1);
		a.setInitialValue(ADD_NODE.inputB, 2);
		b.setInitialValue(ADD_NODE.inputA, 3);
		b.setInitialValue(ADD_NODE.inputB, 4);
		graph.connect(a, ADD_NODE.output, b, ADD_NODE.inputA);

		JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, graph).getPartialOrThrow();
		graph = codec.decode(JsonOps.INSTANCE, encoded).map(Pair::getFirst).getPartialOrThrow();
		EvaluationRound<Void> eval = graph.newEvalRound(null);
		assertEquals((1 + 2) + 4, eval.eval(b).get(ADD_NODE.output).doubleValue());
	}
}
