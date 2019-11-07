/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.zset.primitive;

/**
 * 基础分数比较器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/7
 */
public class ScoreComparators {

    private ScoreComparators() {

    }

    /**
     * 获取一个分数比较器
     *
     * @param desc 是否降序
     * @return 分数比较器
     */
    public static ScoreComparator scoreComparator(boolean desc) {
        return desc ? DescScoreComparator.INSTANCE : AscScoreComparator.INSTANCE;
    }

    /**
     * 升序比较器
     */
    private static class AscScoreComparator implements ScoreComparator {

        private static AscScoreComparator INSTANCE = new AscScoreComparator();

        @Override
        public int compare(long score1, long score2) {
            return Long.compare(score1, score2);
        }
    }

    /**
     * 降序比较器
     */
    private static class DescScoreComparator implements ScoreComparator {

        private static DescScoreComparator INSTANCE = new DescScoreComparator();

        @Override
        public int compare(long score1, long score2) {
            return Long.compare(score2, score1);
        }
    }
}