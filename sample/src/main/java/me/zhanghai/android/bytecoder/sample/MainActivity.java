/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.sample;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;
import me.zhanghai.android.bytecoder.library.InvokeStatic;
import me.zhanghai.android.bytecoder.library.TypeName;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(this, Formatter_formatBytes(getResources(), 0, 0).toString(),
                Toast.LENGTH_LONG)
                .show();
    }

    @InvokeStatic(className = "android.text.format.Formatter", methodName = "formatBytes")
    @TypeName("android.text.format.Formatter$BytesResult")
    public static Object Formatter_formatBytes(Resources res, long sizeBytes, int flags)
            throws LinkageError {
        return null;
    }
}
