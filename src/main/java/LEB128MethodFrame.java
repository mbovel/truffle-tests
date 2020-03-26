package main.java;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import static com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN;

public class LEB128MethodFrame {
    static final class Sum extends RootNode {
        @CompilerDirectives.CompilationFinal(dimensions = 1)
        final byte[] data;
        final FrameSlot offsetSlot;

        Sum(byte[] data) {
            super(null);
            this.data = data;
            this.offsetSlot = getFrameDescriptor().addFrameSlot("offset", FrameSlotKind.Int);
        }

        @Override
        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        public Long execute(VirtualFrame frame) {
            frame.setInt(this.offsetSlot, 0);
            long sum = 0;
            try {
                for(int i = 0; i < data.length && frame.getInt(this.offsetSlot) < data.length; ++i) {
                    sum += readLong(frame);
                }
            } catch (FrameSlotTypeException e) {
                throw new RuntimeException(e);
            }
            return sum;
        }

        @ExplodeLoop(kind = FULL_EXPLODE_UNTIL_RETURN)
        long readLong(VirtualFrame frame) throws FrameSlotTypeException {
            long result = 0;
            int shift = 0;
            int offset = frame.getInt(this.offsetSlot);
            int length = 0;
            byte b;
            do {
                b = data[offset];
                result |= ((b & 0x7FL) << shift);
                shift += 7;
                offset++;
                length++;
            } while ((b & 0x80) != 0 && length < 9);
            frame.setInt(this.offsetSlot, offset);
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
