package com.baymc.tipspro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 保护插件元数据与默认资源文件约定
 */
final class PluginResourceTest {

    /**
     * 校验插件元数据声明主类, Folia 支持, 作者, 命令别名和拆分后的权限节点
     *
     * @throws IOException 读取资源文件失败时抛出
     */
    @Test
    void pluginMetadataDeclaresCommandAliasPermissionAndFoliaSupport() throws IOException {
        String pluginYml = Files.readString(Path.of("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.contains("main: com.baymc.tipspro.BayMcTipsProPlugin"));
        assertTrue(pluginYml.contains("folia-supported: true"));
        assertTrue(pluginYml.contains("- Konsheng"));
        assertTrue(pluginYml.contains("baymctipspro:"));
        assertTrue(pluginYml.contains("- tips"));
        assertTrue(pluginYml.contains("baymctipspro.help:"));
        assertTrue(pluginYml.contains("baymctipspro.info:"));
        assertTrue(pluginYml.contains("baymctipspro.next:"));
        assertTrue(pluginYml.contains("baymctipspro.status:"));
        assertTrue(pluginYml.contains("baymctipspro.reload:"));
        assertFalse(pluginYml.contains("baymctipspro.command:"));
    }

    /**
     * 校验默认配置只保留运行时选项, 公告内容独立存放在 tips.yml
     *
     * @throws IOException 读取资源文件失败时抛出
     */
    @Test
    void defaultConfigKeepsOnlyAnnouncementRuntimeOptions() throws IOException {
        String configYml = Files.readString(Path.of("src/main/resources/config.yml"));

        assertTrue(configYml.contains("announcements:"));
        assertTrue(configYml.contains("language:"));
        assertTrue(configYml.contains("file: \"zh_CN.yml\""));
        assertTrue(configYml.contains("interval-seconds: 300"));
        assertTrue(configYml.contains("send-to-console: true"));
        assertFalse(configYml.contains("messages:"));
        assertFalse(configYml.contains("<click:run_command:'/spawn'>"));
    }

    /**
     * 校验默认公告文件使用 MiniMessage 示例并避免旧式颜色代码
     *
     * @throws IOException 读取资源文件失败时抛出
     */
    @Test
    void defaultTipsUsesMiniMessageAnnouncementExamples() throws IOException {
        String tipsYml = Files.readString(Path.of("src/main/resources/tips.yml"));

        assertTrue(tipsYml.contains("tips:"));
        assertTrue(tipsYml.contains("sound:"));
        assertTrue(tipsYml.contains("minecraft:block.note_block.pling"));
        assertTrue(tipsYml.contains("<click:run_command:'/spawn'>"));
        assertTrue(tipsYml.contains("<hover:show_text:'点击打开官网'>"));
        assertFalse(
            tipsYml.lines()
                .map(String::stripLeading)
                .filter(line -> line.startsWith("-"))
                .anyMatch(line -> line.contains("&a")));
    }

    /**
     * 校验默认配置和插件元数据使用中文文本并遵守英文标点约定
     *
     * @throws IOException 读取资源文件失败时抛出
     */
    @Test
    void defaultConfigAndPluginMetadataUseChineseResourceText() throws IOException {
        String configYml = Files.readString(Path.of("src/main/resources/config.yml"));
        String pluginYml = Files.readString(Path.of("src/main/resources/plugin.yml"));
        String tipsYml = Files.readString(Path.of("src/main/resources/tips.yml"));

        assertTrue(configYml.contains("公告列表请写入 tips.yml"));
        assertTrue(tipsYml.contains("BayMcTipsPro 只支持 MiniMessage"));
        assertTrue(pluginYml.contains("Paper/Folia 轻量级 MiniMessage 聊天栏公告插件"));
        assertTrue(pluginYml.contains("BayMcTipsPro 主命令"));
        assertTrue(pluginYml.contains("允许查看 BayMcTipsPro 命令帮助"));
        assertTrue(pluginYml.contains("允许重载 BayMcTipsPro 配置和语言文件"));
        assertFalse(configYml.contains("Legacy color codes"));
        assertFalse(pluginYml.contains("Lightweight MiniMessage chat announcement plugin"));
        assertFalse(hasChinesePunctuation(configYml + pluginYml + tipsYml));
    }

    /**
     * 校验默认语言文件保留运行时命令反馈, 状态, 校验和日志分区
     *
     * @throws IOException 读取资源文件失败时抛出
     */
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

    /**
     * 校验默认语言文件遵守中文文本, 英文标点和句末无标点约定
     *
     * @throws IOException 读取资源文件失败时抛出
     */
    @Test
    void defaultLanguageFileUsesRequiredChineseTextStyle() throws IOException {
        String languageYml = Files.readString(Path.of("src/main/resources/lang/zh_CN.yml"));

        assertFalse(
            hasChinesePunctuation(languageYml));
        assertFalse(
            languageYml.lines()
                .map(String::stripTrailing)
                .anyMatch(line -> line.matches(".*[.,;:!?](</[^>]+>)*\"$")));
    }

    /**
     * 校验运行时可见文本集中在语言文件中, Java 主源码不保留硬编码文案
     *
     * @throws IOException 遍历源码文件失败时抛出
     */
    @Test
    void mainJavaSourcesDoNotKeepRuntimeTextLiterals() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            boolean containsRuntimeText =
                files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(PluginResourceTest::readUnchecked)
                    .anyMatch(content ->
                        content.contains("\"你没有权限使用 BayMcTipsPro 命令")
                            || content.contains("\"BayMcTipsPro 配置已重载")
                            || content.contains("\"没有可用公告")
                            || content.contains("\"自动公告已启用")
                            || content.contains("\"调用调度器方法失败"));

            assertFalse(containsRuntimeText);
        }
    }

    /**
     * 读取文件内容并将受检异常包装为运行时异常
     *
     * @param path 要读取的文件路径
     * @return 文件文本内容
     */
    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * 判断文本中是否包含被禁止的中文标点符号
     *
     * @param content 要检查的文本
     * @return 存在中文标点时返回 {@code true}
     */
    private static boolean hasChinesePunctuation(String content) {
        return content.chars()
            .anyMatch(character -> "\uFF0C\u3002\uFF1B\uFF1A\u3001\uFF01\uFF1F".indexOf(character) >= 0);
    }
}
