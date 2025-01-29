package io.github.nahkd123.nodegraph.serialize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.nahkd123.nodegraph.graph.EvaluationRound;
import io.github.nahkd123.nodegraph.graph.NodeGraph;
import io.github.nahkd123.nodegraph.graph.NodeInstance;
import io.github.nahkd123.nodegraph.node.Node;
import io.github.nahkd123.nodegraph.node.NodeProcessContext;
import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;
import io.github.nahkd123.nodegraph.socket.Socket;

class NodeGraphSerializerV1Test {
	@Test
	void testSerializeAndDeserialize() throws IOException {
		class AddNode implements Node<Object, Void> {
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

		AddNode addNode = new AddNode();
		NodeGraph<Void> domain = new NodeGraph<>();
		NodeInstance<Object, Void> a = domain.addInstance(new NodeInstance<>(addNode, null));
		NodeInstance<Object, Void> b = domain.addInstance(new NodeInstance<>(addNode, null));
		a.setInitialValue(addNode.inputA, 1);
		a.setInitialValue(addNode.inputB, 2);
		b.setInitialValue(addNode.inputA, 3);
		b.setInitialValue(addNode.inputB, 4);
		domain.connect(a, addNode.output, b, addNode.inputA);

		NodeGraphSerializerV1 v1 = new NodeGraphSerializerV1();
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		v1.serializeGraph(
			domain,
			Map.of(addNode, "add")::get,
			Map.of(Number.class, (ValueSerializer<Number>) (v, s) -> s.writeDouble(v.doubleValue()))::get,
			new DataOutputStream(bo));

		ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
		NodeGraph<Void> newDomain = v1.deserializeGraph(
			Map.of("add", addNode)::get,
			Map.of(Number.class, (ValueDeserializer<Number>) s -> s.readDouble())::get,
			new DataInputStream(bi));
		EvaluationRound<Void> eval = newDomain.newEvalRound(null);
		assertEquals((1 + 2) + 4, eval.eval(b).get(addNode.output).doubleValue());
	}
}
