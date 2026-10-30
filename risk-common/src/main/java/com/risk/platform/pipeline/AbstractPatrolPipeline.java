package com.risk.platform.pipeline;

import com.risk.platform.model.Decision;
import com.risk.platform.model.PatrolVerdict;

import java.util.Map;

/**
 * 巡查管线模板（Template Method）：固定同构 Pipeline 骨架，子类只实现各阶段钩子。
 *
 * 统一骨架（与平台架构图中"巡查引擎层"一致）：
 *   归一化 -> 特征提取 -> 检测(主通道+兜底通道) -> 决策
 *
 * 各线实例：
 *   内容巡查(content-patrol-service)：归一化正文 -> 3-gram 特征 -> SimHash+分段索引 -> 查重决策
 *   消息巡查(message-patrol-service)：变体归一化   -> 关键词命中 -> AC 自动机   -> 角色分级决策
 *   账号巡查(account-patrol-service)：事实归一     -> 特征事实化 -> 规则引擎   -> 分级决策
 *   资金巡查(fund-patrol-service)：    事实归一     -> 特征事实化 -> 规则引擎   -> 分层决策
 */
public abstract class AbstractPatrolPipeline<T> implements PatrolPipeline<T> {

    @Override
    public final PatrolVerdict patrol(T event) {
        String normalized = normalize(event);
        Map<String, Object> features = extract(normalized, event);
        Detection detection = detect(features, event);
        return decide(event, detection);
    }

    /** 阶段1：归一化（文本归一/格式清洗），各线自定义。 */
    protected abstract String normalize(T event);

    /** 阶段2：特征提取（n-gram、关键词命中、行为序列等），产出统一特征视图。 */
    protected abstract Map<String, Object> extract(String normalized, T event);

    /** 阶段3：检测器（可多个通道融合），产出原始检测结论。 */
    protected abstract Detection detect(Map<String, Object> features, T event);

    /** 阶段4：决策钩子，默认透传检测结论；子类可覆写做白名单降级/角色分级等业务规则。 */
    protected PatrolVerdict decide(T event, Detection detection) {
        return new PatrolVerdict(eventIdOf(event), detection.decision(), detection.score(),
                detection.reason(), Map.of());
    }

    /** 事件 ID 抽取（幂等键）。 */
    protected abstract String eventIdOf(T event);

    /** 检测器原始结论。 */
    public record Detection(Decision decision, int score, String reason) {
    }
}
