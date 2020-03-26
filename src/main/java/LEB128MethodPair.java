package main.java;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import org.graalvm.collections.Pair;

import static com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN;

public class LEB128MethodPair {
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
            while (0 <= offset && offset < data.length) {
                sum += read(data, offset).getLeft();
                offset += read(data, offset).getRight();
            }
            return sum;
        }

        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        static Pair<Long, Integer> read(final byte[] data, int startOffset) {
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
            return Pair.create(result, length);
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
