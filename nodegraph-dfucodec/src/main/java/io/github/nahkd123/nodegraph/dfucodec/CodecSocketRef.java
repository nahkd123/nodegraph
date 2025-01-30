package io.github.nahkd123.nodegraph.dfucodec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CodecSocketRef(String node, String socket) {
	public static final MapCodec<CodecSocketRef> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.STRING.fieldOf("node").forGetter(CodecSocketRef::node),
		Codec.STRING.fieldOf("socket").forGetter(CodecSocketRef::socket))
		.apply(i, CodecSocketRef::new));
}
