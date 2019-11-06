package com.wjybxx.zset;


import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * 基本类型的sorted set - 参考redis的zset实现
 * <b>成员和分数</b>
 * 有序集合里面的成员是不能重复的都是唯一的，但是，不同成员间有可能有相同的分数。
 * 当多个成员有相同的分数时，他们将是有序的字典（ordered lexicographically）（仍由分数作为第一排序条件，然后，相同分数的成员按照字典规则相对排序）。
 * <p>
 * <b>NOTE</b>：ZSET中的排名从0开始
 *
 * <p>
 * 这里只实现了jedis zset中的几个常用的接口，扩展不是太麻烦，可以自己根据需要实现。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/4
 */
@NotThreadSafe
public class ZSet {

    /**
     * obj -> score
     */
    private final Map<Long, Long> dict = new HashMap<>(128);
    /**
     * scoreList
     */
    private final SkipList zsl;

    public ZSet() {
        this.zsl = new SkipList();
    }

    /**
     * @return zset中的元素数量
     */
    public int count() {
        return zsl.length();
    }

    // -------------------------------------------------------- insert -----------------------------------------------

    /**
     * 往有序集合中新增一个元素。如果元素存在，则更新他的值。
     *
     * @param score  数据的评分
     * @param member 成员id
     */
    public void zadd(final long score, final long member) {
        final Long oldScore = dict.put(member, score);
        if (oldScore != null) {
            // 这里小心，score是基本类型，因此oldScore会自动拆箱，因此可以 == 比较，否则不能 == 比较
            if (oldScore != score) {
                zsl.zslDelete(oldScore, member);
                zsl.zslInsert(score, member);
            }
        } else {
            zsl.zslInsert(score, member);
        }
    }

    /**
     * 增加指定元素对应的值，如果指定元素不存在，则假设之前的值是0。
     *
     * @param increment 要增加的值
     * @param member    成员id
     * @return 新值
     */
    public long zincrby(long increment, long member) {
        final Long oldScore = dict.get(member);
        final long score = (oldScore == null ? increment : (increment + oldScore));
        zadd(score, member);
        return score;
    }

    // -------------------------------------------------------- remove -----------------------------------------------

    /**
     * 删除指定元素
     *
     * @param member 成员id
     * @return 如果成员存在，则返回true，否则返回false
     */
    public boolean zrem(long member) {
        final Long oldScore = dict.remove(member);
        if (oldScore != null) {
            zsl.zslDelete(oldScore, member);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 移除zset中所有score值介于min和max之间(包括等于min或max)的成员
     *
     * @param min 最低分 inclusive
     * @param max 最高分 inclusive
     * @return 删除的元素数目
     */
    public int zremrangeByScore(long min, long max) {
        return zremrangeByScore(new ZScoreRangeSpec(min, max));
    }

    /**
     * 移除zset中所有score值在范围描述期间的成员
     *
     * @param spec score范围区间
     * @return 删除的元素数目
     */
    public int zremrangeByScore(ZScoreRangeSpec spec) {
        return zsl.zslDeleteRangeByScore(spec, dict);
    }

    /**
     * 删除指定排名范围的全部元素，start和end都是从0开始的。
     * 排名0表示分数最小的元素。
     * start和end都可以是负数，此时它们表示从最高排名元素开始的偏移量，eg: -1表示最高排名的元素， -2表示第二高分的元素，以此类推。
     * <p>
     * Remove all elements in the sorted set at key with rank between start and end. Start and end are
     * 0-based with rank 0 being the element with the lowest score. Both start and end can be negative
     * numbers, where they indicate offsets starting at the element with the highest rank. For
     * example: -1 is the element with the highest score, -2 the element with the second highest score
     * and so forth.
     * <p>
     * <b>Time complexity:</b> O(log(N))+O(M) with N being the number of elements in the sorted set
     * and M the number of elements removed by the operation
     *
     * @param start 起始排名
     * @param end   截止排名
     * @return 删除的元素数目
     */
    public int zremrangeByRank(int start, int end) {
        final int zslLength = zsl.length();

        start = convertStartRank(start, zslLength);
        end = convertEndRank(end, zslLength);

        if (isRankRangeEmpty(start, end, zslLength)) {
            return 0;
        }

        return zsl.zslDeleteRangeByRank(start + 1, end + 1, dict);
    }

    /**
     * 转换起始排名
     *
     * @param start     请求参数中的起始排名
     * @param zslLength 跳表的长度
     * @return 有效起始排名
     */
    private static int convertStartRank(int start, int zslLength) {
        if (start < 0) {
            start += zslLength;
        }
        if (start < 0) {
            start = 0;
        }
        return start;
    }

    /**
     * 转换截止排名
     *
     * @param end       请求参数中的截止排名
     * @param zslLength 跳表的长度
     * @return 有效截止排名
     */
    private static int convertEndRank(int end, int zslLength) {
        if (end < 0) {
            end += zslLength;
        }
        if (end >= zslLength) {
            end = zslLength - 1;
        }
        return end;
    }

    /**
     * 判断排名区间是否为空
     *
     * @param start     转换后的起始排名
     * @param end       转换后的截止排名
     * @param zslLength 跳表长度
     * @return true/false
     */
    private static boolean isRankRangeEmpty(final int start, final int end, final int zslLength) {
        /* Invariant: start >= 0, so this test will be true when end < 0.
         * The range is empty when start > end or start >= length. */
        return start > end || start >= zslLength;
    }
    // -------------------------------------------------------- query -----------------------------------------------

    /**
     * 返回有序集key中成员member的排名。其中有序集成员按score值递增(从小到大)顺序排列。
     * 返回的排名从0开始(0-based)，也就是说，score值最小的成员排名为0。
     * 使用{@link #zrevrank(long)}可以获得成员按score值递减(从大到小)排列的排名。
     *
     * <b>与redis的区别</b>：我们使用-1表示成员不存在，而不是返回null。
     *
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))
     *
     * @param member 成员id
     * @return 如果存在该成员，则返回该成员的排名，否则返回-1
     */
    public int zrank(long member) {
        final Long score = dict.get(member);
        if (score == null) {
            return -1;
        }
        // zslGetRank 一定大于0
        return zsl.zslGetRank(score, member) - 1;
    }

    /**
     * 返回有序集key中成员member的排名，其中有序集成员按score值从大到小排列。
     * 返回的排名从0开始(0-based)，也就是说，score值最大的成员排名为0。
     * 使用{@link #zrank(long)}可以获得成员按score值递增(从小到大)排列的排名。
     *
     * <b>与redis的区别</b>：我们使用-1表示成员不存在，而不是返回null。
     *
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))
     *
     * @param member 成员id
     * @return 如果存在该成员，则返回该成员的排名，否则返回-1
     */
    public int zrevrank(long member) {
        final Long score = dict.get(member);
        if (score == null) {
            return -1;
        }
        // zslGetRank 一定大于0
        return count() - zsl.zslGetRank(score, member);
    }

    /**
     * 查询member的分数。
     * 如果分数不存在，则返回null - 这里返回任意的基础值都是不合理的，因此必须返回null。
     *
     * @param member 成员id
     * @return score
     */
    public Long zscore(long member) {
        return dict.get(member);
    }

    /**
     * 返回zset中指定分数区间内的成员，分数由低到高
     *
     * @param minScore 最低分数 inclusive
     * @param maxScore 最高分数 inclusive
     * @return memberInfo
     */
    public List<Member> zrangeByScore(long minScore, long maxScore) {
        return zrangeByScoreWithOptions(new ZScoreRangeSpec(minScore, maxScore), 0, -1, false);
    }

    /**
     * 返回zset中指定分数区间内的成员，分数由高到低排序
     *
     * @param minScore 最低分数 inclusive
     * @param maxScore 最高分数 inclusive
     * @return memberInfo
     */
    public List<Member> zrevrangeByScore(final long minScore, final long maxScore) {
        return zrangeByScoreWithOptions(new ZScoreRangeSpec(minScore, maxScore), 0, -1, true);
    }

    /**
     * 返回zset中指定分数区间内的成员，并按照指定顺序返回
     *
     * @param range   score范围描述信息
     * @param offset  偏移量(用于分页)  大于等于0
     * @param limit   返回的元素数量(用于分页) 小于0表示不限制
     * @param reverse 是否逆序
     * @return memberInfo
     */
    public List<Member> zrangeByScoreWithOptions(final ZScoreRangeSpec range, int offset, int limit, boolean reverse) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset" + ": " + offset + " (expected: >= 0)");
        }

        SkipListNode listNode;
        /* If reversed, get the last node in range as starting point. */
        if (reverse) {
            listNode = zsl.zslLastInRange(range);
        } else {
            listNode = zsl.zslFirstInRange(range);
        }

        /* No "first" element in the specified interval. */
        if (listNode == null) {
            return new ArrayList<>();
        }

        /* If there is an offset, just traverse the number of elements without
         * checking the score because that is done in the next loop. */
        while (listNode != null && offset-- != 0) {
            if (reverse) {
                listNode = listNode.backward;
            } else {
                listNode = listNode.levelInfo[0].forward;
            }
        }

        final List<Member> result = new ArrayList<>();

        /* 这里使用 != 0 判断，当limit小于0时，表示不限制 */
        while (listNode != null && limit-- != 0) {
            /* Abort when the node is no longer in range. */
            if (reverse) {
                if (!SkipList.zslValueGteMin(listNode.score, range)) {
                    break;
                }
            } else {
                if (!SkipList.zslValueLteMax(listNode.score, range)) {
                    break;
                }
            }

            result.add(new Member(listNode.obj, listNode.score));

            /* Move to next node */
            if (reverse) {
                listNode = listNode.backward;
            } else {
                listNode = listNode.levelInfo[0].forward;
            }
        }
        return result;
    }

    /**
     * 查询指定排名区间的成员id和分数，结果排名由低到高。
     * start和end都是从0开始的。
     * 排名0表示分数最小的元素。
     * start和end都可以是负数，此时它们表示从最高排名元素开始的偏移量，eg: -1表示最高排名的元素， -2表示第二高分的元素，以此类推。
     *
     * @param start 起始排名 inclusive
     * @param end   截止排名 inclusive
     * @return memberInfo
     */
    public List<Member> zrangeByRank(int start, int end) {
        return zrangeByRankInternal(start, end, false);
    }

    /**
     * 查询指定排名区间的成员id和分数，结果排名由高到低。
     * start和end都是从0开始的。
     * 排名0表示分数最小的元素。
     * start和end都可以是负数，此时它们表示从最高排名元素开始的偏移量，eg: -1表示最高排名的元素， -2表示第二高分的元素，以此类推。
     *
     * @param start 起始排名 inclusive
     * @param end   截止排名 inclusive
     * @return memberInfo
     */
    public List<Member> zrevrangeByRank(int start, int end) {
        return zrangeByRankInternal(start, end, true);
    }

    /**
     * 查询指定排名区间的成员id和分数，start和end都是从0开始的。
     * 排名0表示分数最小的元素。
     * start和end都可以是负数，此时它们表示从最高排名元素开始的偏移量，eg: -1表示最高排名的元素， -2表示第二高分的元素，以此类推。
     *
     * @param start   起始排名 inclusive
     * @param end     截止排名 inclusive
     * @param reverse 是否逆序返回
     * @return 区间范围内的成员id和score
     */
    private List<Member> zrangeByRankInternal(int start, int end, boolean reverse) {
        final int zslLength = zsl.length();

        start = convertStartRank(start, zslLength);
        end = convertEndRank(end, zslLength);

        if (isRankRangeEmpty(start, end, zslLength)) {
            return new ArrayList<>();
        }

        int rangeLen = end - start + 1;

        SkipListNode listNode;
        /* Check if starting point is trivial, before doing log(N) lookup. */
        if (reverse) {
            listNode = zsl.tail;
            if (start > 0) {
                listNode = zsl.zslGetElementByRank(zslLength - start);
            }
        } else {
            listNode = zsl.header.levelInfo[0].forward;
            if (start > 0) {
                listNode = zsl.zslGetElementByRank(start + 1);
            }
        }

        final List<Member> result = new ArrayList<>(rangeLen);
        while (rangeLen-- > 0 && listNode != null) {
            result.add(new Member(listNode.obj, listNode.score));
            listNode = reverse ? listNode.backward : listNode.levelInfo[0].forward;
        }
        return result;
    }

    // ------------------------------------------------------- 内部实现 ----------------------------------------

    /**
     * 跳表
     * 注意：跳表的排名是从1开始的。
     *
     * @author wjybxx
     * @version 1.0
     * date - 2019/11/4
     */
    public static class SkipList {

        /**
         * 跳表允许最大层级 - redis允许64层级，我们使用32层
         */
        private static final int ZSKIPLIST_MAXLEVEL = 32;

        /**
         * 跳表升层概率 - redis 64层时的p为0.25，我们使用0.5f
         */
        private static final float ZSKIPLIST_P = 0.5f;

        /**
         * {@link Random}本身是线程安全的，但是多线程使用会产生不必要的竞争，因此创建一个独立的random对象。
         * - 其实也可以使用{@link java.util.concurrent.ThreadLocalRandom}
         */
        private final Random random = new Random();
        /**
         * 跳表头结点 - 哨兵
         * 1. 可以简化判定逻辑
         * 2. 恰好可以使得rank从1开始
         */
        private final SkipListNode header;

        /**
         * 跳表尾节点
         */
        private SkipListNode tail;

        /**
         * 跳表元素个数
         * 注意：head头指针不包含在length计数中。
         */
        private int length = 0;

        /**
         * level表示SkipList的总层数，即所有节点层数的最大值。
         */
        private int level = 1;

        public SkipList() {
            this.header = zslCreateNode(ZSKIPLIST_MAXLEVEL, 0, 0);
        }

        /**
         * 创建一个skipList的节点
         *
         * @param level 节点具有的层级 - {@link #zslRandomLevel()}
         * @param score 成员分数
         * @param objId 成员id
         * @return node
         */
        private static SkipListNode zslCreateNode(int level, long score, long objId) {
            final SkipListNode node = new SkipListNode(objId, score, new SkipListLevel[level]);
            for (int index = 0; index < level; index++) {
                node.levelInfo[index] = new SkipListLevel();
            }
            return node;
        }

        /**
         * 返回一个随机的层级分配给即将插入的节点。
         * 返回的层级值在 1 和 ZSKIPLIST_MAXLEVEL 之间（包含两者）。
         * 具有类似幂次定律的分布，越高level返回的可能性更小。
         * <p>
         * Returns a random level for the new skiplist node we are going to create.
         * The return value of this function is between 1 and ZSKIPLIST_MAXLEVEL
         * (both inclusive), with a powerlaw-alike distribution where higher
         * levels are less likely to be returned.
         *
         * @return level
         */
        private int zslRandomLevel() {
            int level = 1;
            while (level < ZSKIPLIST_MAXLEVEL && random.nextFloat() < ZSKIPLIST_P) {
                level++;
            }
            return level;
        }

        /**
         * 插入一个新的节点到跳表。
         * 这里假定元素已经不存在（直到调用方执行该方法）。
         * <p>
         * zslInsert a new node in the skiplist. Assumes the element does not already
         * exist (up to the caller to enforce that). The skiplist takes ownership
         * of the passed SDS string 'obj'.
         * <pre>
         *             header                    newNode
         *               _                                                 _
         * level - 1    |_| pre                                           |_|
         *  |           |_| pre                    _                      |_|
         *  |           |_| pre  _                |_|                     |_|
         *  |           |_|  ↓  |_| pre  _        |_|      _              |_|
         *  |           |_|     |_|  ↓  |_| pre   |_|     |_|             |_|
         *  |           |_|     |_|     |_| pre   |_|     |_|      _      |_|
         *  |           |_|     |_|     |_| pre   |_|     |_|     |_|     |_|
         *  0           |0|     |1|     |2| pre   |_|     |3|     |4|     |5|
         * </pre>
         *
         * @param score 分数
         * @param obj   obj 分数对应的成员id
         */
        SkipListNode zslInsert(long score, long obj) {
            // 新节点的level
            final int level = zslRandomLevel();

            // update - 需要更新后继节点的Node，新节点各层的前驱节点
            // 1. 分数小的节点
            // 2. 分数相同但id小的节点（分数相同时根据数据排序）
            // rank - 新节点各层前驱的当前排名
            // 这里不必创建一个ZSKIPLIST_MAXLEVEL长度的数组，它取决于插入节点后的新高度，你在别处看见的代码会造成大量的空间浪费，增加GC压力。
            // 如果创建的都是ZSKIPLIST_MAXLEVEL长度的数组，那么应该实现缓存
            final SkipListNode[] update = new SkipListNode[Math.max(level, this.level)];
            final int[] rank = new int[update.length];

            // preNode - 新插入节点的前驱节点
            SkipListNode preNode = header;
            for (int i = this.level - 1; i >= 0; i--) {
                /* store rank that is crossed to reach the insert position */
                if (i == (this.level - 1)) {
                    // 起始点，也就是head，它的排名就是0
                    rank[i] = 0;
                } else {
                    // 由于是回溯降级继续遍历，因此其初始排名是前一次遍历的排名
                    rank[i] = rank[i + 1];
                }

                while (preNode.levelInfo[i].forward != null &&
                        (preNode.levelInfo[i].forward.score < score ||
                                (preNode.levelInfo[i].forward.score == score && preNode.levelInfo[i].forward.obj < obj))) {
                    // preNode的后继节点仍然小于要插入的节点，需要继续前进，同时累计排名
                    rank[i] += preNode.levelInfo[i].span;
                    preNode = preNode.levelInfo[i].forward;
                }

                // 这是要插入节点的第i层的前驱节点，此时触发降级
                update[i] = preNode;
            }

            if (level > this.level) {
                /* 新节点的层级大于当前层级，那么高出来的层级导致需要更新head，且排名和跨度是固定的 */
                for (int i = this.level; i < level; i++) {
                    rank[i] = 0;
                    update[i] = this.header;
                    update[i].levelInfo[i].span = this.length;
                }
                this.level = level;
            }

            /* 由于我们允许的重复score，并且zslInsert(该方法)的调用者在插入前必须测试要插入的member是否已经在hash表中。
             * 因此我们假设key（obj）尚未被插入，并且重复插入score的情况永远不会发生。*/
            /* we assume the key is not already inside, since we allow duplicated
             * scores, and the re-insertion of score and redis object should never
             * happen since the caller of zslInsert() should test in the hash table
             * if the element is already inside or not.*/
            final SkipListNode newNode = zslCreateNode(level, score, obj);

            /* 这些节点的高度小于等于新插入的节点的高度，需要更新指针。此外它们当前的跨度被拆分了两部分，需要重新计算。 */
            for (int i = 0; i < level; i++) {
                /* 链接新插入的节点 */
                newNode.levelInfo[i].forward = update[i].levelInfo[i].forward;
                update[i].levelInfo[i].forward = newNode;

                /* rank[0] 是新节点的直接前驱的排名，每一层都有一个前驱，可以通过彼此的排名计算跨度 */
                /* 计算新插入节点的跨度 和 重新计算所有前驱节点的跨度，之前的跨度被拆分为了两份*/
                /* update span covered by update[i] as newNode is inserted here */
                newNode.levelInfo[i].span = update[i].levelInfo[i].span - (rank[0] - rank[i]);
                update[i].levelInfo[i].span = (rank[0] - rank[i]) + 1;
            }

            /*  这些节点高于新插入的节点，它们的跨度可以简单的+1 */
            /* increment span for untouched levels */
            for (int i = level; i < this.level; i++) {
                update[i].levelInfo[i].span++;
            }

            /* 设置新节点的前向节点(回溯节点) */
            newNode.backward = (update[0] == this.header) ? null : update[0];

            /* 设置新节点的后向节点 */
            if (newNode.levelInfo[0].forward != null) {
                newNode.levelInfo[0].forward.backward = newNode;
            } else {
                this.tail = newNode;
            }

            this.length++;
            return newNode;
        }

        /**
         * Delete an element with matching score/object from the skiplist.
         *
         * @param score 分数用于快速定位节点
         * @param obj   用于确定节点是否是对应的数据节点
         */
        boolean zslDelete(long score, long obj) {
            // update - 需要更新后继节点的Node
            // 1. 分数小的节点
            // 2. 分数相同但id小的节点（分数相同时根据数据排序）
            final SkipListNode[] update = new SkipListNode[this.level];

            SkipListNode preNode = this.header;
            for (int i = this.level - 1; i >= 0; i--) {
                while (preNode.levelInfo[i].forward != null &&
                        (preNode.levelInfo[i].forward.score < score ||
                                (preNode.levelInfo[i].forward.score == score && preNode.levelInfo[i].forward.obj < obj))) {
                    // preNode的后继节点仍然小于要插入的节点，需要继续前进
                    preNode = preNode.levelInfo[i].forward;
                }
                // 这是目标节点第i层的可能前驱节点
                update[i] = preNode;
            }

            /* 由于可能多个节点拥有相同的分数，因此必须同时比较score和object */
            /* We may have multiple elements with the same score, what we need
             * is to find the element with both the right score and object. */
            final SkipListNode targetNode = preNode.levelInfo[0].forward;
            if (targetNode != null && score == targetNode.score && targetNode.obj == obj) {
                zslDeleteNode(targetNode, update);
                return true;
            }

            /* not found */
            return false;
        }

        /**
         * Internal function used by zslDelete, zslDeleteByScore and zslDeleteByRank
         *
         * @param deleteNode 要删除的节点
         * @param update     可能要更新的节点们
         */
        private void zslDeleteNode(final SkipListNode deleteNode, final SkipListNode[] update) {
            for (int i = 0; i < this.level; i++) {
                if (update[i].levelInfo[i].forward == deleteNode) {
                    // 这些节点的高度小于等于要删除的节点，需要合并两个跨度
                    update[i].levelInfo[i].span += deleteNode.levelInfo[i].span - 1;
                    update[i].levelInfo[i].forward = deleteNode.levelInfo[i].forward;
                } else {
                    // 这些节点的高度高于要删除的节点，它们的跨度可以简单的 -1
                    update[i].levelInfo[i].span--;
                }
            }

            if (deleteNode.levelInfo[0].forward != null) {
                // 要删除的节点有后继节点
                deleteNode.levelInfo[0].forward.backward = deleteNode.backward;
            } else {
                // 要删除的节点是tail节点
                this.tail = deleteNode.backward;
            }

            // 如果删除的节点是最高等级的节点，则检查是否需要降级
            if (deleteNode.levelInfo.length == this.level) {
                while (this.level > 1 && this.header.levelInfo[this.level - 1].forward == null) {
                    // 如果最高层没有后继节点，则降级
                    this.level--;
                }
            }

            this.length--;
        }


        /**
         * 值是否大于等于下限
         *
         * @param value 要比较的score
         * @param spec  范围描述信息
         * @return true/false
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        static boolean zslValueGteMin(long value, ZScoreRangeSpec spec) {
            return spec.minex ? (value > spec.min) : (value >= spec.min);
        }

        /**
         * 值是否小于等于上限
         *
         * @param value 要比较的score
         * @param spec  范围描述信息
         * @return true/false
         */
        static boolean zslValueLteMax(long value, ZScoreRangeSpec spec) {
            return spec.maxex ? (value < spec.max) : (value <= spec.max);
        }

        /**
         * 判断zset中的数据所属的范围是否和指定range存在交集(intersection)。
         * 它不代表zset存在指定范围内的数据。
         * Returns if there is a part of the zset is in range.
         * <pre>
         *                         ZSet
         *              min ____________________ max
         *                 |____________________|
         *   min ______________ max  min _____________
         *      |______________|        |_____________|
         *          Range                   Range
         * </pre>
         *
         * @param range 范围描述信息
         * @return true/false
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean zslIsInRange(ZScoreRangeSpec range) {
            if (isScoreRangeEmpty(range)) {
                // 传进来的范围为空
                return false;
            }

            if (this.tail == null || !zslValueGteMin(this.tail.score, range)) {
                // 列表有序，按照从score小到大，如果尾部节点数据小于最小值，那么一定不在区间范围内
                return false;
            }

            final SkipListNode firstNode = this.header.levelInfo[0].forward;
            if (firstNode == null || !zslValueLteMax(firstNode.score, range)) {
                // 列表有序，按照从score小到大，如果首部节点数据大于最大值，那么一定不在范围内
                return false;
            }
            return true;
        }

        /**
         * 测试score范围信息是否为空(无效)
         *
         * @param range 范围描述信息
         * @return true/false
         */
        private static boolean isScoreRangeEmpty(ZScoreRangeSpec range) {
            return range.min > range.max ||
                    (range.min == range.max && (range.minex || range.maxex));
        }

        /**
         * 找出第一个在指定范围内的节点。如果没有符合的节点，则返回null。
         * <p>
         * Find the first node that is contained in the specified range.
         * Returns NULL when no element is contained in the range.
         *
         * @param range 范围描述符
         * @return 不存在返回null
         */
        @Nullable
        SkipListNode zslFirstInRange(ZScoreRangeSpec range) {
            /* zset数据范围与指定范围没有交集，可提前返回，减少不必要的遍历 */
            /* If everything is out of range, return early. */
            if (!zslIsInRange(range)) {
                return null;
            }

            SkipListNode lastNodeLtMin = this.header;
            for (int i = this.level - 1; i >= 0; i--) {
                /* 前进直到出现后继节点大于等于指定最小值的节点 */
                /* Go forward while *OUT* of range. */
                while (lastNodeLtMin.levelInfo[i].forward != null &&
                        !zslValueGteMin(lastNodeLtMin.levelInfo[i].forward.score, range)) {
                    // 如果当前节点的后继节点仍然小于指定范围的最小值，则继续前进
                    lastNodeLtMin = lastNodeLtMin.levelInfo[i].forward;
                }
            }

            /* 这里的上下文表明了，一定存在一个节点的值大于等于指定范围的最小值，因此下一个节点一定不为null */
            /* This is an inner range, so the next node cannot be NULL. */
            final SkipListNode firstNodeGteMin = lastNodeLtMin.levelInfo[0].forward;
            assert firstNodeGteMin != null;

            /* 如果该节点的数据大于max，则不存在再范围内的节点 */
            /* Check if score <= max. */
            if (!zslValueLteMax(firstNodeGteMin.score, range)) {
                return null;
            }
            return firstNodeGteMin;
        }

        /**
         * 找出最后一个在指定范围内的节点。如果没有符合的节点，则返回null。
         * <p>
         * Find the last node that is contained in the specified range.
         * Returns NULL when no element is contained in the range.
         *
         * @param range 范围描述信息
         * @return 不存在返回null
         */
        @Nullable
        SkipListNode zslLastInRange(ZScoreRangeSpec range) {
            /* zset数据范围与指定范围没有交集，可提前返回，减少不必要的遍历 */
            /* If everything is out of range, return early. */
            if (!zslIsInRange(range)) {
                return null;
            }

            SkipListNode lastNodeLteMax = this.header;
            for (int i = this.level - 1; i >= 0; i--) {
                /* Go forward while *IN* range. */
                while (lastNodeLteMax.levelInfo[i].forward != null &&
                        zslValueLteMax(lastNodeLteMax.levelInfo[i].forward.score, range)) {
                    // 如果当前节点的后继节点仍然小于最大值，则继续前进
                    lastNodeLteMax = lastNodeLteMax.levelInfo[i].forward;
                }
            }

            /* 这里的上下文表明一定存在一个节点的值小于指定范围的最大值，因此当前节点一定不为null */
            /* This is an inner range, so this node cannot be NULL. */
            assert lastNodeLteMax != null;

            /* Check if score >= min. */
            if (!zslValueGteMin(lastNodeLteMax.score, range)) {
                return null;
            }
            return lastNodeLteMax;
        }


        /**
         * 删除指定分数区间的所有节点。
         * <b>Note</b>: 该方法引用了ZSet的哈希表视图，以便从哈希表中删除元素。
         * <p>
         * Delete all the elements with score between min and max from the skiplist.
         * Min and max are inclusive, so a score >= min || score <= max is deleted.
         * Note that this function takes the reference to the hash table view of the
         * sorted set, in order to remove the elements from the hash table too.
         *
         * @param range 范围描述符
         * @param dict  对象id到score的映射
         * @return 删除的节点数量
         */
        int zslDeleteRangeByScore(ZScoreRangeSpec range, Map<Long, Long> dict) {
            final SkipListNode[] update = new SkipListNode[this.level];
            int removed = 0;

            SkipListNode lastNodeLtMin = this.header;
            for (int i = this.level - 1; i >= 0; i--) {
                while (lastNodeLtMin.levelInfo[i].forward != null &&
                        !zslValueGteMin(lastNodeLtMin.levelInfo[i].forward.score, range)) {
                    lastNodeLtMin = lastNodeLtMin.levelInfo[i].forward;
                }
                update[i] = lastNodeLtMin;
            }

            /* 当前节点是小于目标范围最小值的最后一个节点，它的下一个节点可能为null，或大于等于最小值 */
            /* Current node is the last with score < or <= min. */
            SkipListNode firstNodeGteMin = lastNodeLtMin.levelInfo[0].forward;

            /* 删除在范围内的节点(小于等于最大值的节点) */
            /* Delete nodes while in range. */
            while (firstNodeGteMin != null
                    && zslValueLteMax(firstNodeGteMin.score, range)) {
                final SkipListNode next = firstNodeGteMin.levelInfo[0].forward;
                zslDeleteNode(firstNodeGteMin, update);
                dict.remove(firstNodeGteMin.obj);
                removed++;
                firstNodeGteMin = next;
            }
            return removed;
        }

        /**
         * 删除指定排名区间的所有成员。包括start和end。
         * <b>Note</b>: start和end基于从1开始
         * <p>
         * Delete all the elements with rank between start and end from the skiplist.
         * Start and end are inclusive. Note that start and end need to be 1-based
         *
         * @param start 起始排名 inclusive
         * @param end   截止排名 inclusive
         * @param dict  member -> score的字典
         * @return 删除的成员数量
         */
        int zslDeleteRangeByRank(int start, int end, Map<Long, Long> dict) {
            final SkipListNode[] update = new SkipListNode[this.level];
            /* 已遍历的真实元素数量，表示元素的真实排名 */
            int traversed = 0;
            int removed = 0;

            SkipListNode lastNodeLtStartRank = this.header;
            for (int i = this.level - 1; i >= 0; i--) {
                while (lastNodeLtStartRank.levelInfo[i].forward != null && (traversed + lastNodeLtStartRank.levelInfo[i].span) < start) {
                    // 下一个节点的排名还未到范围内，继续前进
                    // 更新已遍历的元素数量
                    traversed += lastNodeLtStartRank.levelInfo[i].span;
                    lastNodeLtStartRank = lastNodeLtStartRank.levelInfo[i].forward;
                }
                update[i] = lastNodeLtStartRank;
            }

            traversed++;

            SkipListNode nodeGteStart = lastNodeLtStartRank.levelInfo[0].forward;
            while (nodeGteStart != null && traversed <= end) {
                final SkipListNode next = nodeGteStart.levelInfo[0].forward;
                zslDeleteNode(nodeGteStart, update);
                dict.remove(nodeGteStart.obj);
                removed++;
                traversed++;
                nodeGteStart = next;
            }
            return removed;
        }

        /**
         * 通过score和key查找元素所属的排名。
         * 如果找不到对应的元素，则返回0。
         * <b>Note</b>：排名从1开始
         * <p>
         * Find the rank for an element by both score and key.
         * Returns 0 when the element cannot be found, rank otherwise.
         * Note that the rank is 1-based due to the span of zsl->header to the
         * first element.
         *
         * @param score 节点分数
         * @param obj   节点对应的数据id
         * @return 排名，从1开始
         */
        int zslGetRank(long score, long obj) {
            int rank = 0;
            SkipListNode firstNodeGteScore = this.header;
            for (int i = this.level - 1; i >= 0; i--) {
                while (firstNodeGteScore.levelInfo[i].forward != null &&
                        (firstNodeGteScore.levelInfo[i].forward.score < score ||
                                (firstNodeGteScore.levelInfo[i].forward.score == score && firstNodeGteScore.levelInfo[i].forward.obj <= obj))) {
                    // forward.obj <= obj 也继续前进，也就是我么期望如果score相同时，在目标节点停下来，这样rank也不必特殊处理
                    rank += firstNodeGteScore.levelInfo[i].span;
                    firstNodeGteScore = firstNodeGteScore.levelInfo[i].forward;
                }

                /* firstNodeGteScore might be equal to zsl->header, so test if firstNodeGteScore is header */
                if (firstNodeGteScore != this.header && firstNodeGteScore.obj == obj) {
                    // 可能在任意层找到
                    return rank;
                }
            }
            return 0;
        }

        /**
         * 查找指定排名的元素数据，如果不存在，则返回Null。
         * 注意：排名从1开始
         * <p>
         * Finds an element by its rank. The rank argument needs to be 1-based.
         *
         * @param rank 排名，1开始
         * @return element
         */
        @Nullable
        SkipListNode zslGetElementByRank(int rank) {
            int traversed = 0;
            SkipListNode firstNodeGteRank = this.header;
            for (int i = this.level - 1; i >= 0; i--) {
                while (firstNodeGteRank.levelInfo[i].forward != null && (traversed + firstNodeGteRank.levelInfo[i].span) <= rank) {
                    // <= rank 表示我们期望在目标节点停下来
                    traversed += firstNodeGteRank.levelInfo[i].span;
                    firstNodeGteRank = firstNodeGteRank.levelInfo[i].forward;
                }

                if (traversed == rank) {
                    // 可能在任意层找到该排名的数据
                    return firstNodeGteRank;
                }
            }
            return null;
        }

        /**
         * @return 跳表中的元素数量
         */
        private int length() {
            return length;
        }

        /**
         * 获取跳表的堆内存视图
         *
         * @return string
         */
        public String dump() {
            final StringBuilder sb = new StringBuilder("{level = 0, nodeArray:[\n");
            SkipListNode curNode = this.header.levelInfo[0].forward;
            int rank = 0;
            while (curNode != null) {
                sb.append("{rank:").append(rank++)
                        .append(",obj:").append(curNode.obj)
                        .append(",score:").append(curNode.score);

                curNode = curNode.levelInfo[0].forward;

                if (curNode != null) {
                    sb.append("},\n");
                } else {
                    sb.append("}\n");
                }
            }
            return sb.append("]}").toString();
        }
    }

    /**
     * 跳表节点
     */
    static class SkipListNode {
        /**
         * 节点对应的数据id - 如果要通用的话，这里将来将是一个泛型对象，需要实现{@link Comparable}，且必须满足仅当equals为true的时候compare返回0
         */
        final long obj;
        /**
         * 该节点数据对应的评分 - 如果要通用的话，这里将来将是一个泛型对象，需要实现{@link Comparable}。
         */
        final long score;
        /**
         * 该节点的层级信息
         * level[]存放指向各层链表后一个节点的指针（后向指针）。
         */
        final SkipListLevel[] levelInfo;
        /**
         * 该节点的前向指针
         * backward字段是指向链表前一个节点的指针（前向指针）。
         * 节点只有1个前向指针，所以只有第1层链表是一个双向链表。
         */
        SkipListNode backward;

        private SkipListNode(long obj, long score, SkipListLevel[] levelInfo) {
            this.obj = obj;
            this.score = score;
            this.levelInfo = levelInfo;
        }
    }

    /**
     * 跳表层级
     */
    static class SkipListLevel {
        /**
         * 每层对应1个后向指针 (后继节点)
         */
        SkipListNode forward;
        /**
         * 到后继节点之间的跨度
         * 它表示当前的指针跨越了多少个节点。span用于计算元素排名(rank)，这是Redis对于SkipList做的一个扩展。
         */
        int span;
    }

    /**
     * {@link ZSet}中“score”范围描述信息 - specification模式
     */
    public static class ZScoreRangeSpec {
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

    /**
     * zset中单个成员信息
     */
    public static class Member {

        private final long member;
        private final long score;

        Member(long member, long score) {
            this.member = member;
            this.score = score;
        }

        public long getMember() {
            return member;
        }

        public long getScore() {
            return score;
        }
    }

    // - 测试用例

    public static void main(String[] args) {
        final ZSet zSet = new ZSet();

        // 插入100个数据，member编号就是1-100
        IntStream.rangeClosed(1, 100).forEach(member -> {
            // 使用nextInt避免越界，导致一些奇怪的值
            zSet.zadd(ThreadLocalRandom.current().nextInt(0, 10000), member);
        });

        // 重新插入
        IntStream.rangeClosed(1, 100).forEach(member -> {
            zSet.zincrby(ThreadLocalRandom.current().nextInt(0, 10000), member);
        });

        System.out.println("------------------------- dump ----------------------");
        System.out.println(zSet.zsl.dump());
        // 由于不是很好调试，建议在debug界面根据输出信息调试
        System.out.println("debug");
    }
}
