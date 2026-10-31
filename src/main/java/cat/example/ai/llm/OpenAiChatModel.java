package cat.example.ai.llm;

import com.alibaba.fastjson2.JSON;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 最基础的ChatModel实现
 */
public class OpenAiChatModel implements ChatModel {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // http://127.0.0.1:8777/v1
    public OpenAiChatModel(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }


    @Override
    public String chat(List<Message> messages) {

        LinkedHashMap<Object, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages.stream()
                        .map(message -> Map.of("role", message.role(), "content", message.content()))
                        .toList());
        body.put("temperature", 0.5);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("模型调用失败" + response.body());
            }

            Object result = JSON.parseObject(response.body());
            Object choices = ((Map<?, ?>) result).get("choices");
            Object first = ((List<?>) choices).get(0);
            Object message = ((Map<?, ?>) first).get("message");
            return ((Map<?, ?>) message).get("content").toString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
