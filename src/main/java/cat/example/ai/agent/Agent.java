package cat.example.ai.agent;

import java.util.List;
import java.util.Map;

public interface Agent {

    String getName();

    String chat(String prompt);

    /** 最近一次 chat 的执行轨迹（供页面渲染时间线），无轨迹则返回空列表。 */
    default List<Map<String, Object>> trace() {
        return List.of();
    }

}
