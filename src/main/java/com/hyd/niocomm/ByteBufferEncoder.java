package com.hyd.niocomm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hyd.niocomm.server.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author yidin
 */
public class ByteBufferEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(ByteBufferEncoder.class);

    /////////////////////////////////////////////// request encoding/decoding

    public static ByteBuffer fromRequest(Request request) {
        byte[] bytes = encodeRequest(request);
        return bytes2Buffer(bytes);
    }

    public static byte[] encodeRequest(Request request) {
        long sequence = request.getSequence();
        String path = request.getPath();
        String paramJson = request.getParams() == null ? "" : request.getParams().toJSONString();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(bos);

        try {
            zipOutputStream.putNextEntry(new ZipEntry("sequence"));
            zipOutputStream.write(Bytes.longToBytes(sequence));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("path"));
            zipOutputStream.write(path.getBytes("UTF-8"));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("params"));
            zipOutputStream.write(paramJson.getBytes("UTF-8"));
            zipOutputStream.closeEntry();

            zipOutputStream.flush();
            zipOutputStream.close();
        } catch (IOException e) {
            throw new ServerException("Error encoding request", e);
        }

        byte[] zipBytes = bos.toByteArray();

        try {
            File file = new File("target/debug.zip");
            if (file.exists() || file.createNewFile()) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(zipBytes);
                }
            }
        } catch (IOException e) {
            LOG.error("", e);
        }

        return Base64.getEncoder().encode(zipBytes);
    }

    public static Request decodeRequest(byte[] pack) {
        byte[] zippedBytes = Base64.getDecoder().decode(pack);

        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedBytes));
        ZipEntry zipEntry;
        Request request = new Request();

        try {
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                byte[] entryContent = readFromInputStream(zipInputStream);
                String entryName = zipEntry.getName();

                switch (entryName) {
                    case "sequence":
                        request.setSequence(Bytes.bytesToLong(entryContent));
                        break;
                    case "path":
                        request.setPath(new String(entryContent));
                        break;
                    case "params":
                        request.setParams(parseJsonObject(entryContent));
                        break;
                }
            }

            return request;
        } catch (IOException e) {
            LOG.error("Error reading zip stream", e);
            return null;
        }
    }

    /////////////////////////////////////////////// response encoding/decoding

    public static ByteBuffer encode(Response response) {
        byte[] bytes = encodeResponse(response);
        return bytes2Buffer(bytes);
    }

    public static Response decodeResponse(byte[] pack) {
        byte[] zippedBytes = Base64.getDecoder().decode(pack);

        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedBytes));
        ZipEntry zipEntry;
        Response response = new Response();

        try {
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                byte[] entryContent = readFromInputStream(zipInputStream);
                String entryName = zipEntry.getName();

                switch (entryName) {
                    case "sequence":
                        response.setSequence(Bytes.bytesToLong(entryContent));
                        break;
                    case "resultCode":
                        response.setResultCode(Bytes.bytes2Int(entryContent));
                        break;
                    case "data":
                        response.setData(parseJsonObject(entryContent));
                        break;
                }
            }

            return response;
        } catch (IOException e) {
            LOG.error("Error reading zip stream", e);
            return null;
        }
    }

    public static byte[] encodeResponse(Response response) {
        long sequence = response.getSequence();
        int resultCode = response.getResultCode();
        String dataJson = response.getData() == null ? "" : response.getData().toJSONString();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(bos);

        try {
            zipOutputStream.putNextEntry(new ZipEntry("sequence"));
            zipOutputStream.write(Bytes.longToBytes(sequence));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("resultCode"));
            zipOutputStream.write(Bytes.int2Bytes(resultCode));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("data"));
            zipOutputStream.write(dataJson.getBytes("UTF-8"));
            zipOutputStream.closeEntry();

        } catch (IOException e) {
            throw new ServerException("Error encoding response", e);
        }

        return Base64.getEncoder().encode(bos.toByteArray());
    }

    /////////////////////////////////////////////// base functions

    private static byte[] readFromInputStream(InputStream inputStream) throws IOException {
        byte[] content = new byte[0];
        byte[] buffer = new byte[4096];
        int count;

        while ((count = inputStream.read(buffer)) != -1) {
            byte[] newContent = new byte[content.length + count];
            System.arraycopy(content, 0, newContent, 0, content.length);
            System.arraycopy(buffer, 0, newContent, content.length, count);
            content = newContent;
        }

        return content;
    }

    private static ByteBuffer bytes2Buffer(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 1);
        byteBuffer.put(bytes);
        byteBuffer.put((byte) '\n');
        return byteBuffer;
    }

    private static JSONObject parseJsonObject(byte[] entryContent) {
        try {
            return JSON.parseObject(new String(entryContent, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOG.error("Error parsing json", e);
            return new JSONObject();
        }
    }

    public static void readFromKey(
            SelectionKey key, ByteBuffer readBuffer,
            ByteArrayOutputStream bos, Consumer<byte[]> packetProcessor
    ) throws IOException {

        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
        } catch (IOException e) {
            LOG.error("", e);
            key.cancel();
            socketChannel.close();
            return;
        }

        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            key.channel().close();
            key.cancel();
        } else {
            processReadBuffer(readBuffer, bos, packetProcessor);
        }

    }

    private static void processReadBuffer(
            ByteBuffer readBuffer,
            ByteArrayOutputStream bos,
            Consumer<byte[]> packetProcessor
    ) {

        readBuffer.flip();

        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b != '\n') {
                bos.write(b);
            } else {
                if (packetProcessor != null) {
                    packetProcessor.accept(bos.toByteArray());
                }
                bos.reset();
            }
        }

        readBuffer.clear();
    }

}
