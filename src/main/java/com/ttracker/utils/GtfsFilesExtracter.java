package com.ttracker.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;

@Component
public class GtfsFilesExtracter {
    
    public void extract(Path downloadpath, Path targetDirectory) throws IOException{
    Files.createDirectories(targetDirectory);

        try (ZipInputStream zip =
                     new ZipInputStream(Files.newInputStream(downloadpath))) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {

                Path file = targetDirectory.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(file);
                } else {
                    Files.createDirectories(file.getParent());

                    Files.copy(
                            zip,
                            file,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                zip.closeEntry();
            }
        }
    }
}
