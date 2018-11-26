/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import me.zhanghai.android.bytecoder.library.InvokeStatic;
import me.zhanghai.android.bytecoder.library.TypeName;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Object activityThread = ActivityThread_currentActivityThread();
        String text = "ActivityThread.currentActivityThread(): " + activityThread.toString();

        TextView textView = new TextView(this);
        textView.setText(text);
        setContentView(textView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @InvokeStatic(className = "android.app.ActivityThread", methodName = "currentActivityThread")
    @TypeName("android.app.ActivityThread")
    public static Object ActivityThread_currentActivityThread() throws LinkageError {
        return null;
    }
}
