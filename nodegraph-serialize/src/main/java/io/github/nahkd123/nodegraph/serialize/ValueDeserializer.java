package io.github.nahkd123.nodegraph.serialize;

import java.io.DataInput;
import java.io.IOException;

@FunctionalInterface
public interface ValueDeserializer<V> {
	V deserialize(DataInput stream) throws IOException;

	ValueDeserializer<Boolean> BOOL = DataInput::readBoolean;
	ValueDeserializer<Byte> BYTE = DataInput::readByte;
	ValueDeserializer<Short> SHORT = DataInput::readShort;
	ValueDeserializer<Integer> INT = DataInput::readInt;
	ValueDeserializer<Long> LONG = DataInput::readLong;
	ValueDeserializer<Float> FLOAT = DataInput::readFloat;
	ValueDeserializer<Double> DOUBLE = DataInput::readDouble;
	ValueDeserializer<String> STRING = DataInput::readUTF;
}
