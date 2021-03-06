

package com.shuabao.socketServer.tcpSocket.serialization;


import com.shuabao.socketServer.util.StackTraceUtil;
import com.shuabao.socketServer.util.UnsafeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.ByteOrder;



public final class UnsafeDirectBufferUtil {

    private static final Logger logger = LoggerFactory.getLogger(UnsafeDirectBufferUtil.class);

    private static final UnsafeUtil.MemoryAccessor memoryAccessor = UnsafeUtil.getMemoryAccessor();

    private static final long BYTE_ARRAY_BASE_OFFSET = UnsafeUtil.arrayBaseOffset(byte[].class);

    // Limits the number of bytes to copy per {@link Unsafe#copyMemory(long, long, long)} to allow safepoint polling
    // during a large copy.
    private static final long UNSAFE_COPY_THRESHOLD = 1024L * 1024L;

    // These numbers represent the point at which we have empirically
    // determined that the average cost of a JNI call exceeds the expense
    // of an element by element copy.  These numbers may change over time.
    private static final int JNI_COPY_TO_ARRAY_THRESHOLD = 6;
    private static final int JNI_COPY_FROM_ARRAY_THRESHOLD = 6;

    private static final boolean BIG_ENDIAN_NATIVE_ORDER = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    // Unaligned-access capability
    private static final boolean UNALIGNED;

    static {
        boolean _unaligned;
        try {
            Class<?> bitsClass = Class.forName("java.nio.Bits", false, UnsafeUtil.getSystemClassLoader());
            Method unalignedMethod = bitsClass.getDeclaredMethod("unaligned");
            unalignedMethod.setAccessible(true);
            _unaligned = (boolean) unalignedMethod.invoke(null);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("java.nio.Bits: unavailable, {}.", StackTraceUtil.stackTrace(t));
            }

            _unaligned = false;
        }
        UNALIGNED = _unaligned;
    }

    public static byte getByte(long address) {
        return memoryAccessor.getByte(address);
    }

    public static short getShort(long address) {
        if (UNALIGNED) {
            short v = memoryAccessor.getShort(address);
            return BIG_ENDIAN_NATIVE_ORDER ? v : Short.reverseBytes(v);
        }
        return (short) (memoryAccessor.getByte(address) << 8 | memoryAccessor.getByte(address + 1) & 0xff);
    }

    public static short getShortLE(long address) {
        if (UNALIGNED) {
            short v = memoryAccessor.getShort(address);
            return BIG_ENDIAN_NATIVE_ORDER ? Short.reverseBytes(v) : v;
        }
        return (short) (memoryAccessor.getByte(address) & 0xff | memoryAccessor.getByte(address + 1) << 8);
    }

    public static int getInt(long address) {
        if (UNALIGNED) {
            int v = memoryAccessor.getInt(address);
            return BIG_ENDIAN_NATIVE_ORDER ? v : Integer.reverseBytes(v);
        }
        return memoryAccessor.getByte(address) << 24 |
                (memoryAccessor.getByte(address + 1) & 0xff) << 16 |
                (memoryAccessor.getByte(address + 2) & 0xff) << 8 |
                memoryAccessor.getByte(address + 3) & 0xff;
    }

    public static int getIntLE(long address) {
        if (UNALIGNED) {
            int v = memoryAccessor.getInt(address);
            return BIG_ENDIAN_NATIVE_ORDER ? Integer.reverseBytes(v) : v;
        }
        return memoryAccessor.getByte(address) & 0xff |
                (memoryAccessor.getByte(address + 1) & 0xff) << 8 |
                (memoryAccessor.getByte(address + 2) & 0xff) << 16 |
                memoryAccessor.getByte(address + 3) << 24;
    }

    public static long getLong(long address) {
        if (UNALIGNED) {
            long v = memoryAccessor.getLong(address);
            return BIG_ENDIAN_NATIVE_ORDER ? v : Long.reverseBytes(v);
        }
        return ((long) memoryAccessor.getByte(address)) << 56 |
                (memoryAccessor.getByte(address + 1) & 0xffL) << 48 |
                (memoryAccessor.getByte(address + 2) & 0xffL) << 40 |
                (memoryAccessor.getByte(address + 3) & 0xffL) << 32 |
                (memoryAccessor.getByte(address + 4) & 0xffL) << 24 |
                (memoryAccessor.getByte(address + 5) & 0xffL) << 16 |
                (memoryAccessor.getByte(address + 6) & 0xffL) << 8 |
                (memoryAccessor.getByte(address + 7)) & 0xffL;
    }

    public static long getLongLE(long address) {
        if (UNALIGNED) {
            long v = memoryAccessor.getLong(address);
            return BIG_ENDIAN_NATIVE_ORDER ? Long.reverseBytes(v) : v;
        }
        return (memoryAccessor.getByte(address)) & 0xffL |
                (memoryAccessor.getByte(address + 1) & 0xffL) << 8 |
                (memoryAccessor.getByte(address + 2) & 0xffL) << 16 |
                (memoryAccessor.getByte(address + 3) & 0xffL) << 24 |
                (memoryAccessor.getByte(address + 4) & 0xffL) << 32 |
                (memoryAccessor.getByte(address + 5) & 0xffL) << 40 |
                (memoryAccessor.getByte(address + 6) & 0xffL) << 48 |
                ((long) memoryAccessor.getByte(address + 7)) << 56;
    }

    public static void getBytes(long address, byte[] dst, int dstIndex, int length) {
        if (length > JNI_COPY_TO_ARRAY_THRESHOLD) {
            copyMemory(null, address, dst, BYTE_ARRAY_BASE_OFFSET + dstIndex, length);
        } else {
            int end = dstIndex + length;
            for (int i = dstIndex; i < end; i++) {
                dst[i] = memoryAccessor.getByte(address++);
            }
        }
    }

    public static void setByte(long address, int value) {
        memoryAccessor.putByte(address, (byte) value);
    }

    public static void setShort(long address, int value) {
        if (UNALIGNED) {
            memoryAccessor.putShort(
                    address, BIG_ENDIAN_NATIVE_ORDER ? (short) value : Short.reverseBytes((short) value));
        } else {
            memoryAccessor.putByte(address, (byte) (value >>> 8));
            memoryAccessor.putByte(address + 1, (byte) value);
        }
    }

    public static void setShortLE(long address, int value) {
        if (UNALIGNED) {
            memoryAccessor.putShort(
                    address, BIG_ENDIAN_NATIVE_ORDER ? Short.reverseBytes((short) value) : (short) value);
        } else {
            memoryAccessor.putByte(address, (byte) value);
            memoryAccessor.putByte(address + 1, (byte) (value >>> 8));
        }
    }

    public static void setInt(long address, int value) {
        if (UNALIGNED) {
            memoryAccessor.putInt(address, BIG_ENDIAN_NATIVE_ORDER ? value : Integer.reverseBytes(value));
        } else {
            memoryAccessor.putByte(address, (byte) (value >>> 24));
            memoryAccessor.putByte(address + 1, (byte) (value >>> 16));
            memoryAccessor.putByte(address + 2, (byte) (value >>> 8));
            memoryAccessor.putByte(address + 3, (byte) value);
        }
    }

    public static void setIntLE(long address, int value) {
        if (UNALIGNED) {
            memoryAccessor.putInt(address, BIG_ENDIAN_NATIVE_ORDER ? Integer.reverseBytes(value) : value);
        } else {
            memoryAccessor.putByte(address, (byte) value);
            memoryAccessor.putByte(address + 1, (byte) (value >>> 8));
            memoryAccessor.putByte(address + 2, (byte) (value >>> 16));
            memoryAccessor.putByte(address + 3, (byte) (value >>> 24));
        }
    }

    public static void setLong(long address, long value) {
        if (UNALIGNED) {
            memoryAccessor.putLong(address, BIG_ENDIAN_NATIVE_ORDER ? value : Long.reverseBytes(value));
        } else {
            memoryAccessor.putByte(address, (byte) (value >>> 56));
            memoryAccessor.putByte(address + 1, (byte) (value >>> 48));
            memoryAccessor.putByte(address + 2, (byte) (value >>> 40));
            memoryAccessor.putByte(address + 3, (byte) (value >>> 32));
            memoryAccessor.putByte(address + 4, (byte) (value >>> 24));
            memoryAccessor.putByte(address + 5, (byte) (value >>> 16));
            memoryAccessor.putByte(address + 6, (byte) (value >>> 8));
            memoryAccessor.putByte(address + 7, (byte) value);
        }
    }

    public static void setLongLE(long address, long value) {
        if (UNALIGNED) {
            memoryAccessor.putLong(address, BIG_ENDIAN_NATIVE_ORDER ? Long.reverseBytes(value) : value);
        } else {
            memoryAccessor.putByte(address, (byte) value);
            memoryAccessor.putByte(address + 1, (byte) (value >>> 8));
            memoryAccessor.putByte(address + 2, (byte) (value >>> 16));
            memoryAccessor.putByte(address + 3, (byte) (value >>> 24));
            memoryAccessor.putByte(address + 4, (byte) (value >>> 32));
            memoryAccessor.putByte(address + 5, (byte) (value >>> 40));
            memoryAccessor.putByte(address + 6, (byte) (value >>> 48));
            memoryAccessor.putByte(address + 7, (byte) (value >>> 56));
        }
    }

    public static void setBytes(long address, byte[] src, int srcIndex, int length) {
        if (length > JNI_COPY_FROM_ARRAY_THRESHOLD) {
            copyMemory(src, BYTE_ARRAY_BASE_OFFSET + srcIndex, null, address, length);
        } else {
            int end = srcIndex + length;
            for (int i = srcIndex; i < end; i++) {
                memoryAccessor.putByte(address++, src[i]);
            }
        }
    }

    private static void copyMemory(Object src, long srcOffset, Object dst, long dstOffset, long length) {
        while (length > 0) {
            long size = Math.min(length, UNSAFE_COPY_THRESHOLD);
            memoryAccessor.copyMemory(src, srcOffset, dst, dstOffset, size);
            length -= size;
            srcOffset += size;
            dstOffset += size;
        }
    }

    private UnsafeDirectBufferUtil() {}
}
