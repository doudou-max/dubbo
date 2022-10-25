/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.GreetingService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * dubbo consumer 启动类
 */
public class Application {

    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    public static void main(String[] args) throws Exception {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-consumer.xml");

        context.start();

        // getBean() 方法会调用到 DubboBootstrap 的 init() 方法
        // getBean() 获取的对象返回的是代理的对象，代理对象就是 InvokerInvocationHandler，然后通过该对象的 invoke() 进行调用
        DemoService demoService = context.getBean("demoService", DemoService.class);
        GreetingService greetingService = context.getBean("greetingService", GreetingService.class);


        /*new Thread(() -> {
            while (true) {
                String greetings = greetingService.hello();
                System.out.println(greetings + " from separated thread.");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();*/

        while (true) {
            System.out.println("-----------------------------");
            /*CompletableFuture<String> hello = demoService.sayHelloAsync("world");
            System.out.println("demoService result: " + hello.get());*/

            String syncWorld = demoService.sayHello("sync world");
            System.out.println("demoService result:" + syncWorld);

            String greetings = greetingService.hello();
            System.out.println("greetingService result: " + greetings);

            System.out.println("-----------------------------");
            System.out.println();

            Thread.sleep(1000);
        }



        /*Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        if(line.equals("1")) demoService.sayHelloAsync("world");
        if(line.equals("2")) greetingService.hello();*/
    }
}
