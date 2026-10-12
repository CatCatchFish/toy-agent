package cat.example.ai.agent.node;

import cat.example.ai.agent.Agent;
import cat.example.ai.llm.ChatModel;
import cat.example.ai.llm.Message;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ToolAgent extends Agent {

    class ToolAgentImpl implements ToolAgent{

        private final ChatModel model;
        private final List<Map<String, Object>> lastTrace = new ArrayList<>();

        private static final String SYSTEM_PROMPT = """
                你是一个可以调用工具的智能体。可用工具：
                - get_weather(city): 查询指定城市的实时天气
                - calculator(expression): 计算四则运算，如 12*8

                决策规则：
                - 需要工具时，只输出 JSON：{"tool": "工具名", "arguments": {"参数名": "值"}}
                - 不需要工具时，直接用自然语言回答，不要输出 JSON。
                """;


        public ToolAgentImpl(ChatModel model) {
            this.model = model;
        }

        @Override
        public String getName() {
            return "最简工具调用智能体";
        }

        @Override
        public String chat(String prompt) {
            lastTrace.clear();
            List<Message> messages =
                    new ArrayList<>();
            messages.add(Message.system(SYSTEM_PROMPT));
            messages.add(Message.user(prompt));
            // 第一轮：模型决定"要不要用工具"
            String first = model.chat(messages).trim();
            Object decision = tryParse(first);

            // 如果decision是Map类型，且tool字段存在，则说明需要工具
            if (decision instanceof Map<?, ?> map && map.containsKey("tool")) {
                // 将第一轮回答添加至上下文
                messages.add(Message.assistant(first));

                String tool = (String) map.get("tool");
                Map<String, String> arguments = (Map<String, String>) map.get("arguments");
                lastTrace.add(Map.of("type", "tool", "label", "模型发起工具调用", "detail", tool + " " + arguments));

                // 第二轮：执行工具，把结果作为新消息追加进对话再发给模型
                String observation = execute(tool, JSON.toJSONString(arguments));
                lastTrace.add(Map.of("type", "observation", "label", "工具返回结果", "detail", observation));

                messages.add(Message.user("工具 " + tool + " 返回结果: " + observation + "\n请据此回答用户。"));
                return model.chat(messages).trim();
            }
            // 不需要工具：第一轮回复就是最终答案
            return first;
        }

        private String execute(String tool, String argsJson) {
            try {
                JSONObject args = argsJson == null ? new JSONObject() : JSONObject.parseObject(argsJson);
                return switch (tool) {
                    case "get_weather" -> ReActAgent.MockWeather.query(args.getString("city"));
                    case "calculator" -> {
                        String expression = args.getString("expression");
                        Matcher m = Pattern.compile("\\s*(\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(?:\\.\\d+)?)\\s*")
                                .matcher(expression == null ? "" : expression);
                        if (!m.matches()) yield "无法解析表达式";
                        double a = Double.parseDouble(m.group(1));
                        double b = Double.parseDouble(m.group(3));
                        yield switch (m.group(2)) {
                            case "+" -> String.valueOf(a + b);
                            case "-" -> String.valueOf(a - b);
                            case "*" -> String.valueOf(a * b);
                            case "/" -> b == 0 ? "除数不能为 0" : String.valueOf(a / b);
                            default -> "不支持的运算符";
                        };
                    }
                    default -> "未知工具: " + tool;
                };
            } catch (Exception e) {
                return "工具执行失败: " + e.getMessage();
            }
        }

        private Object tryParse(String json) {
            try {
                return JSONObject.parseObject(json);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public List<Map<String, Object>> trace() {
            return List.copyOf(lastTrace);
        }
    }

}
