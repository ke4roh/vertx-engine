package com.redhat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceUtils {

    public static String fileContentsFromResource(String resourceName) {
        InputStream stream = ResourceUtils.class.getClassLoader().getResourceAsStream(resourceName);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)))) {
            Stream<String> lines = br.lines();

            return lines.collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException | NullPointerException e) {
            return "";
        }
    }
}
