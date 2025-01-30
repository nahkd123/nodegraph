package io.github.nahkd123.nodegraph.dfucodec;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CodecSocketConnection(CodecSocketRef from, CodecSocketRef to) {
	public static final MapCodec<CodecSocketConnection> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		CodecSocketRef.CODEC.fieldOf("from").forGetter(CodecSocketConnection::from),
		CodecSocketRef.CODEC.fieldOf("to").forGetter(CodecSocketConnection::to)).apply(i, CodecSocketConnection::new));
}
