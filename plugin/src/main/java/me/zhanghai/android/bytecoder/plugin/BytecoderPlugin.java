/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.FeatureExtension;
import com.android.build.gradle.FeaturePlugin;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.TestExtension;
import com.android.build.gradle.TestPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;

import javax.annotation.Nonnull;

public class BytecoderPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project target) {
        BaseExtension extension = getAndroidExtension(target);
        if (extension == null) {
            return;
        }
        extension.registerTransform(new BytecoderTransform());
    }

    private static BaseExtension getAndroidExtension(@Nonnull Project project) {
        PluginContainer plugins = project.getPlugins();
        if (plugins.hasPlugin(AppPlugin.class)) {
            return project.getExtensions().getByType(AppExtension.class);
        } else if (plugins.hasPlugin(FeaturePlugin.class)) {
            return project.getExtensions().getByType(FeatureExtension.class);
        // InstantAppExtension is not public.
        //} else if (plugins.hasPlugin(InstantAppPlugin.class)) {
        //    return project.getExtensions().getByType(InstantAppExtension.class);
        } else if (plugins.hasPlugin(LibraryPlugin.class)) {
            return project.getExtensions().getByType(LibraryExtension.class);
        } else if (plugins.hasPlugin(TestPlugin.class)) {
            return project.getExtensions().getByType(TestExtension.class);
        } else {
            return null;
        }
    }
}
