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

        private static final String LIBRARY_CLASS_NAME_PREFIX =
                "me.zhanghai.android.bytecoder.library.";
        private static final Type TYPE_GET_FIELD = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "GetField"));
        private static final Type TYPE_GET_STATIC = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "GetStatic"));
        private static final Type TYPE_PUT_FIELD = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "PutField"));
        private static final Type TYPE_PUT_STATIC = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "PutStatic"));
        private static final Type TYPE_INVOKE_CONSTRUCTOR = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "InvokeConstructor"));
        private static final Type TYPE_INVOKE_INTERFACE = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "InvokeInterface"));
        private static final Type TYPE_INVOKE_STATIC = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "InvokeStatic"));
        private static final Type TYPE_INVOKE_VIRTUAL = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "InvokeVirtual"));
        private static final Type TYPE_TYPE_NAME = Type.getType(getDescriptor(
                LIBRARY_CLASS_NAME_PREFIX + "TypeName"));

        private String method;
        private int access;
        private Type[] parameterTypes;
        private Type returnType;
        private boolean throwsLinkageError;

        private int annotatedOpcode;
        private Type annotatedClassType;
        private String annotatedMethodName;
        private String annotatedFieldName;
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
            Type annotationType = Type.getType(desc);
            if (annotationType.equals(TYPE_GET_FIELD)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.GETFIELD;
                return new BytecoderAnnotationVisitor();
            } else if (annotationType.equals(TYPE_GET_STATIC)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.GETSTATIC;
                return new BytecoderAnnotationVisitor();
            } if (annotationType.equals(TYPE_PUT_FIELD)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.PUTFIELD;
                return new BytecoderAnnotationVisitor();
            } if (annotationType.equals(TYPE_PUT_STATIC)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.PUTSTATIC;
                return new BytecoderAnnotationVisitor();
            } if (annotationType.equals(TYPE_INVOKE_CONSTRUCTOR)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.INVOKESPECIAL;
                return new BytecoderAnnotationVisitor();
            } else if (annotationType.equals(TYPE_INVOKE_INTERFACE)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.INVOKEINTERFACE;
                return new BytecoderAnnotationVisitor();
            } else if (annotationType.equals(TYPE_INVOKE_STATIC)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.INVOKESTATIC;
                return new BytecoderAnnotationVisitor();
            } else if (annotationType.equals(TYPE_INVOKE_VIRTUAL)) {
                if (hasBytecoderAnnotation()) {
                    throw new IllegalArgumentException("Method has a duplicate annotation " + desc
                            + ": " + method);
                }
                annotatedOpcode = Opcodes.INVOKEVIRTUAL;
                return new BytecoderAnnotationVisitor();
            } else if (annotationType.equals(TYPE_TYPE_NAME)) {
                return new ReturnTypeNameAnnotationVisitor();
            } else {
                return super.visitAnnotation(desc, visible);
            }
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor,
                                                          boolean visible) {
            Type annotationType = Type.getType(descriptor);
            if (annotationType.equals(TYPE_TYPE_NAME)) {
                if (annotatedParameterTypes == null) {
                    annotatedParameterTypes = new Type[parameterTypes.length];
                }
                return new ParameterTypeNameAnnotationVisitor(parameter);
            } else {
                return super.visitParameterAnnotation(parameter, descriptor, visible);
            }
        }

        @Override
        public void visitCode() {
            if (!hasBytecoderAnnotation()) {
                super.visitCode();
            }
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            if (!hasBytecoderAnnotation()) {
                super.visitFrame(type, nLocal, local, nStack, stack);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (!hasBytecoderAnnotation()) {
                super.visitInsn(opcode);
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (!hasBytecoderAnnotation()) {
                super.visitIntInsn(opcode, operand);
            }
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (!hasBytecoderAnnotation()) {
                super.visitVarInsn(opcode, var);
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (!hasBytecoderAnnotation()) {
                super.visitTypeInsn(opcode, type);
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (!hasBytecoderAnnotation()) {
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
            if (!hasBytecoderAnnotation()) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                           Object... bsmArgs) {
            if (!hasBytecoderAnnotation()) {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            if (!hasBytecoderAnnotation()) {
                super.visitJumpInsn(opcode, label);
            }
        }

        @Override
        public void visitLabel(Label label) {
            if (!hasBytecoderAnnotation()) {
                super.visitLabel(label);
            }
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (!hasBytecoderAnnotation()) {
                super.visitLdcInsn(cst);
            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            if (!hasBytecoderAnnotation()) {
                super.visitIincInsn(var, increment);
            }
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            if (!hasBytecoderAnnotation()) {
                super.visitTableSwitchInsn(min, max, dflt, labels);
            }
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            if (!hasBytecoderAnnotation()) {
                super.visitLookupSwitchInsn(dflt, keys, labels);
            }
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            if (!hasBytecoderAnnotation()) {
                super.visitMultiANewArrayInsn(desc, dims);
            }
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc,
                                                     boolean visible) {
            if (!hasBytecoderAnnotation()) {
                return super.visitInsnAnnotation(typeRef, typePath, desc, visible);
            }
            return null;
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            if (!hasBytecoderAnnotation()) {
                super.visitTryCatchBlock(start, end, handler, type);
            }
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath,
                                                         String desc, boolean visible) {
            if (!hasBytecoderAnnotation()) {
                return super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
            }
            return null;
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start,
                                       Label end, int index) {
            if (!hasBytecoderAnnotation()) {
                super.visitLocalVariable(name, desc, signature, start, end, index);
            }
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
                                                              Label[] start, Label[] end,
                                                              int[] index, String desc,
                                                              boolean visible) {
            if (!hasBytecoderAnnotation()) {
                return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index,
                        desc, visible);
            }
            return null;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (!hasBytecoderAnnotation()) {
                super.visitLineNumber(line, start);
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (!hasBytecoderAnnotation()) {
                super.visitMaxs(maxStack, maxLocals);
            }
        }

        @Override
        public void visitEnd() {
            if (!hasBytecoderAnnotation()) {
                if (annotatedParameterTypes != null) {
                    throw new IllegalArgumentException("Method has a parameter with @TypeName but"
                            + " missing annotation: " + method);
                }
                if (annotatedReturnType != null) {
                    throw new IllegalArgumentException("Method has @TypeName but missing"
                            + " annotation: " + method);
                }
                super.visitEnd();
                return;
            }

            if (annotatedClassType == null) {
                throw new IllegalArgumentException("Method must have either classConstant or"
                        + " className in its annotation");
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

            Type[] parameterTypesWithAnnotated = new Type[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; ++i) {
                parameterTypesWithAnnotated[i] = annotatedParameterTypes != null
                        && annotatedParameterTypes[i] != null ? annotatedParameterTypes[i]
                        : parameterTypes[i];
            }

            Type returnTypeWithAnnotated = annotatedReturnType != null ? annotatedReturnType
                    : returnType;

            switch (annotatedOpcode) {
                case Opcodes.GETFIELD:
                    if (parameterTypesWithAnnotated.length != 1) {
                        throw new IllegalArgumentException("Method must take an instance of the"
                                + " target class as its only parameter: " + method);
                    }
                    break;
                case Opcodes.GETSTATIC:
                    if (parameterTypesWithAnnotated.length != 0) {
                        throw new IllegalArgumentException("Method must not take any parameter: "
                                + method);
                    }
                    break;
                case Opcodes.PUTFIELD:
                    if (parameterTypesWithAnnotated.length != 2) {
                        throw new IllegalArgumentException("Method must only take an instance of"
                                + " the target class as its first parameter and the new value as"
                                + " its second parameter: " + method);
                    }
                    break;
                case Opcodes.PUTSTATIC:
                    if (parameterTypesWithAnnotated.length != 1) {
                        throw new IllegalArgumentException("Method must take the new value as its"
                                + " only parameter: " + method);
                    }
                    break;
            }
            switch (annotatedOpcode) {
                case Opcodes.GETFIELD:
                case Opcodes.PUTFIELD:
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKEVIRTUAL:
                    if (parameterTypesWithAnnotated.length < 1) {
                        throw new IllegalArgumentException("Method must take an instance of the"
                                + " target class as its first parameter: " + method);
                    }
                    if (!parameterTypesWithAnnotated[0].equals(annotatedClassType)) {
                        throw new IllegalArgumentException("Method must declare the type of its"
                                + " first parameter to be the same as the target class: " + method);
                    }
                    break;
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

            switch (annotatedOpcode) {
                case Opcodes.GETFIELD:
                case Opcodes.GETSTATIC:
                case Opcodes.PUTFIELD:
                case Opcodes.PUTSTATIC: {

                    Type targetFieldType;
                    switch (annotatedOpcode) {
                        case Opcodes.GETFIELD:
                        case Opcodes.GETSTATIC:
                            targetFieldType = returnTypeWithAnnotated;
                            break;
                        case Opcodes.PUTFIELD:
                            targetFieldType = parameterTypesWithAnnotated[1];
                            break;
                        case Opcodes.PUTSTATIC:
                            targetFieldType = parameterTypesWithAnnotated[0];
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    String targetFieldDescriptor = targetFieldType.getDescriptor();

                    mv.visitFieldInsn(annotatedOpcode, targetClassInternalName, annotatedFieldName,
                            targetFieldDescriptor);

                    break;
                }
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKEVIRTUAL: {

                    Type targetReturnType = annotatedOpcode == Opcodes.INVOKESPECIAL ?
                            Type.VOID_TYPE : returnTypeWithAnnotated;
                    Type[] targetParameterTypes;
                    switch (annotatedOpcode) {
                        case Opcodes.INVOKEINTERFACE:
                        case Opcodes.INVOKEVIRTUAL:
                            targetParameterTypes = new Type[parameterTypesWithAnnotated.length - 1];
                            System.arraycopy(parameterTypesWithAnnotated, 1, targetParameterTypes,
                                    0, targetParameterTypes.length);
                            break;
                        case Opcodes.INVOKESPECIAL:
                        case Opcodes.INVOKESTATIC:
                            targetParameterTypes = parameterTypesWithAnnotated;
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    String targetMethodDescriptor = Type.getMethodDescriptor(targetReturnType,
                            targetParameterTypes);

                    boolean targetIsInterface = annotatedOpcode == Opcodes.INVOKEINTERFACE;

                    mv.visitMethodInsn(annotatedOpcode, targetClassInternalName,
                            annotatedMethodName, targetMethodDescriptor, targetIsInterface);

                    break;
                }
            }

            int returnOpcode = returnType.getOpcode(Opcodes.IRETURN);
            mv.visitInsn(returnOpcode);
            if (returnOpcode != Opcodes.RETURN) {
                maxStack = Math.max(maxStack, 1);
            }

            mv.visitMaxs(maxStack, maxLocals);

            mv.visitEnd();
        }

        private boolean hasBytecoderAnnotation() {
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

        private class BytecoderAnnotationVisitor extends AnnotationVisitor {

            public BytecoderAnnotationVisitor() {
                super(Opcodes.ASM6);
            }

            @Override
            public void visit(String name, Object value) {
                switch (name) {
                    case "classConstant": {
                        Type classType = (Type) value;
                        if (!classType.equals(Type.VOID_TYPE)) {
                            if (annotatedClassType != null) {
                                throw new IllegalArgumentException("Method must not have both"
                                        + " classConstant and className in its annotation: "
                                        + method);
                            }
                            annotatedClassType = classType;
                        }
                        break;
                    }
                    case "className": {
                        String className = (String) value;
                        if (!className.equals("")) {
                            if (annotatedClassType != null) {
                                throw new IllegalArgumentException("Method must not have both"
                                        + " classConstant and className in its annotation: "
                                        + method);
                            }
                            annotatedClassType = Type.getType(getDescriptor(className));
                        }
                        break;
                    }
                    case "methodName":
                        annotatedMethodName = (String) value;
                        break;
                    case "fieldName":
                        annotatedFieldName = (String) value;
                        break;
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
                }
            }
        }
    }
}
