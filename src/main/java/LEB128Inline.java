package main.java;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import static com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN;

public class LEB128Inline {
    static final class Sum extends RootNode {
        @CompilationFinal(dimensions = 1)
        final byte[] data;

        Sum(byte[] data) {
            super(null);
            this.data = data;
        }

        @Override
        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        public Long execute(VirtualFrame frame) {
            int offset = 0;
            long sum = 0;
            while (offset < data.length) {
                long result = 0;
                int shift = 0;
                byte b;
                do {
                    b = data[offset];
                    result |= ((b & 0x7FL) << shift);
                    shift += 7;
                    offset++;
                } while ((b & 0x80) != 0);

                sum += result;
            }
            return sum;
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
