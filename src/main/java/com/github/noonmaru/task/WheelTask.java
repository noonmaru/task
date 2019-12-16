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

/**
 * {@link WheelTimer}에 등록하여 사용하는 태스크 클래스입니다. <br>
 * 한번 혹은 주기적으로 호출 될 수 있습니다.
 *
 * @author  Nemo
 * @see     WheelTimer
 */
public abstract class WheelTask implements Runnable
{
    private static final int VIRGIN = 0;

    static final int SCHEDULED = 1;

    static final int RUNNING = 2;

    static final int EXECUTED = 3;

    static final int CANCELLED = 4;

    WheelQueue queue;

    WheelTask prev, next;

    int state = VIRGIN;

    int period;

    long nextRun;

    /**
     * 태스크가 호출되는 메서드입니다.
     * Override하여 사용하세요.
     */
    @Override
    public abstract void run();

    /**
     * 태스크를 취소합니다.
     * 취소 이후에 더 이상 호출 되지 않습니다.
     *
     * @return 등록되지 않은 태스크 혹은 이미 취소된 태스크는 false를 반환합니다.
     */
    public boolean cancel()
    {
        WheelQueue queue = this.queue;

        if (queue == null)
        {
            return false;
        }

        queue.unlink(this);
        state = CANCELLED;
        return true;
    }
}
