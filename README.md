# java-zset
redis的zset的java实现，包含两个版本，一个long类型score的实现，一个泛型score的实现。两种zset除了score的区别以外，没有别的区别。  
java-zset实现了redis zset中的常用命令。此外，扩展zset较为容易，你可以在理解后自行添加需要的特性。

#### 主要参考
 * [lua 版zset实现](https://github.com/XanthusL/zset)
 * [go 版zset实现](https://github.com/liyiheng/zset)
 * [java版zset实现](https://github.com/gaopan461/java-zset)
 * [Redis 为什么用跳表而不用平衡树](https://juejin.im/post/57fa935b0e3dd90057c50fbc)
 
#### 已经有好几个版本了，为什么还要自己实现一遍呢？
上面说到的源码，主要问题：代码可读性极差，又缺乏注释，导致理解起来十分困难（看得我头疼）。因此在我的实现中，除了添加丰富的注释外，还进行了适当的重构，代码的可读性有着极大的提升。
方便小伙们学习。