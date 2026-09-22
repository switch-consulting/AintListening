package de.switchconsulting.elfpatcher

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ElfPatcherTask extends DefaultTask {

    @OutputDirectory
    File outputDir

    @InputFiles
    Object classpathFiles

    @Internal
    Object runtimeConfiguration

    @TaskAction
    void patch() {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        def config = runtimeConfiguration instanceof Provider ? runtimeConfiguration.get() : runtimeConfiguration
        Configuration classpath = (Configuration) config
        for (def artifact : classpath.incoming.artifacts.artifacts) {
            def id = artifact.id.componentIdentifier
            boolean matches = false
            if (id.hasProperty('group')) {
                String group = id.group
                if (group == 'ai.djl.android' || group == 'com.github.EberronBruce') {
                    matches = true
                }
            } else if (id.toString().contains('ai.djl.android') || id.toString().contains('WhisperCore_Android')) {
                matches = true
            }

            if (matches) {
                def jarFile = artifact.file
                if (jarFile.exists()) {
                    for (File file : project.zipTree(jarFile)) {
                        if (file.name.endsWith('.so') && file.path.contains('arm64-v8a')) {
                            def destFile = new File(outputDir, file.name)
                            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                            patchElf64File(destFile)
                        }
                    }
                }
            }
        }
    }

    private static void patchElf64File(File file) {
        RandomAccessFile raf = new RandomAccessFile(file, "rw")
        try {
            if (raf.length() < 64) return
            
            byte[] magic = new byte[4]
            raf.readFully(magic)
            if ((magic[0] & 0xFF) != 0x7F || (magic[1] & 0xFF) != (int)'E' || (magic[2] & 0xFF) != (int)'L' || (magic[3] & 0xFF) != (int)'F') {
                return
            }
            
            int elfClass = raf.read()
            int elfData = raf.read()
            if (elfClass != 2) {
                return
            }
            
            boolean isLittleEndian = (elfData == 1)
            
            raf.seek(32)
            long phoff = readLongElf(raf, isLittleEndian)
            
            raf.seek(54)
            int phentsize = readShortElf(raf, isLittleEndian)
            int phnum = readShortElf(raf, isLittleEndian)
            
            for (int i = 0; i < phnum; i++) {
                long offset = phoff + (i * phentsize)
                raf.seek(offset)
                
                int p_type = readIntElf(raf, isLittleEndian)
                if (p_type == 1) {
                    raf.seek(offset + 48)
                    long p_align = readLongElf(raf, isLittleEndian)
                    
                    if (p_align < 16384) {
                        raf.seek(offset + 48)
                        writeLongElf(raf, 16384L, isLittleEndian)
                    }
                }
            }
        } finally {
            raf.close()
        }
    }

    private static long readLongElf(RandomAccessFile raf, boolean isLittleEndian) {
        byte[] b = new byte[8]
        raf.readFully(b)
        if (isLittleEndian) {
            return ((b[7] & 0xFFL) << 56) |
                   ((b[6] & 0xFFL) << 48) |
                   ((b[5] & 0xFFL) << 40) |
                   ((b[4] & 0xFFL) << 32) |
                   ((b[3] & 0xFFL) << 24) |
                   ((b[2] & 0xFFL) << 16) |
                   ((b[1] & 0xFFL) << 8)  |
                   (b[0] & 0xFFL)
        } else {
            return ((b[0] & 0xFFL) << 56) |
                   ((b[1] & 0xFFL) << 48) |
                   ((b[2] & 0xFFL) << 40) |
                   ((b[3] & 0xFFL) << 32) |
                   ((b[4] & 0xFFL) << 24) |
                   ((b[5] & 0xFFL) << 16) |
                   ((b[6] & 0xFFL) << 8)  |
                   (b[7] & 0xFFL)
        }
    }

    private static int readIntElf(RandomAccessFile raf, boolean isLittleEndian) {
        byte[] b = new byte[4]
        raf.readFully(b)
        if (isLittleEndian) {
            return ((b[3] & 0xFF) << 24) |
                   ((b[2] & 0xFF) << 16) |
                   ((b[1] & 0xFF) << 8)  |
                   (b[0] & 0xFF)
        } else {
            return ((b[0] & 0xFF) << 24) |
                   ((b[1] & 0xFF) << 16) |
                   ((b[2] & 0xFF) << 8)  |
                   (b[3] & 0xFF)
        }
    }

    private static int readShortElf(RandomAccessFile raf, boolean isLittleEndian) {
        byte[] b = new byte[2]
        raf.readFully(b)
        if (isLittleEndian) {
            return ((b[1] & 0xFF) << 8) | (b[0] & 0xFF)
        } else {
            return ((b[0] & 0xFF) << 8) | (b[1] & 0xFF)
        }
    }

    private static void writeLongElf(RandomAccessFile raf, long v, boolean isLittleEndian) {
        byte[] b = new byte[8]
        if (isLittleEndian) {
            b[0] = (byte)(v & 0xFF)
            b[1] = (byte)((v >> 8) & 0xFF)
            b[2] = (byte)((v >> 16) & 0xFF)
            b[3] = (byte)((v >> 24) & 0xFF)
            b[4] = (byte)((v >> 32) & 0xFF)
            b[5] = (byte)((v >> 40) & 0xFF)
            b[6] = (byte)((v >> 48) & 0xFF)
            b[7] = (byte)((v >> 56) & 0xFF)
        } else {
            b[0] = (byte)((v >> 56) & 0xFF)
            b[1] = (byte)((v >> 48) & 0xFF)
            b[2] = (byte)((v >> 40) & 0xFF)
            b[3] = (byte)((v >> 32) & 0xFF)
            b[4] = (byte)((v >> 24) & 0xFF)
            b[5] = (byte)((v >> 16) & 0xFF)
            b[6] = (byte)((v >> 8) & 0xFF)
            b[7] = (byte)(v & 0xFF)
        }
        raf.write(b)
    }
}
