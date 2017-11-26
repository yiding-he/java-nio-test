package com.hyd.niocomm;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author yidin
 */
public class CloseableUtils {

    public static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                // nothing to do
            }
        }
    }
}
