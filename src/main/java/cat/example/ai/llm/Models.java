package cat.example.ai.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 模型管理工具类
 */
public final class Models {

    private static String baseUrl;
    private static String apiKey;
    private static String model;

    static {
        load();
    }

    private static void load() {
        Properties properties = new Properties();
        Path conf = Path.of("config.properties");

        try (InputStream in = Files.newInputStream(conf)) {
            properties.load(in);
        } catch (IOException e){
            throw new IllegalStateException("配置文件加载失败", e);
        }

        baseUrl = properties.getProperty("base-url");
        apiKey = properties.getProperty("api-key");
        model = properties.getProperty("model");
    }

    public static OpenAiChatModel getChatModel() {
        return new OpenAiChatModel(baseUrl, apiKey, model);
    }

}
