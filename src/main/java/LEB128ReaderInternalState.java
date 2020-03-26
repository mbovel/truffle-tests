package main.java;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import static com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN;

public class LEB128ReaderInternalState {
    static final class Sum extends RootNode {
        final BinaryReader reader;

        Sum(byte[] data) {
            super(null);
            this.reader = new BinaryReader(data);
        }

        @Override
        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        public Object execute(VirtualFrame frame) {
            long sum = 0;
            reader.setOffset(0);
            for(int i = 0; !reader.done() && i < reader.data.length; ++i) {
                sum += reader.next();
            }
            return sum;
        }
    }

    static final class BinaryReader {
        @CompilationFinal(dimensions = 1)
        final byte[] data;

        private int offset;

        BinaryReader(byte[] data) {
            this.data = data;
        }

        boolean done() {
            return this.offset >= data.length;
        }

        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        long next() {
            long result = 0;
            int shift = 0;
            int length = 0;
            byte b;
            do {
                b = data[offset];
                result |= ((b & 0x7FL) << shift);
                shift += 7;
                offset++;
                length++;
            } while ((b & 0x80) != 0 && length < 9);

            return result;
        }

        public void setOffset(int offset) {
            this.offset = offset;
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
