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

package com.wjybxx.zset.object2long;

/**
 * zset中单个成员信息
 */
public class Object2LongMember<K> {

    private final K member;
    private final long score;

    Object2LongMember(K member, long score) {
        this.member = member;
        this.score = score;
    }

    public K getMember() {
        return member;
    }

    public long getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "{" +
                "member=" + member +
                ", score=" + score +
                '}';
    }
}
