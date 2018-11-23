package com.infomaximum.database.utils;

import com.infomaximum.database.exception.DatabaseException;

import java.nio.file.Path;

public class PathUtils {

    public static void checkPath(Path path) throws DatabaseException {
        if (!path.isAbsolute()) {
            throw new DatabaseException("RocksDB-paths is not absolute.");
        }
    }
}
