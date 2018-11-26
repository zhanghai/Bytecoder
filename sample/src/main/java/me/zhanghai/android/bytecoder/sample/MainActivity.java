/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import me.zhanghai.android.bytecoder.library.InvokeStatic;
import me.zhanghai.android.bytecoder.library.TypeName;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Object activityThread = ActivityThread_currentActivityThread();
        String text = "ActivityThread.currentActivityThread(): " + activityThread.toString();
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    @InvokeStatic(className = "android.app.ActivityThread", methodName = "currentActivityThread")
    @TypeName("android.app.ActivityThread")
    public static Object ActivityThread_currentActivityThread() throws LinkageError {
        return null;
    }
}
