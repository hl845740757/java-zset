package com.wjybxx.zset.generic;

import java.util.Comparator;

/**
 * <b>Note:</b>
 * 1. score对象必须实现为不可变，一定不可以修改里面的内容。
 * 2. {@link #sum(Object, Object)}必须
 * 3. 不要在score对象中存储杂七杂八的属性，如果想存储一些额外的数据，请存储在key中。
 *
 * @param <T> the type of score
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/6
 */
public interface ScoreHandler<T> extends Comparator<T> {

    /**
     * 比较两个score的大小。
     *
     * @param o1 score
     * @param o2 score
     * @return -1,0,1
     */
    @Override
    int compare(T o1, T o2);

    /**
     * 计算两个score的和
     *
     * @param oldScore  当前分数
     * @param increment 增量
     * @return newInstance
     * @apiNote 必须返回一个新的对象。
     */
    T sum(T oldScore, T increment);

}
