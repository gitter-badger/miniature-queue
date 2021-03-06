package com.github.mlk.queue.jedis.example;

import com.github.mlk.queue.*;
import com.github.mlk.queue.jedis.JedisServer;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

public class FanoutTest {
    @Queue(value = "fanout-example", queueTypeHint = QueueType.FANOUT_QUEUE)
    interface FanoutExampleQueue {
        @Publish
        void publishMessage(String message);

        @Handle
        void receiveMessage(Function<String, Boolean> function);
    }

    @Test
    public void whenItemPutOnQueueThenAllListenersRelieveACopy() throws InterruptedException {
        final AtomicBoolean oneReceiveMessage = new AtomicBoolean(false);
        final AtomicBoolean twoReceiveMessage = new AtomicBoolean(false);
        JedisServer s1 = new JedisServer("localhost");
        JedisServer s2 = new JedisServer("localhost");
        JedisServer s3 = new JedisServer("localhost");

        try {

            FanoutExampleQueue sender = Queuify.builder().server(s3).target(FanoutExampleQueue.class);

            FanoutExampleQueue one = Queuify.builder().server(s1).target(FanoutExampleQueue.class);
            one.receiveMessage((x) -> {
                oneReceiveMessage.set(true);
                return true;
            });


            FanoutExampleQueue two = Queuify.builder().server(s2).target(FanoutExampleQueue.class);
            two.receiveMessage((x) -> {
                twoReceiveMessage.set(true);
                return true;
            });

            // Give REDIS some time to get ready...
            Thread.sleep(500);

            sender.publishMessage("msg");

            // Give REDIS some time to send the message
            Thread.sleep(500);

            assertTrue(oneReceiveMessage.get() && twoReceiveMessage.get());
        } finally {
            s1.close();
            s2.close();
            s3.close();
        }
    }
}
