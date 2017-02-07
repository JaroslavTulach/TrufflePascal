// CheckStyle: start generated
package cz.cuni.mff.d3s.trupple.language.nodes.variables;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.internal.SpecializationNode;
import com.oracle.truffle.api.dsl.internal.SpecializedNode;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import cz.cuni.mff.d3s.trupple.language.PascalTypesGen;

@GeneratedBy(ReadVariableNode.class)
public final class ReadVariableNodeGen extends ReadVariableNode implements SpecializedNode {

    private final FrameSlot slot;
    @CompilationFinal private boolean excludeReadLong_;
    @CompilationFinal private boolean excludeReadBool_;
    @CompilationFinal private boolean excludeReadChar_;
    @CompilationFinal private boolean excludeReadDouble_;
    @CompilationFinal private boolean excludeReadObject_;
    @Child private BaseNode_ specialization_;

    private ReadVariableNodeGen(FrameSlot slot) {
        this.slot = slot;
        this.specialization_ = UninitializedNode_.create(this);
    }

    @Override
    protected FrameSlot getSlot() {
        return this.slot;
    }

    @Override
    public NodeCost getCost() {
        return specialization_.getNodeCost();
    }

    @Override
    public Object executeGeneric(VirtualFrame frameValue) {
        return specialization_.execute(frameValue);
    }

    @Override
    public void executeVoid(VirtualFrame frameValue) {
        specialization_.executeVoid(frameValue);
        return;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frameValue) throws UnexpectedResultException {
        return specialization_.executeBoolean(frameValue);
    }

    @Override
    public char executeChar(VirtualFrame frameValue) throws UnexpectedResultException {
        return specialization_.executeChar(frameValue);
    }

    @Override
    public long executeLong(VirtualFrame frameValue) throws UnexpectedResultException {
        return specialization_.executeLong(frameValue);
    }

    @Override
    public SpecializationNode getSpecializationNode() {
        return specialization_;
    }

    @Override
    public Node deepCopy() {
        return SpecializationNode.updateRoot(super.deepCopy());
    }

    public static ReadVariableNode create(FrameSlot slot) {
        return new ReadVariableNodeGen(slot);
    }

    @GeneratedBy(ReadVariableNode.class)
    private abstract static class BaseNode_ extends SpecializationNode {

        @CompilationFinal protected ReadVariableNodeGen root;

        BaseNode_(ReadVariableNodeGen root, int index) {
            super(index);
            this.root = root;
        }

        @Override
        protected final void setRoot(Node root) {
            this.root = (ReadVariableNodeGen) root;
        }

        @Override
        protected final Node[] getSuppliedChildren() {
            return new Node[] {};
        }

        @Override
        public final Object acceptAndExecute(Frame frameValue) {
            return this.execute((VirtualFrame) frameValue);
        }

        public abstract Object execute(VirtualFrame frameValue);

        public void executeVoid(VirtualFrame frameValue) {
            execute(frameValue);
            return;
        }

        public boolean executeBoolean(VirtualFrame frameValue) throws UnexpectedResultException {
            return PascalTypesGen.expectBoolean(execute(frameValue));
        }

        public char executeChar(VirtualFrame frameValue) throws UnexpectedResultException {
            return PascalTypesGen.expectCharacter(execute(frameValue));
        }

        public long executeLong(VirtualFrame frameValue) throws UnexpectedResultException {
            return PascalTypesGen.expectLong(execute(frameValue));
        }

        @Override
        protected final SpecializationNode createNext(Frame frameValue) {
            if (!root.excludeReadLong_) {
                return ReadLongNode_.create(root);
            }
            if (!root.excludeReadBool_) {
                return ReadBoolNode_.create(root);
            }
            if (!root.excludeReadChar_) {
                return ReadCharNode_.create(root);
            }
            if (!root.excludeReadDouble_) {
                return ReadDoubleNode_.create(root);
            }
            if (!root.excludeReadObject_) {
                return ReadObjectNode_.create(root);
            }
            return null;
        }

    }
    @GeneratedBy(ReadVariableNode.class)
    private static final class UninitializedNode_ extends BaseNode_ {

        UninitializedNode_(ReadVariableNodeGen root) {
            super(root, 2147483647);
        }

        @Override
        public Object execute(VirtualFrame frameValue) {
            return uninitialized(frameValue);
        }

        static BaseNode_ create(ReadVariableNodeGen root) {
            return new UninitializedNode_(root);
        }

    }
    @GeneratedBy(methodName = "readLong(VirtualFrame)", value = ReadVariableNode.class)
    private static final class ReadLongNode_ extends BaseNode_ {

        ReadLongNode_(ReadVariableNodeGen root) {
            super(root, 1);
        }

        @Override
        public Object execute(VirtualFrame frameValue) {
            try {
                return executeLong(frameValue);
            } catch (UnexpectedResultException ex) {
                return ex.getResult();
            }
        }

        @Override
        public long executeLong(VirtualFrame frameValue) throws UnexpectedResultException {
            try {
                return root.readLong(frameValue);
            } catch (FrameSlotTypeException ex) {
                root.excludeReadLong_ = true;
                return PascalTypesGen.expectLong(remove("threw rewrite exception", frameValue));
            }
        }

        static BaseNode_ create(ReadVariableNodeGen root) {
            return new ReadLongNode_(root);
        }

    }
    @GeneratedBy(methodName = "readBool(VirtualFrame)", value = ReadVariableNode.class)
    private static final class ReadBoolNode_ extends BaseNode_ {

        ReadBoolNode_(ReadVariableNodeGen root) {
            super(root, 2);
        }

        @Override
        public Object execute(VirtualFrame frameValue) {
            try {
                return executeBoolean(frameValue);
            } catch (UnexpectedResultException ex) {
                return ex.getResult();
            }
        }

        @Override
        public boolean executeBoolean(VirtualFrame frameValue) throws UnexpectedResultException {
            try {
                return root.readBool(frameValue);
            } catch (FrameSlotTypeException ex) {
                root.excludeReadBool_ = true;
                return PascalTypesGen.expectBoolean(remove("threw rewrite exception", frameValue));
            }
        }

        static BaseNode_ create(ReadVariableNodeGen root) {
            return new ReadBoolNode_(root);
        }

    }
    @GeneratedBy(methodName = "readChar(VirtualFrame)", value = ReadVariableNode.class)
    private static final class ReadCharNode_ extends BaseNode_ {

        ReadCharNode_(ReadVariableNodeGen root) {
            super(root, 3);
        }

        @Override
        public Object execute(VirtualFrame frameValue) {
            try {
                return executeChar(frameValue);
            } catch (UnexpectedResultException ex) {
                return ex.getResult();
            }
        }

        @Override
        public char executeChar(VirtualFrame frameValue) throws UnexpectedResultException {
            try {
                return root.readChar(frameValue);
            } catch (FrameSlotTypeException ex) {
                root.excludeReadChar_ = true;
                return PascalTypesGen.expectCharacter(remove("threw rewrite exception", frameValue));
            }
        }

        static BaseNode_ create(ReadVariableNodeGen root) {
            return new ReadCharNode_(root);
        }

    }
    @GeneratedBy(methodName = "readDouble(VirtualFrame)", value = ReadVariableNode.class)
    private static final class ReadDoubleNode_ extends BaseNode_ {

        ReadDoubleNode_(ReadVariableNodeGen root) {
            super(root, 4);
        }

        @Override
        public Object execute(VirtualFrame frameValue) {
            try {
                return root.readDouble(frameValue);
            } catch (FrameSlotTypeException ex) {
                root.excludeReadDouble_ = true;
                return remove("threw rewrite exception", frameValue);
            }
        }

        static BaseNode_ create(ReadVariableNodeGen root) {
            return new ReadDoubleNode_(root);
        }

    }
    @GeneratedBy(methodName = "readObject(VirtualFrame)", value = ReadVariableNode.class)
    private static final class ReadObjectNode_ extends BaseNode_ {

        ReadObjectNode_(ReadVariableNodeGen root) {
            super(root, 5);
        }

        @Override
        public Object execute(VirtualFrame frameValue) {
            try {
                return root.readObject(frameValue);
            } catch (FrameSlotTypeException ex) {
                root.excludeReadObject_ = true;
                return remove("threw rewrite exception", frameValue);
            }
        }

        static BaseNode_ create(ReadVariableNodeGen root) {
            return new ReadObjectNode_(root);
        }

    }
}