package com.arminzheng;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 库存扣减
 *
 * @author armin
 * @since 2022.06.14
 */
public class KillDemo {

    /*
    启动10个线程
    库存6个
    生成一个合并队列
    每个用户都能拿到自己的请求响应 */
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        KillDemo killDemo = new KillDemo();
        killDemo.mergeJob();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Future<Result>> futureList = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            Long orderId = i + 100L;
            Long userId = (long) i;
            Future<Result> future =
                    executorService.submit(
                            () -> {
                                countDownLatch.countDown();
                                // 没有做等待
                                countDownLatch.await(1, TimeUnit.SECONDS);
                                return killDemo.operate(new UserRequest(orderId, userId, 1));
                            });

            futureList.add(future);
        }

        futureList.forEach(
                future -> {
                    try {
                        // 每个用户最多等待 300ms
                        Result result = future.get(300, TimeUnit.MILLISECONDS);
                        System.out.println(Thread.currentThread().getName() + " 客户端请求响应 " + result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
    }

    // 模拟数据库行
    private volatile Integer stock = 6;
    // 合并队列
    private final BlockingDeque<RequestPromise> queue = new LinkedBlockingDeque<>(10);

    /**
     * 用户库存扣减
     *
     * <pre>
     * TODO 阈值判断
     * TODO 队列创建</pre>
     *
     * synchronized 指向堆对象
     *
     * <p>当对象为局部变量（非原始类型）时，只有引用是局部的，而不是实际的对象本身。实际上在堆上其可以被许多其他线程访问。
     *
     * <p>因此，需要对对象进行同步，以便单个线程一次只能访问该对象。
     *
     * @see <a
     *     href="https://stackoverflow.com/questions/43134998/is-it-reasonable-to-synchronize-on-a-local-variable">Is
     *     it reasonable to synchronize on a local variable?</a>
     */
    public Result operate(UserRequest userRequest) throws InterruptedException {
        RequestPromise requestPromise = new RequestPromise(userRequest);
        synchronized (requestPromise) {
            boolean enqueueSuccess = queue.offer(requestPromise, 100, TimeUnit.MILLISECONDS);
            if (!enqueueSuccess) {
                return new Result(false, "系统繁忙");
            }
            try {
                // 进队列成功后阻塞 200ms
                requestPromise.wait(200);
                // 等待超时不会抛出异常。结束时间之后，直接往下执行，返回null
                if (requestPromise.getResult() == null) {
                    return new Result(false, "等待超时");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return requestPromise.getResult();
    }

    public void mergeJob() {
        Runnable runnable =
                () -> {
                    ArrayList<RequestPromise> list = new ArrayList<>();
                    while (true) {
                        if (queue.isEmpty()) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(10);
                                continue;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        /*while (queue.peek() != null) { // 如果生产端比消费端快，就会造成死循环
                            list.add(queue.poll());
                        }*/
                        int batchSize = queue.size();
                        for (int i = 0; i < batchSize; i++) {
                            list.add(queue.poll()); // poll 自动移除
                        }

                        System.out.println(Thread.currentThread().getName() + " 合并扣减库存: " + list);

                        int sum = list.stream().mapToInt(e -> e.getUserRequest().getCount()).sum();
                        // 两种情况 ：1/2 库存足够
                        if (sum <= stock) {
                            stock -= sum;
                            // notify user
                            list.forEach(
                                    requestPromise -> {
                                        requestPromise.setResult(new Result(true, "ok"));
                                        synchronized (requestPromise) {
                                            requestPromise.notify();
                                        }
                                    });
                            continue;
                        }
                        // 2/2 库存不足
                        for (RequestPromise requestPromise : list) {
                            int count = requestPromise.getUserRequest().getCount();
                            if (count <= stock) { // 库存: 1, 下单: {user1: 2, user2: 1}
                                stock -= count;
                                requestPromise.setResult(new Result(true, "ok"));
                            } else {
                                requestPromise.setResult(new Result(false, "库存不足"));
                            }
                            synchronized (requestPromise) { // 库存不足没有进行通知
                                requestPromise.notify();
                            }
                        }
                        list.clear();
                    }
                };
        new Thread(runnable, "mergeThread").start();
    }

    @Data
    static class RequestPromise {
        private UserRequest userRequest;
        private Result result;

        public RequestPromise(UserRequest userRequest) {
            this.userRequest = userRequest;
        }
    }

    @Data
    static class Result {
        private boolean success;
        private String msg;

        public Result(boolean success, String msg) {
            this.success = success;
            this.msg = msg;
        }
    }

    @Data
    static class UserRequest {
        private Long orderId;
        private Long userId;
        private Integer count;

        public UserRequest(Long orderId, Long userId, Integer count) {
            this.orderId = orderId;
            this.userId = userId;
            this.count = count;
        }
    }
}
