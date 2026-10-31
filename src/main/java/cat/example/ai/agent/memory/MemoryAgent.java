package cat.example.ai.agent.memory;

import cat.example.ai.agent.Agent;
import cat.example.ai.llm.ChatModel;
import cat.example.ai.llm.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface MemoryAgent extends Agent {

    class MemoryAgentImpl implements MemoryAgent{

        private final ChatModel model;
        private final List<Message> history = new ArrayList<>();
        private volatile String summary = "";
        private final List<Map<String, Object>> lastTrace = new ArrayList<>();

        private static final String SYSTEM_PROMPT = """
                你是一个有记忆的聊天智能体。请基于"对话历史 + 压缩摘要 + 新输入"回答，
                用户提到的个人信息（如名字）要在后续对话中保持一致。
                """;

        public MemoryAgentImpl(ChatModel model){
            this.model = model;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public String chat(String prompt) {
            return "";
        }

        @Override
        public List<Map<String, Object>> trace() {
            return List.copyOf(lastTrace);
        }
    }

}
