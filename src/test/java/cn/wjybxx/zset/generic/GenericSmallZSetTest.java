package cn.wjybxx.zset.generic;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * {@link GenericZSet}的复杂score测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/7
 * github - https://github.com/hl845740757
 */
public class GenericSmallZSetTest {

    public static void main(String[] args) {
        final GenericZSet<Long, ComplexScore> zSet = GenericZSet.newLongKeyZSet(new ComplexScoreHandler());
        zSet.setSmallRange(true);

        // 插入数据
        LongStream.rangeClosed(1, 10000).forEach(playerId -> {
            zSet.zadd(randomScore(), playerId);
        });

        // 覆盖数据
        LongStream.rangeClosed(1, 10000).forEach(playerId -> {
            zSet.zadd(randomScore(), playerId);
        });

        System.out.println("------------------------- dump ----------------------");
        System.out.println(zSet.dump());
        System.out.println();

        List<Entry<Long, ComplexScore>> streamList = zSet.stream().collect(Collectors.toList());
        List<Entry<Long, ComplexScore>> directList = zSet.toList();
        if (!directList.equals(streamList)) {
            throw new IllegalArgumentException("!directList.equals(streamList)");
        }
    }

    private static ComplexScore randomScore() {
        return new ComplexScore(ThreadLocalRandom.current().nextInt(0, 4),
                ThreadLocalRandom.current().nextInt(1, 101));
    }

    private static class ComplexScore {

        private final int vipLevel;
        private final int level;

        ComplexScore(int vipLevel, int level) {
            this.level = level;
            this.vipLevel = vipLevel;
        }

        public int getLevel() {
            return level;
        }

        public int getVipLevel() {
            return vipLevel;
        }

        @Override
        public String toString() {
            return "{" +
                    "vipLevel=" + vipLevel +
                    ", level=" + level +
                    '}';
        }
    }

    private static class ComplexScoreHandler implements ScoreHandler<ComplexScore> {

        @Override
        public int compare(ComplexScore o1, ComplexScore o2) {
            final int vipLevelCompareR = Integer.compare(o2.vipLevel, o1.vipLevel);
            if (vipLevelCompareR != 0) {
                return vipLevelCompareR;
            }
            return Integer.compare(o2.level, o1.level);
        }

        @Override
        public ComplexScore sum(ComplexScore oldScore, ComplexScore increment) {
            throw new UnsupportedOperationException();
        }
    }

}