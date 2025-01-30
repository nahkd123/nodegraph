package io.github.nahkd123.nodegraph.dfucodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;

import io.github.nahkd123.nodegraph.socket.InputSocket;

public class InitialValuesCodec implements Codec<Collection<Entry<InputSocket<?>, ?>>> {
	private Collection<InputSocket<?>> inputSockets;
	private ValueCodecRegistry valueCodecs;

	public InitialValuesCodec(Collection<InputSocket<?>> inputSockets, ValueCodecRegistry valueCodecs) {
		this.inputSockets = inputSockets;
		this.valueCodecs = valueCodecs;
	}

	public Optional<InputSocket<?>> socketFromId(String id) {
		return inputSockets.stream()
			.filter(socket -> socket.name().equals(id))
			.findAny();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> DataResult<T> encode(Collection<Entry<InputSocket<?>, ?>> input, DynamicOps<T> ops, T prefix) {
		RecordBuilder<T> map = ops.mapBuilder();

		for (Entry<InputSocket<?>, ?> entry : input) {
			if (entry.getKey().defaultValue().equals(entry.getValue())) continue;
			T key = ops.createString(entry.getKey().name());
			Codec<?> valueCodec = valueCodecs.getFromType(entry.getKey().type());
			DataResult<T> value = ((Codec) valueCodec).encode(entry.getValue(), ops, ops.empty());
			map = map.add(key, value);
		}

		return map.build(prefix);
	}

	@Override
	public <T> DataResult<Pair<Collection<Entry<InputSocket<?>, ?>>, T>> decode(DynamicOps<T> ops, T input) {
		return ops.getMap(input)
			.map(map -> map.entries()
				.<DataResult<Entry<InputSocket<?>, ?>>>map(pair -> ops.getStringValue(pair.getFirst())
					.map(id -> Pair.of(id, socketFromId(id)))
					.flatMap(o -> o.getSecond().isPresent()
						? DataResult.success(o.getSecond().get())
						: DataResult.error(() -> "No such socket with ID %s".formatted(o.getFirst())))
					.flatMap(socket -> {
						Codec<?> valueCodec = valueCodecs.getFromType(socket.type());
						return valueCodec.decode(ops, pair.getSecond())
							.map(p -> Pair.of(socket, p.getFirst()));
					})
					.map(p -> Map.entry(p.getFirst(), p.getSecond())))
				.toList())
			.flatMap(list -> {
				class ResultHolder {
					List<DataResult<?>> errors = null;
					Collection<Entry<InputSocket<?>, ?>> collection = new ArrayList<>();

					void collect(DataResult<Entry<InputSocket<?>, ?>> entry) {
						if (entry.isError()) {
							if (errors == null) errors = new ArrayList<>();
							errors.add(entry);
						} else {
							collection.add(entry.getOrThrow());
						}
					}

					DataResult<Collection<Entry<InputSocket<?>, ?>>> get() {
						return errors == null
							? DataResult.success(collection)
							: DataResult.error(
								() -> errors.stream()
									.map(e -> e.error().get().message())
									.collect(Collectors.joining("; ")),
								collection);
					}
				}

				ResultHolder holder = new ResultHolder();
				list.forEach(holder::collect);
				return holder.get();
			})
			.map(coll -> Pair.of(coll, input));
	}
}
