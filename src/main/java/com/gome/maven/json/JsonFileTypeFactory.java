package com.gome.maven.json;

import com.gome.maven.openapi.fileTypes.FileTypeConsumer;
import com.gome.maven.openapi.fileTypes.FileTypeFactory;

/**
 * @author Mikhail Golubev
 */
public class JsonFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes( FileTypeConsumer consumer) {
        consumer.consume(JsonFileType.INSTANCE, JsonFileType.DEFAULT_EXTENSION);
    }
}
