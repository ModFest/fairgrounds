package net.modfest.buffer;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.WrappedByteBuf;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
@NullMarked
public class HashingByteBuf extends WrappedByteBuf {
	private final HashFunction hashFunction;
	private Hasher hasher;

	public HashingByteBuf(HashFunction hashFunction, ByteBuf buf) {
		super(buf);
		this.hashFunction = Objects.requireNonNull(hashFunction);
		this.hasher = Objects.requireNonNull(hashFunction.newHasher());
	}

	public HashCode hash() {
		return hasher.hash();
	}

	public int hashBits() {
		return hashFunction.bits();
	}

	public void resetHasher() {
		this.hasher = Objects.requireNonNull(hashFunction.newHasher());
	}

	@Override
	public byte readByte() {
		var val = super.readByte();
		hasher.putByte(val);
		return val;
	}

	@Override
	public boolean readBoolean() {
		var val = super.readBoolean();
		hasher.putBoolean(val);
		return val;
	}

	@Override
	public short readUnsignedByte() {
		var val = super.readUnsignedByte();
		hasher.putByte((byte) val);
		return val;
	}

	@Override
	public short readShort() {
		var val = super.readShort();
		hasher.putShort(val);
		return val;
	}

	@Override
	public short readShortLE() {
		var val = super.readShortLE();
		hasher.putShort(val);
		return val;
	}

	@Override
	public int readUnsignedShort() {
		var val = super.readUnsignedShort();
		hasher.putShort((short) val);
		return val;
	}

	@Override
	public int readUnsignedShortLE() {
		var val = super.readUnsignedShortLE();
		hasher.putShort((short) val);
		return val;
	}

	@Override
	public int readMedium() {
		var val = super.readMedium();
		hasher.putShort((short) (val & 0xFFFF));
		hasher.putByte((byte) (val >> 16 & 0xFF));
		return val;
	}

	@Override
	public int readMediumLE() {
		var val = super.readMediumLE();
		hasher.putShort((short) (val & 0xFFFF));
		hasher.putByte((byte) (val >> 16 & 0xFF));
		return val;
	}

	@Override
	public int readUnsignedMedium() {
		var val = super.readUnsignedMedium();
		hasher.putShort((short) (val & 0xFFFF));
		hasher.putByte((byte) (val >> 16 & 0xFF));
		return val;
	}

	@Override
	public int readUnsignedMediumLE() {
		var val = super.readUnsignedMediumLE();
		hasher.putShort((short) (val & 0xFFFF));
		hasher.putByte((byte) (val >> 16 & 0xFF));
		return val;
	}

	@Override
	public int readInt() {
		var val = super.readInt();
		hasher.putInt(val);
		return val;
	}

	@Override
	public int readIntLE() {
		var val = super.readIntLE();
		hasher.putInt(val);
		return val;
	}

	@Override
	public long readUnsignedInt() {
		var val = super.readUnsignedInt();
		hasher.putInt((int) val);
		return val;
	}

	@Override
	public long readUnsignedIntLE() {
		var val = super.readUnsignedIntLE();
		hasher.putInt((int) val);
		return val;
	}

	@Override
	public long readLong() {
		var val = super.readLong();
		hasher.putLong(val);
		return val;
	}

	@Override
	public long readLongLE() {
		var val = super.readLongLE();
		hasher.putLong(val);
		return val;
	}

	@Override
	public char readChar() {
		var val = super.readChar();
		hasher.putChar(val);
		return val;
	}

	@Override
	public float readFloat() {
		var val = super.readFloat();
		hasher.putFloat(val);
		return val;
	}

	@Override
	public double readDouble() {
		var val = super.readDouble();
		hasher.putDouble(val);
		return val;
	}

	@Override
	public ByteBuf readBytes(int length) {
		var val = super.readBytes(length);
		hashByteBuf(val);
		return val;
	}

	@Override
	public ByteBuf readSlice(int length) {
		var val = super.readSlice(length);
		hashByteBuf(val);
		return val;
	}

	@Override
	public ByteBuf readRetainedSlice(int length) {
		var val = super.readRetainedSlice(length);
		hashByteBuf(val);
		return val;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst) {
		var val = super.readBytes(dst);
		hashByteBuf(dst);
		return val;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst, int length) {
		var val = super.readBytes(dst, length);
		hashByteBuf(dst, dst.writerIndex() - length, length);
		return val;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
		var val = super.readBytes(dst, dstIndex, length);
		hashByteBuf(dst, dstIndex, length);
		return val;
	}

	@Override
	public ByteBuf readBytes(byte[] dst) {
		var val = super.readBytes(dst);
		hasher.putBytes(dst);
		return val;
	}

	@Override
	public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
		var val = super.readBytes(dst, dstIndex, length);
		hasher.putBytes(dst, dstIndex, length);
		return val;
	}

	@Override
	public ByteBuf readBytes(ByteBuffer dst) {
		var val = super.readBytes(dst);
		hasher.putBytes(dst);
		return val;
	}

	@Override
	public ByteBuf readBytes(OutputStream out, int length) throws IOException {
		int start = buf.readerIndex();
		var val = super.readBytes(out, length);
		hashByteBuf(buf, start, buf.readerIndex());
		return val;
	}

	@Override
	public int readBytes(GatheringByteChannel out, int length) throws IOException {
		int start = buf.readerIndex();
		var val = super.readBytes(out, length);
		hashByteBuf(buf, start, buf.readerIndex());
		return val;
	}

	@Override
	public int readBytes(FileChannel out, long position, int length) throws IOException {
		int start = buf.readerIndex();
		var val = super.readBytes(out, position, length);
		hashByteBuf(buf, start, buf.readerIndex());
		return val;
	}

	@Override
	public CharSequence readCharSequence(int length, Charset charset) {
		var val = super.readCharSequence(length, charset);
		hasher.putString(val, charset);
		return val;
	}

	@Override
	public String readString(int length, Charset charset) {
		var val = super.readString(length, charset);
		hasher.putString(val, charset);
		return val;
	}

	@Override
	public ByteBuf writeBoolean(boolean value) {
		hasher.putBoolean(value);
		return super.writeBoolean(value);
	}

	@Override
	public ByteBuf writeByte(int value) {
		hasher.putByte((byte) value);
		return super.writeByte(value);
	}

	@Override
	public ByteBuf writeShort(int value) {
		hasher.putShort((short) value);
		return super.writeShort(value);
	}

	@Override
	public ByteBuf writeShortLE(int value) {
		hasher.putShort((short) value);
		return super.writeShortLE(value);
	}

	@Override
	public ByteBuf writeMedium(int value) {
		hasher.putShort((short) value);
		hasher.putByte((byte) (value >> 16));
		return super.writeMedium(value);
	}

	@Override
	public ByteBuf writeMediumLE(int value) {
		hasher.putShort((short) value);
		hasher.putByte((byte) (value >> 16));
		return super.writeMediumLE(value);
	}

	@Override
	public ByteBuf writeInt(int value) {
		hasher.putInt(value);
		return super.writeInt(value);
	}

	@Override
	public ByteBuf writeIntLE(int value) {
		hasher.putInt(value);
		return super.writeIntLE(value);
	}

	@Override
	public ByteBuf writeLong(long value) {
		hasher.putLong(value);
		return super.writeLong(value);
	}

	@Override
	public ByteBuf writeLongLE(long value) {
		hasher.putLong(value);
		return super.writeLongLE(value);
	}

	@Override
	public ByteBuf writeChar(int value) {
		hasher.putInt(value);
		return super.writeChar(value);
	}

	@Override
	public ByteBuf writeFloat(float value) {
		hasher.putFloat(value);
		return super.writeFloat(value);
	}

	@Override
	public ByteBuf writeDouble(double value) {
		hasher.putDouble(value);
		return super.writeDouble(value);
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src) {
		hashByteBuf(src);
		return super.writeBytes(src);
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src, int length) {
		hashByteBuf(src, src.readerIndex() - length, length);
		return super.writeBytes(src, length);
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
		hashByteBuf(src, srcIndex, length);
		return super.writeBytes(src, srcIndex, length);
	}

	@Override
	public ByteBuf writeBytes(byte[] src) {
		hasher.putBytes(src);
		return super.writeBytes(src);
	}

	@Override
	public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
		hasher.putBytes(src);
		return super.writeBytes(src, srcIndex, length);
	}

	@Override
	public ByteBuf writeBytes(ByteBuffer src) {
		hasher.putBytes(src);
		return super.writeBytes(src);
	}

	@Override
	public int writeBytes(InputStream in, int length) throws IOException {
		int start = buf.writerIndex();
		var val = super.writeBytes(in, length);
		hashByteBuf(buf, start, buf.writerIndex());
		return val;
	}

	@Override
	public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
		int start = buf.writerIndex();
		var val = super.writeBytes(in, length);
		hashByteBuf(buf, start, buf.writerIndex());
		return val;
	}

	@Override
	public int writeBytes(FileChannel in, long position, int length) throws IOException {
		int start = buf.writerIndex();
		var val = super.writeBytes(in, position, length);
		hashByteBuf(buf, start, buf.writerIndex());
		return val;
	}

	@Override
	public ByteBuf writeZero(int length) {
		hasher.putBytes(new byte[length]);
		return super.writeZero(length);
	}

	@Override
	public int writeCharSequence(CharSequence sequence, Charset charset) {
		hasher.putString(sequence, charset);
		return super.writeCharSequence(sequence, charset);
	}

	private void hashByteBuf(ByteBuf val) {
		if (val.nioBufferCount() > 0) {
			hasher.putBytes(val.nioBuffer());
		} else if (val.hasArray()) {
			hasher.putBytes(val.array(), val.readerIndex(), val.readableBytes());
		} else {
			byte[] tmp = new byte[val.readableBytes()];
			val.getBytes(val.readerIndex(), tmp);
			hasher.putBytes(tmp);
		}
	}

	private void hashByteBuf(ByteBuf val, int index, int length) {
		if (val.nioBufferCount() > 0) {
			hasher.putBytes(val.nioBuffer(index, length));
		} else if (val.hasArray()) {
			hasher.putBytes(val.array(), index, length);
		} else {
			byte[] tmp = new byte[length];
			val.getBytes(index, tmp);
			hasher.putBytes(tmp);
		}
	}
}
