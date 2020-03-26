package main.java;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import static com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN;

public class LEB128MethodAttribute {
    static final class Sum extends RootNode {
        @CompilerDirectives.CompilationFinal(dimensions = 1)
        final byte[] data;
        final FrameSlot offsetSlot;

        int offset;

        Sum(byte[] data) {
            super(null);
            this.data = data;
            this.offsetSlot = getFrameDescriptor().addFrameSlot("offset", FrameSlotKind.Int);
        }

        @Override
        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        public Long execute(VirtualFrame frame) {
            long sum = 0;
            offset = 0;
            for(int i = 0; offset < data.length && i < data.length; ++i) {
                sum += readLong();
            }
            return sum;
        }

        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        long readLong() {
            long result = 0;
            int shift = 0;
            int length = 0;
            int o = offset;
            byte b;
            do {
                b = data[o];
                result |= ((b & 0x7FL) << shift);
                shift += 7;
                o++;
                length++;
            } while ((b & 0x80) != 0 && length < 9);
            return result;

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
