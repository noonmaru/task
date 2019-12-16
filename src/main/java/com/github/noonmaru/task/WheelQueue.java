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

final class WheelQueue
{
    private WheelTask first, last;

    void linkLast(WheelTask task)
    {
        final WheelTask l = last;
        task.prev = l;
        last = task;
        if (l == null)
            first = task;
        else
            l.next = task;
        task.queue = this;
    }

    WheelTask peek()
    {
        return this.first;
    }

    void unlink(WheelTask x)
    {
        final WheelTask next = x.next;
        final WheelTask prev = x.prev;

        if (prev == null)
        {
            first = next;
        }
        else
        {
            prev.next = next;
            x.prev = null;
        }

        if (next == null)
        {
            last = prev;
        }
        else
        {
            next.prev = prev;
            x.next = null;
        }
        x.queue = null;
    }

    void unlinkFirst(WheelTask f)
    {
        final WheelTask next = f.next;
        f.queue = null;
        f.next = null; // help GC
        first = next;
        if (next == null)
            last = null;
        else
            next.prev = null;
    }
}
