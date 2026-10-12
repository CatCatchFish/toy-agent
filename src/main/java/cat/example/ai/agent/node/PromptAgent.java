package cat.example.ai.agent.node;

import cat.example.ai.agent.Agent;
import cat.example.ai.llm.ChatModel;
import cat.example.ai.llm.Message;

import java.util.List;

public interface PromptAgent extends Agent {

    class PromptAgentImpl implements PromptAgent{

        private final ChatModel chatModel;

        private static final String SYSTEM_PROMPT = """
        # 角色
        你是一个技术助理，专注讲清楚 AI Agent 的原理与工程实践。

        # 背景
        学习者是正在学习智能体开发的 Java 工程师。

        # 任务与约束
        1. 回答使用简体中文，先给结论再展开。
        2. 不确定的内容如实说明，禁止编造。
        """;

        public PromptAgentImpl(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        @Override
        public String getName() {
            return "提示词智能体";
        }

        @Override
        public String chat(String prompt) {
            return chatModel.chat(List.of(Message.system(SYSTEM_PROMPT), Message.user(prompt)));
        }
    }

}
