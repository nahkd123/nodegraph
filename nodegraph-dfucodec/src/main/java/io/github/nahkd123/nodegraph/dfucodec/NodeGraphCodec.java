package io.github.nahkd123.nodegraph.dfucodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import io.github.nahkd123.nodegraph.graph.NodeGraph;
import io.github.nahkd123.nodegraph.graph.NodeInstance;
import io.github.nahkd123.nodegraph.graph.NodeSocketRef;
import io.github.nahkd123.nodegraph.socket.InputSocket;
import io.github.nahkd123.nodegraph.socket.OutputSocket;

public class NodeGraphCodec<E> extends MapCodec<NodeGraph<E>> {
	private static final String KEY_INSTANCES = "instances";
	private static final String KEY_CONNECTIONS = "connections";

	private MapCodec<NodeInstance<?, E>> instancesCodec;

	public NodeGraphCodec(MapCodec<NodeInstance<?, E>> instancesCodec) {
		this.instancesCodec = instancesCodec;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> DataResult<NodeGraph<E>> decode(DynamicOps<T> ops, MapLike<T> input) {
		T instancesMapT = input.get(ops.createString(KEY_INSTANCES));
		T conncetionsListT = input.get(ops.createString(KEY_CONNECTIONS));
		NodeGraph<E> graph = new NodeGraph<>();
		Map<String, NodeInstance<?, E>> instanceIds = new HashMap<>();
		List<DataResult<?>> errors = new ArrayList<>();

		if (instancesMapT != null) ops.getMapValues(instancesMapT)
			.ifSuccess(instances -> instances.forEach(pair -> {
				String instanceId = ops.getStringValue(pair.getFirst()).mapOrElse(Function.identity(), error -> {
					errors.add(error);
					return null;
				});
				if (instanceId == null) return;
				ops.getMap(pair.getSecond())
					.flatMap(map -> instancesCodec.decode(ops, map))
					.ifSuccess(instance -> {
						graph.addInstance(instance);
						instanceIds.put(instanceId, instance);
					})
					.ifError(errors::add);
			}))
			.ifError(errors::add);

		if (conncetionsListT != null) ops.getStream(conncetionsListT)
			.ifSuccess(connections -> connections
				.map(raw -> ops.getMap(raw))
				.map(result -> {
					if (result.isError()) errors.add(result);
					return result.isSuccess() ? result.getOrThrow() : null;
				})
				.filter(map -> map != null)
				.map(map -> CodecSocketConnection.CODEC.decode(ops, map))
				.map(result -> {
					if (result.isError()) errors.add(result);
					return result.isSuccess() ? result.getOrThrow() : null;
				})
				.forEach(connection -> {
					NodeInstance<?, E> fromNode = instanceIds.get(connection.from().node());
					if (fromNode == null) {
						errors.add(DataResult.error(() -> "No such node instance with ID %s"
							.formatted(connection.from().node())));
						return;
					}

					NodeInstance<?, E> toNode = instanceIds.get(connection.to().node());
					if (toNode == null) {
						errors.add(DataResult.error(() -> "No such node instance with ID %s"
							.formatted(connection.to().node())));
						return;
					}

					OutputSocket<?> fromSocket = fromNode.getNode().getOutputSockets()
						.stream()
						.filter(socket -> socket.name().equals(connection.from().socket()))
						.findAny()
						.orElse(null);
					if (fromSocket == null) {
						errors.add(DataResult.error(() -> "No such output socket with ID %s in instance %s".formatted(
							connection.from().socket(),
							connection.from().node())));
						return;
					}

					InputSocket<?> toSocket = fromNode.getNode().getInputSockets()
						.stream()
						.filter(socket -> socket.name().equals(connection.to().socket()))
						.findAny()
						.orElse(null);
					if (toSocket == null) {
						errors.add(DataResult.error(() -> "No such input socket with ID %s in instance %s".formatted(
							connection.to().socket(),
							connection.to().node())));
						return;
					}

					((NodeGraph) graph).connect(fromNode, fromSocket, toNode, toSocket);
				}))
			.ifError(errors::add);

		return errors.size() == 0
			? DataResult.success(graph)
			: DataResult.error(() -> errors.stream()
				.map(r -> r.error().get().message())
				.collect(Collectors.joining(";")), graph);
	}

	@Override
	public <T> RecordBuilder<T> encode(NodeGraph<E> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		RecordBuilder<T> instancesMap = ops.mapBuilder();
		ListBuilder<T> connectionsList = ops.listBuilder();
		Map<NodeInstance<?, E>, String> instanceIds = new HashMap<>();

		for (NodeInstance<?, E> instance : input.getInstances()) {
			String id = "instance%04d".formatted(instanceIds.size());
			instanceIds.put(instance, id);
			instancesMap = instancesMap.add(id, instancesCodec
				.encode(instance, ops, ops.mapBuilder())
				.build(ops.empty()));
		}

		for (Entry<NodeSocketRef<?, E, ?>, NodeSocketRef<?, E, ?>> entry : input.getConnections()) {
			NodeSocketRef<?, E, ?> from = entry.getKey();
			NodeSocketRef<?, E, ?> to = entry.getValue();
			CodecSocketRef codecFrom = new CodecSocketRef(instanceIds.get(from.node()), from.socket().name());
			CodecSocketRef codecTo = new CodecSocketRef(instanceIds.get(to.node()), to.socket().name());
			CodecSocketConnection connection = new CodecSocketConnection(codecFrom, codecTo);
			connectionsList.add(CodecSocketConnection.CODEC.codec().encode(connection, ops, ops.empty()));
		}

		return ops.mapBuilder()
			.add(ops.createString(KEY_INSTANCES), instancesMap.build(ops.empty()))
			.add(ops.createString(KEY_CONNECTIONS), connectionsList.build(ops.empty()));
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.of(KEY_INSTANCES, KEY_CONNECTIONS).map(ops::createString);
	}
}
