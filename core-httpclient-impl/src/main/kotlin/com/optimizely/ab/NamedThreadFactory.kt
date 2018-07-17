/**
 *
 * Copyright 2016-2017, Optimizely and contributors
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
package com.optimizely.ab

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * [ThreadFactory] for providing Optimizely use-case specific naming.
 */
class NamedThreadFactory
/**
 * @param nameFormat the thread name format which should include a string placeholder for the thread number
 * @param daemon whether the threads created should be [Thread.daemon]s or not
 */
(private val nameFormat: String, private val daemon: Boolean) : ThreadFactory {

    private val backingThreadFactory = Executors.defaultThreadFactory()
    private val threadCount = AtomicLong(0)

    override fun newThread(r: Runnable): Thread {
        val thread = backingThreadFactory.newThread(r)
        val threadNumber = threadCount.incrementAndGet()

        thread.name = String.format(nameFormat, threadNumber)
        thread.isDaemon = daemon
        return thread
    }
}
