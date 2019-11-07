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
 * {@link ZSet}中“score”范围描述信息 - specification模式
 */
public class ZScoreRangeSpec {
    /**
     * 最低分数
     */
    final long min;
    /**
     * 最高分数
     */
    final long max;
    /**
     * 是否去除下限
     * exclusive
     */
    final boolean minex;
    /**
     * 是否去除上限
     * exclusive
     */
    final boolean maxex;

    public ZScoreRangeSpec(long min, long max) {
        this.min = min;
        this.max = max;
        this.minex = false;
        this.maxex = false;
    }

    public ZScoreRangeSpec(long min, long max, boolean minex, boolean maxex) {
        this.min = min;
        this.max = max;
        this.minex = minex;
        this.maxex = maxex;
    }
}
