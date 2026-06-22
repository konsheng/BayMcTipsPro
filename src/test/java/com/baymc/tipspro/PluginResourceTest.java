package com.baymc.tipspro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the plugin metadata and default resource contract.
 */
final class PluginResourceTest {

    @Test
    void pluginMetadataDeclaresCommandAliasPermissionAndFoliaSupport() throws IOException {
        String pluginYml = Files.readString(Path.of("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.contains("main: com.baymc.tipspro.BayMcTipsProPlugin"));
        assertTrue(pluginYml.contains("folia-supported: true"));
        assertTrue(pluginYml.contains("baymctipspro:"));
        assertTrue(pluginYml.contains("- tips"));
        assertTrue(pluginYml.contains("baymctipspro.command:"));
    }

    @Test
    void defaultConfigUsesMiniMessageAnnouncementExamples() throws IOException {
        String configYml = Files.readString(Path.of("src/main/resources/config.yml"));

        assertTrue(configYml.contains("announcements:"));
        assertTrue(configYml.contains("interval-seconds: 300"));
        assertTrue(configYml.contains("<click:run_command:'/spawn'>"));
        assertTrue(configYml.contains("<hover:show_text:'点击打开官网'>"));
        assertFalse(
            configYml.lines()
                .map(String::stripLeading)
                .filter(line -> line.startsWith("-"))
                .anyMatch(line -> line.contains("&a")));
    }

    @Test
    void defaultLanguageFileContainsRuntimeMessageSections() throws IOException {
        String languageYml = Files.readString(Path.of("src/main/resources/lang/zh_CN.yml"));

        assertTrue(languageYml.contains("messages:"));
        assertTrue(languageYml.contains("no-permission:"));
        assertTrue(languageYml.contains("help:"));
        assertTrue(languageYml.contains("status:"));
        assertTrue(languageYml.contains("validation:"));
        assertTrue(languageYml.contains("logs:"));
        assertTrue(languageYml.contains("automatic-enabled:"));
    }

    @Test
    void mainJavaSourcesDoNotKeepChineseRuntimeText() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            boolean containsChinese =
                files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(PluginResourceTest::readUnchecked)
                    .anyMatch(content -> content.matches("(?s).*\\p{IsHan}.*"));

            assertFalse(containsChinese);
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
