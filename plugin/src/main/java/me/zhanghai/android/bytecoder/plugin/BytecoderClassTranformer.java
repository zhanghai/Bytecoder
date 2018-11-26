/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

class BytecoderClassTranformer {

    private BytecoderClassTranformer() {}

    public static void transform(@Nonnull Path inputFile, @Nonnull Path outputFile)
            throws IOException {
        ClassReader reader = new ClassReader(Files.readAllBytes(inputFile));
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new BytecoderClassVisitor(writer);
        reader.accept(visitor, 0);
        Files.write(outputFile, writer.toByteArray());
    }

    private static class BytecoderClassVisitor extends ClassVisitor {

        private String className;

        public BytecoderClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM6, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            className = name.replace('/', '.');
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                         String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (mv != null) {
                String method = className + '.' + name;
                mv = new BytecoderMethodVisitor(method, access, desc, exceptions, mv);
            }
            return mv;
        }
    }

    private static class BytecoderMethodVisitor extends MethodVisitor {

        private String method;
        private int access;
        private Type[] parameterTypes;
        private Type returnType;
        private boolean throwsLinkageError;

        private int annotatedOpcode;
        private Type annotatedClassType;
        private String annotatedMethodName;
        private Type[] annotatedParameterTypes;
        private Type annotatedReturnType;

        public BytecoderMethodVisitor(String method, int access, String descriptor,
                                      String[] exceptions, MethodVisitor mv) {
            super(Opcodes.ASM6, mv);

            this.method = method;
            this.access = access;
            parameterTypes = Type.getArgumentTypes(descriptor);
            returnType = Type.getReturnType(descriptor);
            throwsLinkageError = exceptions != null && Arrays.asList(exceptions).contains(
                    "java/lang/LinkageError");
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            switch (desc) {
                case "Lme/zhanghai/android/bytecoder/library/InvokeConstructor;":
                    if (hasTarget()) {
                        throw new IllegalArgumentException("Method has a duplicate @Invoke* " + desc
                                + ": " + method);
                    }
                    annotatedOpcode = Opcodes.INVOKESPECIAL;
                    return new InvokeAnnotationVisitor();
                case "Lme/zhanghai/android/bytecoder/library/InvokeInterface;":
                    if (hasTarget()) {
                        throw new IllegalArgumentException("Method has a duplicate @Invoke* " + desc
                                + ": " + method);
                    }
                    annotatedOpcode = Opcodes.INVOKEINTERFACE;
                    return new InvokeAnnotationVisitor();
                case "Lme/zhanghai/android/bytecoder/library/InvokeStatic;":
                    if (hasTarget()) {
                        throw new IllegalArgumentException("Method has a duplicate @Invoke* " + desc
                                + ": " + method);
                    }
                    annotatedOpcode = Opcodes.INVOKESTATIC;
                    return new InvokeAnnotationVisitor();
                case "Lme/zhanghai/android/bytecoder/library/InvokeVirtual;":
                    if (hasTarget()) {
                        throw new IllegalArgumentException("Method has a duplicate @Invoke* " + desc
                                + ": " + method);
                    }
                    annotatedOpcode = Opcodes.INVOKEVIRTUAL;
                    return new InvokeAnnotationVisitor();
                case "Lme/zhanghai/android/bytecoder/library/TypeName;":
                    return new ReturnTypeNameAnnotationVisitor();
                default:
                    return super.visitAnnotation(desc, visible);
            }
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor,
                                                          boolean visible) {
            switch (descriptor) {
                case "Lme/zhanghai/android/bytecoder/library/TypeName;":
                    if (annotatedParameterTypes == null) {
                        annotatedParameterTypes = new Type[parameterTypes.length];
                    }
                    return new ParameterTypeNameAnnotationVisitor(parameter);
                default:
                    return super.visitParameterAnnotation(parameter, descriptor, visible);
            }
        }

        @Override
        public void visitCode() {
            if (!hasTarget()) {
                super.visitCode();
            }
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            if (!hasTarget()) {
                super.visitFrame(type, nLocal, local, nStack, stack);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (!hasTarget()) {
                super.visitInsn(opcode);
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (!hasTarget()) {
                super.visitIntInsn(opcode, operand);
            }
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (!hasTarget()) {
                super.visitVarInsn(opcode, var);
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (!hasTarget()) {
                super.visitTypeInsn(opcode, type);
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (!hasTarget()) {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc,
                                    boolean itf) {
            if (!hasTarget()) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                           Object... bsmArgs) {
            if (!hasTarget()) {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            if (!hasTarget()) {
                super.visitJumpInsn(opcode, label);
            }
        }

        @Override
        public void visitLabel(Label label) {
            if (!hasTarget()) {
                super.visitLabel(label);
            }
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (!hasTarget()) {
                super.visitLdcInsn(cst);
            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            if (!hasTarget()) {
                super.visitIincInsn(var, increment);
            }
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            if (!hasTarget()) {
                super.visitTableSwitchInsn(min, max, dflt, labels);
            }
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            if (!hasTarget()) {
                super.visitLookupSwitchInsn(dflt, keys, labels);
            }
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            if (!hasTarget()) {
                super.visitMultiANewArrayInsn(desc, dims);
            }
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc,
                                                     boolean visible) {
            if (!hasTarget()) {
                return super.visitInsnAnnotation(typeRef, typePath, desc, visible);
            }
            return null;
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            if (!hasTarget()) {
                super.visitTryCatchBlock(start, end, handler, type);
            }
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath,
                                                         String desc, boolean visible) {
            if (!hasTarget()) {
                return super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
            }
            return null;
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start,
                                       Label end, int index) {
            if (!hasTarget()) {
                super.visitLocalVariable(name, desc, signature, start, end, index);
            }
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
                                                              Label[] start, Label[] end,
                                                              int[] index, String desc,
                                                              boolean visible) {
            if (!hasTarget()) {
                return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index,
                        desc, visible);
            }
            return null;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!hasTarget()) {
                super.visitLineNumber(line, start);
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (!hasTarget()) {
                super.visitMaxs(maxStack, maxLocals);
            }
        }

        @Override
        public void visitEnd() {
            if (!hasTarget()) {
                if (annotatedParameterTypes != null) {
                    throw new IllegalArgumentException("Method has a parameter with @TypeName but"
                            + " missing @Invoke*: " + method);
                }
                if (annotatedReturnType != null) {
                    throw new IllegalArgumentException("Method has @TypeName but missing @Invoke*: "
                            + method);
                }
                super.visitEnd();
            }

            if ((access & Opcodes.ACC_STATIC) == 0) {
                throw new IllegalArgumentException("Method must be static: " + method);
            }
            if ((access & Opcodes.ACC_BRIDGE) != 0) {
                throw new IllegalArgumentException("Method access must not have ACC_BRIDGE: "
                        + method);
            }
            if ((access & Opcodes.ACC_NATIVE) != 0) {
                throw new IllegalArgumentException("Method must not be native: " + method);
            }
            if ((access & Opcodes.ACC_ABSTRACT) != 0) {
                throw new IllegalArgumentException("Method must not be abstract: " + method);
            }
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
                throw new IllegalArgumentException("Method access must not have ACC_SYNTHETIC: "
                        + method);
            }

            switch (annotatedOpcode) {
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKEVIRTUAL:
                    if (parameterTypes.length < 1) {
                        throw new IllegalArgumentException("Method must take an instance of the"
                                + " target class as its first parameter: " + method);
                    }
                    Type firstParameterType = annotatedParameterTypes != null
                            && annotatedParameterTypes[0] != null ? annotatedParameterTypes[0]
                            : parameterTypes[0];
                    if (!firstParameterType.equals(annotatedClassType)) {
                        throw new IllegalArgumentException("Method must declare the type of its"
                                + " first parameter the same as the target class: " + method);
                    }
                    break;
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown opcode " + annotatedOpcode + ": "
                            + method);
            }

            if (!throwsLinkageError) {
                throw new IllegalArgumentException("Method must throw LinkageError: " + method);
            }

            mv.visitCode();

            int maxStack = 0;
            int maxLocals;

            if (annotatedOpcode == Opcodes.INVOKESPECIAL) {
                mv.visitTypeInsn(Opcodes.NEW, annotatedClassType.getInternalName());
                mv.visitInsn(Opcodes.DUP);
                maxStack += 2;
            }

            int localIndex = 0;
            for (Type parameterType : parameterTypes) {
                mv.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), localIndex);
                localIndex += parameterType.getSize();
            }
            maxStack += localIndex;
            maxLocals = localIndex;

            String targetClassInternalName = annotatedClassType.getInternalName();

            int targetParameterStartIndex;
            switch (annotatedOpcode) {
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKEVIRTUAL:
                    targetParameterStartIndex = 1;
                    break;
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                    targetParameterStartIndex = 0;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown opcode " + annotatedOpcode + ": "
                            + method);
            }
            Type[] targetParameterTypes = new Type[parameterTypes.length
                    - targetParameterStartIndex];
            for (int i = 0; i < targetParameterTypes.length; ++i) {
                int parameterIndex = targetParameterStartIndex + i;
                boolean hasAnnotatedParameterType = annotatedParameterTypes != null
                        && annotatedParameterTypes[parameterIndex] != null;
                targetParameterTypes[i] = hasAnnotatedParameterType ?
                        annotatedParameterTypes[parameterIndex] : parameterTypes[parameterIndex];
            }
            Type targetReturnType = annotatedOpcode == Opcodes.INVOKESPECIAL ? Type.VOID_TYPE
                    : annotatedReturnType != null ? annotatedReturnType : returnType;
            String targetMethodDescriptor = Type.getMethodDescriptor(targetReturnType,
                    targetParameterTypes);

            boolean targetIsInterface = annotatedOpcode == Opcodes.INVOKEINTERFACE;

            mv.visitMethodInsn(annotatedOpcode, targetClassInternalName, annotatedMethodName,
                    targetMethodDescriptor, targetIsInterface);

            mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

            if (!returnType.equals(Type.VOID_TYPE)) {
                maxStack = Math.max(maxStack, 1);
            }

            mv.visitMaxs(maxStack, maxLocals);

            mv.visitEnd();
        }

        private boolean hasTarget() {
            return annotatedOpcode != 0;
        }

        /**
         * @see Type#getDescriptor(Class)
         */
        private static String getDescriptor(String className) {
            switch (className) {
                case "void":
                    return "V";
                case "boolean":
                    return "Z";
                case "byte":
                    return "B";
                case "char":
                    return "C";
                case "short":
                    return "S";
                case "int":
                    return "I";
                case "long":
                    return "J";
                case "float":
                    return "F";
                case "double":
                    return "D";
                default:
                    if (className.charAt(0) == '[') {
                        return className.replace('.', '/');
                    } else {
                        return 'L' + className.replace('.', '/') + ';';
                    }
            }
        }

        private class InvokeAnnotationVisitor extends AnnotationVisitor {

            public InvokeAnnotationVisitor() {
                super(Opcodes.ASM6);
            }

            @Override
            public void visit(String name, Object value) {
                switch (name) {
                    case "className":
                        annotatedClassType = Type.getType(getDescriptor((String) value));
                        break;
                    case "methodName":
                        annotatedMethodName = (String) value;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown name " + name + " in @Invoke*: "
                                + method);
                }
            }
        }

        private class ReturnTypeNameAnnotationVisitor extends AnnotationVisitor {

            public ReturnTypeNameAnnotationVisitor() {
                super(Opcodes.ASM6);
            }

            @Override
            public void visit(String name, Object value) {
                switch (name) {
                    case "value":
                        annotatedReturnType = Type.getType(getDescriptor((String) value));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown name " + name + " in"
                                + " @TypeName: " + method);
                }
            }
        }

        private class ParameterTypeNameAnnotationVisitor extends AnnotationVisitor {

            private int index;

            public ParameterTypeNameAnnotationVisitor(int index) {
                super(Opcodes.ASM6);

                this.index = index;
            }

            @Override
            public void visit(String name, Object value) {
                switch (name) {
                    case "value":
                        annotatedParameterTypes[index] = Type.getType(getDescriptor((String)
                                value));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown name " + name + " in"
                                + " @TypeName: " + method);
                }
            }
        }
    }
}
