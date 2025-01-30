package io.github.nahkd123.nodegraph.dfucodec;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.nahkd123.nodegraph.graph.NodeEditorData;
import io.github.nahkd123.nodegraph.graph.NodeGraph;
import io.github.nahkd123.nodegraph.graph.NodeInstance;
import io.github.nahkd123.nodegraph.node.Node;
import io.github.nahkd123.nodegraph.socket.InputSocket;

/**
 * <p>
 * Contain main codecs for encoding and decoding graphs and instances.
 * </p>
 */
public class NodeGraphCodecs {
	public static final MapCodec<NodeEditorData> NODE_EDITOR_DATA = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.STRING.fieldOf("displayName").forGetter(NodeEditorData::getDisplayName),
		Codec.INT.fieldOf("x").forGetter(NodeEditorData::getX),
		Codec.INT.fieldOf("y").forGetter(NodeEditorData::getY),
		Codec.INT.fieldOf("width").forGetter(NodeEditorData::getWidth),
		Codec.INT.fieldOf("height").forGetter(NodeEditorData::getHeight),
		Codec.BOOL.optionalFieldOf("expanded", true).forGetter(NodeEditorData::isExpanded))
		.apply(i, NodeEditorData::new));

	public static Codec<Collection<Entry<InputSocket<?>, ?>>> createInitialValuesCodec(Collection<InputSocket<?>> inputSockets, ValueCodecRegistry valueCodecs) {
		return new InitialValuesCodec(inputSockets, valueCodecs);
	}

	public static <S, E> MapCodec<NodeInstance<S, E>> createInstanceCodec(Node<S, E> node, ValueCodecRegistry valueCodecs) {
		Codec<Collection<Entry<InputSocket<?>, ?>>> initialValuesCodec = createInitialValuesCodec(
			node.getInputSockets(),
			valueCodecs);
		return RecordCodecBuilder.mapCodec(i -> i.group(
			NODE_EDITOR_DATA.codec().lenientOptionalFieldOf("editor")
				.forGetter(d -> Optional.ofNullable(d.getEditorData())),
			initialValuesCodec.lenientOptionalFieldOf("initialValues")
				.forGetter(d -> Optional.of(d.getAllInitialValues())))
			.apply(i, (editor, init) -> new NodeInstance<>(node, editor.orElse(null), init.orElse(null))));
	}

	@SuppressWarnings("unchecked")
	public static <E> MapCodec<NodeInstance<?, E>> createInstancesCodec(Function<Node<?, E>, String> nodeToId, Function<String, Node<?, E>> idToNode, ValueCodecRegistry valueCodecs) {
		return Codec.STRING.dispatchMap(
			"type",
			instance -> nodeToId.apply(instance.getNode()),
			id -> (MapCodec<NodeInstance<?, E>>) (Object) createInstanceCodec(idToNode.apply(id), valueCodecs));
	}

	public static <E> MapCodec<NodeGraph<E>> createGraphCodec(Function<Node<?, E>, String> nodeToId, Function<String, Node<?, E>> idToNode, ValueCodecRegistry valueCodecs) {
		return new NodeGraphCodec<>(createInstancesCodec(nodeToId, idToNode, valueCodecs));
	}
}
