package com.redhat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtils {
    static Logger log = LoggerFactory.getLogger(ResourceUtils.class);

    public static String fileContentsFromResource(String resourceName) {
        InputStream stream = ResourceUtils.class.getClassLoader().getResourceAsStream(resourceName);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)))) {
            Stream<String> lines = br.lines();

            return lines.collect(Collectors.joining());
        } catch (IOException | NullPointerException e) {
            log.error("Error reading '" + resourceName + "'", e);
            return null;
        }
    }
}
