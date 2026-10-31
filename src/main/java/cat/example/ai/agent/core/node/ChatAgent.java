package cat.example.ai.agent.core.node;

import cat.example.ai.agent.Agent;
import cat.example.ai.llm.ChatModel;
import cat.example.ai.llm.Message;
import cat.example.ai.llm.Models;
import cat.example.ai.llm.OpenAiChatModel;

import java.util.List;

public interface ChatAgent extends Agent {

    class ChatAgentImpl implements ChatAgent{

        private final ChatModel chatModel;

        public ChatAgentImpl(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        @Override
        public String getName() {
            return "对话智能体";
        }

        @Override
        public String chat(String prompt) {
            return chatModel.chat(List.of(Message.system(prompt)));
        }
    }

    public static void main(String[] args) {
        OpenAiChatModel chatModel = Models.getChatModel();
        ChatAgentImpl chatAgent = new ChatAgentImpl(chatModel);
        System.out.println(chatAgent.chat("你好"));
    }

}
