package io.github.nahkd123.nodegraph.serialize;

import java.io.DataOutput;
import java.io.IOException;

@FunctionalInterface
public interface ValueSerializer<V> {
	void serialize(V value, DataOutput stream) throws IOException;

	ValueSerializer<Boolean> BOOL = (v, s) -> s.writeBoolean(v);
	ValueSerializer<Byte> BYTE = (v, s) -> s.writeByte(v);
	ValueSerializer<Short> SHORT = (v, s) -> s.writeShort(v);
	ValueSerializer<Integer> INT = (v, s) -> s.writeInt(v);
	ValueSerializer<Long> LONG = (v, s) -> s.writeLong(v);
	ValueSerializer<Float> FLOAT = (v, s) -> s.writeFloat(v);
	ValueSerializer<Double> DOUBLE = (v, s) -> s.writeDouble(v);
	ValueSerializer<String> STRING = (v, s) -> s.writeUTF(v);
}
