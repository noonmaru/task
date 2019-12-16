/*
 * Copyright (c) 2019 Noonmaru
 *
 * Licensed under the General Public License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.noonmaru.task;

import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * 원형 테스크 큐로 구현된 타이머입니다. <br>
 * 이 클래스는 Non thread safe 입니다
 *
 * @author Nemo
 * @see WheelTask
 */
public class WheelTimer implements Runnable
{
    private final WheelQueue[] wheel;

    private final LongSupplier movement;

    private long currentTicks;

    /**
     * 타이머의 최대 tick을 초기화합니다.<br>
     * 객체 생성 후에는 최대 tick을 변경 할 수 없습니다.
     *
     * @param maxTick 최대 지원 tick
     * @see WheelTimer#WheelTimer(int, LongSupplier)
     * @see WheelTimer#WheelTimer(int, LongSupplier, long)
     */
    public WheelTimer(int maxTick)
    {
        this(maxTick, null);
    }

    /**
     * 타이머의 최대 tick과 timeSupplier와 시간 간격을 초기화합니다.
     * timeSupplier는 {@code System::nanoTime() or System::currentTimeMillis()}과 같이 얻어올 수 있습니다.
     *
     * @param maxTick      최대 지원 tick
     * @param timeSupplier 시간 제공자 {@code System::nanoTime() or System::currentTimeMillis()}
     * @param interval     틱 간격
     * @see WheelTimer#WheelTimer(int)
     * @see WheelTimer#WheelTimer(int, LongSupplier)
     */
    public WheelTimer(int maxTick, final LongSupplier timeSupplier, final long interval)
    {
        this(maxTick, () -> timeSupplier.getAsLong() / interval);
    }

    /**
     * 타이머의 최대 tick과 movement를 초기화합니다.
     * movement에서 현재 tick을 제공하여야 합니다.
     *
     * @param maxTick timer in support ticks
     * @param movement timer tick movement
     */
    public WheelTimer(int maxTick, LongSupplier movement)
    {
        if (maxTick <= 0)
            throw new IllegalArgumentException("Illegal maxTick " + maxTick);

        this.wheel = new WheelQueue[maxTick + 1];
        this.movement = movement;

        if (movement != null)
            this.currentTicks = movement.getAsLong();
    }

    /**
     * 한번 호출되는 태스크를 등록합니다.
     *
     * @param task 등록할 태스크
     */
    public void schedule(WheelTask task)
    {
        registerTask(task, 0, 0);
    }

    /**
     * 지연 시간 이후에 한번 호출되는 태스크를 등록합니다.
     *
     * @param task  등록할 태스크
     * @param delay 지연 시간
     */
    public void schedule(WheelTask task, int delay)
    {
        registerTask(task, Math.max(0, delay), 0);
    }

    /**
     * 반복 호출될 태스크를 등록합니다.
     *
     * @param task   등록할 태스크
     * @param delay  지연 시간
     * @param period 반복 지연 시간
     */
    public void schedule(WheelTask task, int delay, int period)
    {
        registerTask(task, Math.max(0, delay), Math.max(1, period));
    }

    private void registerTask(WheelTask task, int delay, int period)
    {
        if (task.state == WheelTask.SCHEDULED)
            throw new IllegalArgumentException("Already scheduled task " + task);

        WheelQueue[] wheel = this.wheel;
        int length = wheel.length;

        if (delay >= length)
            throw new IllegalArgumentException("Illegal delay " + delay);
        if (period >= length)
            throw new IllegalArgumentException("Illegal period " + period);

        if (task.state == WheelTask.RUNNING)
            task.queue.unlinkFirst(task);

        long currentTicks = this.currentTicks;

        task.state = WheelTask.SCHEDULED;
        task.period = period;
        long nextRun = currentTicks + delay;
        task.nextRun = nextRun;
        int index = (int) (nextRun % length);

        WheelQueue queue = wheel[index];

        if (queue == null)
            wheel[index] = queue = new WheelQueue();

        queue.linkLast(task);
    }

    /**
     * 등록된 태스크들을 제공되는 틱에 맞춰 호출합니다.
     * 태스크들은 흐른 시간에 관계없이 1번만 호출합니다.
     */
    @Override
    public void run()
    {
        long taskTicks = this.currentTicks;
        long currentTicks = this.movement == null ? taskTicks + 1 : this.movement.getAsLong();

        if (taskTicks <= currentTicks)
        {
            if (taskTicks < currentTicks)
                this.currentTicks = currentTicks;

            WheelQueue[] wheel = this.wheel;
            int length = wheel.length;
            long targetTick = Math.min(currentTicks, taskTicks + length - 1); // one rotate

            do
            {
                int index = (int) (taskTicks % length);
                WheelQueue queue = wheel[index];

                if (queue != null)
                {
                    WheelTask task;

                    while ((task = queue.peek()) != null && task.nextRun <= currentTicks)
                    {
                        task.state = WheelTask.RUNNING;

                        try
                        {
                            task.run();
                        }
                        catch (Throwable t)
                        {
                            t.printStackTrace();
                        }

                        if (task.state == WheelTask.RUNNING)
                        {
                            queue.unlinkFirst(task);

                            int period = task.period;

                            if (period > 0)
                            {
                                long nextRun = currentTicks + period;
                                task.nextRun = nextRun;
                                int futureIndex = (int) (nextRun % length);
                                WheelQueue futureQueue = wheel[futureIndex];

                                if (futureQueue == null)
                                    wheel[futureIndex] = futureQueue = new WheelQueue();

                                futureQueue.linkLast(task);
                            }
                            else
                            {
                                task.state = WheelTask.EXECUTED;
                            }
                        }
                    }
                }
            }
            while (++taskTicks <= targetTick);
        }
    }

    /**
     * 모든 태스크를 취소합니다.
     */
    public void clear()
    {
        WheelQueue[] wheel = this.wheel;

        for (WheelQueue queue : wheel)
        {
            if (queue != null)
            {
                WheelTask task = queue.peek();

                if (task != null)
                {
                    queue.unlinkFirst(task);
                    task.state = WheelTask.CANCELLED;
                }
            }
        }

        Arrays.fill(wheel, null);
    }
}
