/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.bytecoder.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class BytecoderTransform extends Transform {

    @Override
    public String getName() {
        return "Bytecoder";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return Collections.singleton(QualifiedContent.DefaultContentType.CLASSES);
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return Collections.singleton(QualifiedContent.Scope.PROJECT);
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws IOException {

        Path outputDirectory = transformInvocation.getOutputProvider().getContentLocation(getName(),
                getOutputTypes(), getScopes(), Format.DIRECTORY).toPath();
        if (!transformInvocation.isIncremental()) {
            deleteRecursivelyIfExists(outputDirectory);
            Files.createDirectories(outputDirectory);
        }

        for (TransformInput input : transformInvocation.getInputs()) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                Path inputDirectory = directoryInput.getFile().toPath();
                if (transformInvocation.isIncremental()) {
                    for (Map.Entry<File, Status> fileStatusEntry
                            : directoryInput.getChangedFiles().entrySet()) {
                        Path path = fileStatusEntry.getKey().toPath();
                        Status status = fileStatusEntry.getValue();
                        switch (status) {
                            case NOTCHANGED:
                                break;
                            case ADDED:
                            case CHANGED:
                                transformPath(path, inputDirectory, outputDirectory);
                                break;
                            case REMOVED:
                                deleteRecursivelyIfExists(path);
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                    }
                } else {
                    transformPath(inputDirectory, inputDirectory, outputDirectory);
                }
            }
        }
    }

    private void transformPath(Path path, Path inputDirectory, Path outputDirectory)
            throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                Path directoryInOutput = outputDirectory.resolve(inputDirectory.relativize(
                        directory));
                Files.createDirectories(directoryInOutput);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path fileInOutput = outputDirectory.resolve(inputDirectory.relativize(file));
                BytecoderClassTranformer.transform(file, fileInOutput);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteRecursivelyIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception)
                    throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
