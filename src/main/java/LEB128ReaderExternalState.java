package main.java;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import static com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN;

public class LEB128ReaderExternalState {
    static final class Sum extends RootNode {
        @CompilerDirectives.CompilationFinal(dimensions = 1)
        final byte[] data;

        Sum(byte[] data) {
            super(null);
            this.data = data;
        }

        @Override
        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        public Object execute(VirtualFrame frame) {
            int offset = 0;
            long sum = 0;
            for(int i = 0; offset < data.length && i < data.length; ++i) {
                BinaryReaderResult result = BinaryReader.read(data, offset);
                sum += result.value;
                offset = result.endOffset;
            }
            return sum;
        }
    }

    static final class BinaryReader {
        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        static BinaryReaderResult read(final byte[] data, int startOffset) {
            long result = 0;
            int shift = 0;
            int offset = startOffset;
            int length = 0;
            byte b;
            do {
                b = data[offset];
                result |= ((b & 0x7FL) << shift);
                shift += 7;
                offset++;
                length++;
            } while ((b & 0x80) != 0 && length < 9);
            return new BinaryReaderResult(result, offset);
        }
    }

    static final class BinaryReaderResult {
        @CompilerDirectives.CompilationFinal final long value;
        @CompilerDirectives.CompilationFinal final int endOffset;

        public BinaryReaderResult(long value, int endOffset) {
            this.value = value;
            this.endOffset = endOffset;
        }
    }

    public static void main(String[] args) {
        // 624485, -123456
        final byte[] data = Utils.hexStringToByteArray("E58E26E58E26");
        Sum sumNode = new Sum(data);
        CallTarget target = Truffle.getRuntime().createCallTarget(sumNode);
        for (int i = 0; i < 1000; i++) {
            target.call();
        }
        long result = (long) target.call();
        System.out.println(result);
    }

}