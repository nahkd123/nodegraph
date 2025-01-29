package io.github.nahkd123.nodegraph.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.nahkd123.nodegraph.node.Node;
import io.github.nahkd123.nodegraph.node.NodeProcessContext;
import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;
import io.github.nahkd123.nodegraph.socket.Socket;

class NodeGraphTest {
	@Test
	void testEval() {
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

		EvaluationRound<Void> eval = domain.newEvalRound(null);
		assertEquals(1 + 2, eval.eval(a).get(addNode.output).doubleValue());
		assertEquals(3 + 4, eval.eval(b).get(addNode.output).doubleValue());

		domain.connect(a, addNode.output, b, addNode.inputA);
		eval = domain.newEvalRound(null);
		assertEquals((1 + 2) + 4, eval.eval(b).get(addNode.output).doubleValue());

		domain.connect(a, addNode.output, b, addNode.inputB);
		eval = domain.newEvalRound(null);
		assertEquals((1 + 2) + (1 + 2), eval.eval(b).get(addNode.output).doubleValue());
	}
}
