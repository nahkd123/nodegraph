package io.github.nahkd123.nodegraph.serialize;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.nahkd123.nodegraph.graph.NodeEditorData;
import io.github.nahkd123.nodegraph.graph.NodeGraph;
import io.github.nahkd123.nodegraph.graph.NodeInstance;
import io.github.nahkd123.nodegraph.graph.NodeSocketRef;
import io.github.nahkd123.nodegraph.node.Node;
import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;

class NodeGraphSerializerV1 implements NodeGraphSerializer {
	@Override
	public int versionId() {
		return 1;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <E> NodeGraph<E> deserializeGraph(Function<String, Node<?, E>> idToNode, Function<Class<?>, ValueDeserializer<?>> valueDeserializers, DataInput stream) throws IOException {
		NodeGraph<E> graph = new NodeGraph<>();
		Map<Integer, NodeInstance<?, E>> instanceIds = new HashMap<>();
		int instancesCount = stream.readInt();

		for (int i = 0; i < instancesCount; i++) {
			String id = stream.readUTF();
			Node<?, E> node = idToNode.apply(id);
			if (node == null) throw new IOException("Missing node with ID %s".formatted(id));
			NodeEditorData editor;

			if (stream.readBoolean()) {
				String name = stream.readUTF();
				int x = stream.readInt();
				int y = stream.readInt();
				int w = stream.readInt();
				int h = stream.readInt();
				boolean e = stream.readBoolean();
				editor = new NodeEditorData(name, x, y, w, h, e);
			} else {
				editor = null;
			}

			NodeInstance<?, E> instance = new NodeInstance<>(node, editor);
			stream.readInt(); // TODO: Node parameters
			int socketsCount = stream.readInt();

			for (int j = 0; j < socketsCount; j++) {
				String socketId = stream.readUTF();
				InputSocket<?> socket = node.getInputSockets().stream()
					.filter(s -> s.name().equals(socketId))
					.findAny()
					.orElseThrow(() -> new IOException("Unknown socket %s in node %s".formatted(socketId, id)));
				Object value = valueDeserializers.apply(socket.type()).deserialize(stream);
				instance.setInitialValue((InputSocket) socket, value);
			}

			graph.addInstance(instance);
			instanceIds.put(instanceIds.size(), instance);
		}

		int connectionsCount = stream.readInt();

		for (int i = 0; i < connectionsCount; i++) {
			NodeInstance<?, E> fromNode = instanceIds.get(stream.readInt());
			String fromSocketId = stream.readUTF();
			OutputSocket<?> fromSocket = fromNode.getNode().getOutputSockets().stream()
				.filter(s -> s.name().equals(fromSocketId))
				.findAny()
				.orElseThrow(() -> new IOException("Unknown socket %s in node %s".formatted(
					fromSocketId,
					fromNode.getNode().getClass())));
			NodeInstance<?, E> toNode = instanceIds.get(stream.readInt());
			String toSocketId = stream.readUTF();
			InputSocket<?> toSocket = toNode.getNode().getInputSockets().stream()
				.filter(s -> s.name().equals(toSocketId))
				.findAny()
				.orElseThrow(() -> new IOException("Unknown socket %s in node %s".formatted(
					toSocketId,
					toNode.getNode().getClass())));
			((NodeGraph) graph).connect(fromNode, fromSocket, toNode, toSocket);
		}

		return graph;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <E> void serializeGraph(NodeGraph<E> graph, Function<Node<?, E>, String> nodeToId, Function<Class<?>, ValueSerializer<?>> valueSerializers, DataOutput stream) throws IOException {
		Set<NodeInstance<?, E>> instances = graph.getInstances();
		Map<NodeInstance<?, E>, Integer> instanceIds = new HashMap<>();
		stream.writeInt(instances.size());

		for (NodeInstance<?, E> instance : instances) {
			String id = nodeToId.apply(instance.getNode());
			if (id == null) throw new IOException("Missing ID for node %s".formatted(instance.getNode().getClass()));
			stream.writeUTF(id);

			if (instance.getEditorData() != null) {
				NodeEditorData editor = instance.getEditorData();
				stream.writeBoolean(true);
				stream.writeUTF(editor.getDisplayName());
				stream.writeInt(editor.getX());
				stream.writeInt(editor.getY());
				stream.writeInt(editor.getWidth());
				stream.writeInt(editor.getHeight());
				stream.writeBoolean(editor.isExpanded());
			} else {
				stream.writeBoolean(false);
			}

			Map<InputSocket<?>, ?> sockets = instance.getNode().getInputSockets()
				.stream()
				.filter(s -> !instance.getInitialValue(s).equals(s.defaultValue()))
				.collect(Collectors.toMap(
					s -> (InputSocket<?>) s,
					s -> instance.getInitialValue((InputSocket<?>) s)));
			stream.writeInt(0); // TODO: Node parameters
			stream.writeInt(sockets.size());

			for (Entry<InputSocket<?>, ?> entry : sockets.entrySet()) {
				InputSocket<?> socket = entry.getKey();
				Object value = entry.getValue();
				stream.writeUTF(socket.name());
				((ValueSerializer) valueSerializers.apply(socket.type())).serialize(value, stream);
			}

			instanceIds.put(instance, instanceIds.size());
		}

		var connections = graph.getConnections();
		stream.writeInt(connections.size());

		for (var connection : connections) {
			NodeSocketRef<?, E, ?> from = connection.getKey();
			NodeSocketRef<?, E, ?> to = connection.getValue();
			stream.writeInt(instanceIds.get(from.node()));
			stream.writeUTF(from.socket().name());
			stream.writeInt(instanceIds.get(to.node()));
			stream.writeUTF(to.socket().name());
		}
	}
}
