/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor;


/**
 * Yielding strategy that uses a Thread.yield() for {@link com.lmax.disruptor.EventProcessor}s waiting on a barrier
 * after an initially spinning.
 * <p>
 * This strategy will use 100% CPU, but will more readily give up the CPU than a busy spin strategy if other threads
 * require CPU resource.
 */
public final class YieldingWaitStrategy implements WaitStrategy
{
    private static final int SPIN_TRIES = 100; //循环的阈值

    @Override
    public long waitFor(
        final long sequence, Sequence cursor, final Sequence dependentSequence, final SequenceBarrier barrier)
        throws AlertException, InterruptedException
    {
        long availableSequence;
        int counter = SPIN_TRIES;

        //自旋锁
        while ((availableSequence = dependentSequence.get()) < sequence) //判断生产者序号是否小于消费者序号，如果小于则自旋
        {
            counter = applyWaitMethod(barrier, counter);
        }

        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking()
    {
    }

    private int applyWaitMethod(final SequenceBarrier barrier, int counter)
        throws AlertException
    {
        barrier.checkAlert();

        if (0 == counter)//等于0则让出CPU资源给其他线程
        {
            //调用yield方法并不会让线程进入阻塞状态，而是让线程重回就绪状态，它只需要等待重新获取CPU执行时间
            Thread.yield();
        }
        else
        {
            --counter; //小于100则一直循环
        }

        return counter;
    }
}
