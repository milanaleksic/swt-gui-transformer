package net.milanaleksic.baobab.util;

import net.milanaleksic.baobab.TransformerException;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import java.lang.reflect.*;
import java.util.concurrent.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * User: Milan Aleksic
 * Based on idea from ReflectASM (http://code.google.com/p/reflectasm/)
 */
public abstract class WidgetCreator<T> {

    private static ConcurrentMap<Class<?>, WidgetCreator<?>> cachedCreatorMap = new ConcurrentHashMap<>();

    public abstract T newInstance(Object parent, int style);

    @SuppressWarnings("unchecked")
    public static <T> WidgetCreator<T> get(Class<T> type) {
        WidgetCreator widgetCreator = cachedCreatorMap.get(type);
        if (widgetCreator != null)
            return (WidgetCreator<T>) widgetCreator;

        final Type targetType = Type.getType(type);
        final Type targetCreatorType = Type.getType("L" + targetType.getInternalName() + "Creator;");
        final Type thisType = Type.getType(WidgetCreator.class);

        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, targetCreatorType.getInternalName(), null,
                thisType.getInternalName(), null);
        insertConstructor(cw, thisType);
        insertNewInstance(cw, targetType, findAppropriateSWTStyledConstructor(type));
        cw.visitEnd();

//        debug-start
//        try (FileOutputStream outputStream = new FileOutputStream("e:/temp/test.class")) {
//            outputStream.write(cw.toByteArray());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        debug-end

        Class<?> classDefinition = ObjectUtil.defineClass(targetCreatorType.getClassName(), cw.toByteArray());
        try {
            WidgetCreator<T> creator = (WidgetCreator<T>) classDefinition.newInstance();
            WidgetCreator<T> previousCreator = (WidgetCreator<T>) cachedCreatorMap.putIfAbsent(type, creator);
            return previousCreator == null ? creator : previousCreator;
        } catch (Exception ex) {
            throw new RuntimeException("Error constructing constructor access class: " + targetCreatorType.getClassName(), ex);
        }
    }

    private static Constructor<?> findAppropriateSWTStyledConstructor(Class<?> widgetClass) {
        Constructor<?> defaultConstructor = null;
        Constructor<?>[] constructors = widgetClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 2) {
                if ((Composite.class.isAssignableFrom(parameterTypes[0]) ||   // most cases
                        Menu.class.isAssignableFrom(parameterTypes[0]) ||        // in case of MenuItems
                        Control.class.isAssignableFrom(parameterTypes[0])) &&   // in case of DropTarget
                        parameterTypes[1].equals(int.class)) {
                    return constructor;
                }
            } else if (parameterTypes.length == 0)
                defaultConstructor = constructor;
        }
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 2) {
                if (Device.class.isAssignableFrom(parameterTypes[0])   // in case of Cursor
                        && parameterTypes[1].equals(int.class)) {
                    return constructor;
                }
            }
        }
        if (defaultConstructor != null)
            return defaultConstructor;
        throw new TransformerException("Could not find adequate default constructor or constructor of type " +
                "(? extends {Device,Composite,Menu,Control}, int) in class " + widgetClass.getName());
    }

    private static void insertConstructor(ClassWriter cw, Type thisType) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, thisType.getInternalName(), "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void insertNewInstance(ClassWriter cw, Type targetType, Constructor<?> chosenConstructor) {
        final Class<?>[] parameterTypes = chosenConstructor.getParameterTypes();
        if (parameterTypes.length == 0) {
            insertZeroParameterConstructor(cw, targetType.getInternalName());
            return;
        }
        final String firstParameterInternal = ObjectUtil.getInternalNameForClass(parameterTypes[0].getName());
        if (Device.class.isAssignableFrom(parameterTypes[0]))
            insertTwoParameterConstructorWithDisplay(cw, targetType.getInternalName(), firstParameterInternal);
        else
            insertTwoParameterConstructor(cw, targetType.getInternalName(), firstParameterInternal);
    }

    private static void insertZeroParameterConstructor(ClassWriter cw, String classNameInternal) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "newInstance", "(Ljava/lang/Object;I)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, classNameInternal);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, classNameInternal, "<init>", "()V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();
    }

    private static void insertTwoParameterConstructor(ClassWriter cw, String classNameInternal, String firstParameterInternal) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "newInstance", "(Ljava/lang/Object;I)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, firstParameterInternal);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitTypeInsn(NEW, classNameInternal);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, classNameInternal, "<init>", "(L" + firstParameterInternal + ";I)V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 4);
        mv.visitEnd();
    }

    private static void insertTwoParameterConstructorWithDisplay(ClassWriter cw, String classNameInternal, String firstParameterInternal) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "newInstance", "(Ljava/lang/Object;I)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNONNULL, l0);
        mv.visitTypeInsn(NEW, "net/milanaleksic/baobab/TransformerException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Null parent widget detected!");
        mv.visitMethodInsn(INVOKESPECIAL, "net/milanaleksic/baobab/TransformerException", "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);
        mv.visitLabel(l0);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, "org/eclipse/swt/widgets/Widget");
        mv.visitVarInsn(ASTORE, 3);
        mv.visitTypeInsn(NEW, classNameInternal);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/swt/widgets/Widget", "getDisplay", "()Lorg/eclipse/swt/widgets/Display;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, classNameInternal, "<init>", "(L" + firstParameterInternal + ";I)V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 4);
    }

}
