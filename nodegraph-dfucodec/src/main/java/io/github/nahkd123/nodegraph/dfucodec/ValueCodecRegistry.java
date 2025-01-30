package io.github.nahkd123.nodegraph.dfucodec;

import com.mojang.serialization.Codec;

public interface ValueCodecRegistry {
	<V> Codec<V> getFromType(Class<V> type);
}