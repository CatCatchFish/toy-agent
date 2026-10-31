package cat.example.ai.agent.core.node;

import cat.example.ai.agent.Agent;
import cat.example.ai.llm.ChatModel;
import cat.example.ai.llm.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ReActAgent extends Agent {

    class ReActAgentImpl implements ReActAgent{

        private final ChatModel chatModel;

        private final List<Map<String, Object>> lastTrace = new ArrayList<>();

        private static final String SYSTEM_PROMPT = """
        你是一个使用 ReAct 模式解决问题的智能体。
        每一轮只输出一个 JSON 对象，格式二选一：
        1. 继续行动：{"thought": "...", "action": "工具名", "action_input": "参数"}
        2. 给出答案：{"thought": "...", "final": "最终答案"}
        可用工具：
        - get_weather(city): 查询城市天气
        - calculator(expression): 计算四则运算
        - get_time(): 获取当前时间
        """;

        public ReActAgentImpl(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        /** 工具注册表：本场景仅 3 个本地工具，执行器就是一个 switch。 */
        private String executeTool(String action, String input) {
            try {
                return switch (action == null ? "" : action) {
                    case "get_weather" -> MockWeather.query(input);
                    case "calculator" -> String.valueOf(calc(input));
                    case "get_time" -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    default -> "未知工具: " + action;
                };
            } catch (Exception e) {
                return "工具执行失败: " + e.getMessage();
            }
        }

        @Override
        public String getName() {
            return "ReAct 智能体（思考-行动-观察循环）";
        }

        @Override
        public String chat(String prompt) {
            lastTrace.clear();
            List<Message> messages = new ArrayList<>();
            messages.add(Message.system(SYSTEM_PROMPT));
            messages.add(Message.user(prompt));

            final int maxRounds = 10;
            for (int round = 1; round <= maxRounds; round++) {
                String reply = chatModel.chat(messages);

                // 容错：剥掉模型可能包裹的 ```json 代码块
                String json = reply.trim()
                        .replaceFirst("^```(?:json)?\\s*", "")
                        .replaceFirst("\\s*```$", "");

                String thought = jsonField(json, "thought");
                String action = jsonField(json, "action");
                String actionInput = jsonField(json, "action_input");
                String finalAnswer = jsonField(json, "final");

                // 终止：给出 final（或输出无法解析时直接透传当作答案）
                if (finalAnswer != null || action == null) {
                    lastTrace.add(Map.of("round", round, "thought", thought,
                            "final", finalAnswer != null ? finalAnswer : json));
                    return finalAnswer != null ? finalAnswer : json;
                }

                // 行动：执行工具，把「模型输出」和「观察结果」追加进对话再发给模型
                String observation = executeTool(action, actionInput);
                lastTrace.add(Map.of("round", round, "thought", thought,
                        "action", action, "action_input", actionInput, "observation", observation));
                messages.add(Message.assistant(json));
                messages.add(Message.user("观察结果: " + observation));
            }
            return "已达最大循环次数（" + maxRounds + " 次），仍未得到最终答案。";
        }

        /** 从模型的 JSON 输出中提取字符串字段，不存在则返回 null。 */
        private String jsonField(String json, String key) {
            Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
            return m.find() ? m.group(1) : null;
        }

        /** 教学用四则运算求值（仅支持 a op b）。 */
        private double calc(String expr) {
            Matcher m = Pattern.compile("\\s*(\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(?:\\.\\d+)?)\\s*").matcher(expr);
            if (!m.matches()) throw new IllegalArgumentException("无法解析表达式: " + expr);
            double a = Double.parseDouble(m.group(1));
            String op = m.group(2);
            double b = Double.parseDouble(m.group(3));
            return switch (op) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> {
                    if (b == 0) throw new ArithmeticException("除数不能为 0");
                    yield a / b;
                }
                default -> throw new IllegalArgumentException("不支持的运算符 " + op);
            };
        }

    }

    /** 本地 Mock 天气数据。 */
    final class MockWeather {
        private static final Map<String, String> DATA = Map.of(
                "北京", "晴，26℃", "上海", "多云，28℃", "广州", "雷阵雨，31℃",
                "深圳", "阵雨，30℃", "杭州", "晴，27℃", "成都", "阴，22℃");

        public static String query(String city) {
            String key = (city == null ? "" : city.trim());
            return DATA.getOrDefault(key, "未收录城市 " + key + " 的数据");
        }
    }

}
