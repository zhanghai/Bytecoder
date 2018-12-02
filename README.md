# Bytecoder

An Android gradle plugin to generate bytecode for accessing any method or field.

## Why Bytecoder?

Using reflection to access hidden API on Android is cumbersome. To reflect properly, one need to have at least:

- static fields to cache the reflected class and method/field
- static empty objects as private locks to guard each of them
- code to handle all the exceptions thrown by the reflection API.

And by putting all these together, it can take over 20 lines of code to properly access a hidden API with reflection.

Compiling against a modified `android.jar` can work. But still, this requires one to generate it from the AOSP source, or retrieving a prebuilt one from some "trusted" source. And even if it's done properly, there can still be problems when a hidden API changed its signature across various platform versions, and to maintain compatibility one will be forced to resort to reflection again.

So I created Bytecoder.

The Bytecoder plugin works on the bytecode level. It looks at your annotations on stub methods, and magically transforms them into real implementations that calls the hidden APIs. Its output is the same as if actually compiled with the hidden APIs present, but the process is much easier and faster.

## Integration

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'me.zhanghai.android.bytecoder:plugin:1.0.0'
    }
}
apply plugin: 'me.zhanghai.android.bytecoder'

dependencies {
    // These annotations are removed after transformation.
    compileOnly 'me.zhanghai.android.bytecoder:library:1.0.0'
}
```

## Usage

This plugin works by creating stub methods with annotations that specify the hidden API to access, and callers can just use those stub methods directly whose implementation will be generated when transformed by the plugin.

### Method access

[`@InvokeConstructor`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/InvokeConstructor.java), [`@InvokeInterface`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/InvokeInterface.java), [`@InvokeStatic`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/InvokeStatic.java) and [`@InvokeVirtual`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/InvokeVirtual.java) are the annotations for accessing a hidden public method. Take `@InvokeStatic` as an example:

```java
@InvokeStatic(className = "android.app.ActivityThread", methodName = "currentActivityThread")
@TypeName("android.app.ActivityThread")
public static Object ActivityThread_currentActivityThread() throws LinkageError {
    return null;
}
```

The method definition above will be transformed to be calling `android.app.ActivityThread.currentActivityThread()` in the bytecode, by replacing the method body (`return null;`) with an `invokestatic` bytecode instruction (and some others), so that callers can just call it as if calling the hidden API directly.

You might have noticed the [`@TypeName`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/TypeName.java) annotation. This annotation solves the problem that sometimes the Android framework can mark an entire class as hidden, making the class definition unavailable at compile time. In this case, one can use the `@TypeName` annotation to tell the plugin what the actual type of a parameter or return value should be, and just use a plain `Object` in its place.

### Field access

[`@GetField`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/GetField.java), [`@GetStatic`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/GetStatic.java), [`@PutField`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/PutField.java) and [`@PutStatic`](https://github.com/DreaminginCodeZH/Bytecoder/blob/master/library/src/main/java/me/zhanghai/android/bytecoder/library/PutStatic.java) are the annotations for accessing a hidden public field. And here is a simple example for `@GetField`.

```java
@GetStatic(classConstant = AppOpsManager.class, fieldName = "OP_NONE")
public static int AppOpsManager_getOpNone() throws LinkageError {
    return null;
}
```

You can always check out the Javadoc for the annotation you are using. And in case something went wrong, the plugin will also try to detect the missing parts and report it in the build output.

## License

    Copyright 2018 Hai Zhang

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
