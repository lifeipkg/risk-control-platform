package com.risk.platform.pipeline;

import com.risk.platform.model.PatrolVerdict;

/**
 * 统一巡查管线接口：事件进、PatrolVerdict 出。
 *
 * domain() 对应接入层的 Kafka topic / 网关路由键，
 * 新巡查线只需实现本接口并注册路由，不动任何基建。
 */
public interface PatrolPipeline<T> {

    /** 巡查线标识：content / message / account / fund。 */
    String domain();

    /** 执行巡查。 */
    PatrolVerdict patrol(T event);
}
