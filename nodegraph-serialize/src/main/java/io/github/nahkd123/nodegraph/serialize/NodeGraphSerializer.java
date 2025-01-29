package io.github.nahkd123.nodegraph.serialize;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import io.github.nahkd123.nodegraph.graph.NodeGraph;
import io.github.nahkd123.nodegraph.node.Node;

public interface NodeGraphSerializer {
	NodeGraphSerializer CURRENT_VERSION = new NodeGraphSerializerV1();
	Map<Integer, NodeGraphSerializer> VERSIONS = Map.of(1, CURRENT_VERSION);

	static <E> NodeGraph<E> deserialize(Function<String, Node<?, E>> idToNode, Function<Class<?>, ValueDeserializer<?>> valueDeserializers, DataInput stream) throws IOException {
		int versionId = stream.readInt();
		NodeGraphSerializer serializer = VERSIONS.get(versionId);

		if (serializer == null) {
			if (versionId > CURRENT_VERSION.versionId())
				throw new IOException("Version %d is not supported. Please update nodegraph-serialize."
					.formatted(versionId));
			else throw new IOException("Version %d is no longer supported.".formatted(versionId));
		}

		return serializer.deserializeGraph(idToNode, valueDeserializers, stream);
	}

	static <E> void serialize(NodeGraph<E> graph, Function<Node<?, E>, String> nodeToId, Function<Class<?>, ValueSerializer<?>> valueSerializers, DataOutput stream) throws IOException {
		stream.writeInt(CURRENT_VERSION.versionId());
		CURRENT_VERSION.serializeGraph(graph, nodeToId, valueSerializers, stream);
	}

	int versionId();

	<E> NodeGraph<E> deserializeGraph(Function<String, Node<?, E>> idToNode, Function<Class<?>, ValueDeserializer<?>> valueDeserializers, DataInput stream) throws IOException;

	<E> void serializeGraph(NodeGraph<E> graph, Function<Node<?, E>, String> nodeToId, Function<Class<?>, ValueSerializer<?>> valueSerializers, DataOutput stream) throws IOException;
}
