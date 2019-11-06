package com.wjybxx.zset.generic;

/**
 * 存放一些常用的{@link ScoreHandler}实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/6
 */
public class ScoreHandlers {

    private ScoreHandlers() {

    }

    /**
     * @return Long类型的score处理器
     */
    public static ScoreHandler<Long> longScoreHandler() {
        return LongScoreHandler.INSTANCE;
    }

    /**
     * Long类型的score处理器
     */
    private static class LongScoreHandler implements ScoreHandler<Long> {

        private static final LongScoreHandler INSTANCE = new LongScoreHandler();

        private LongScoreHandler() {

        }

        @Override
        public int compare(Long o1, Long o2) {
            return o1.compareTo(o2);
        }

        @Override
        public Long sum(Long oldScore, Long increment) {
            return oldScore + increment;
        }
    }
}
