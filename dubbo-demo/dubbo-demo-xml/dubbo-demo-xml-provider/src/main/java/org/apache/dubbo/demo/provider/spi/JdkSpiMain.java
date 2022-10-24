package org.apache.dubbo.demo.provider.spi;

import java.util.ServiceLoader;

/**
 * @author: doudou
 * @since: 2022-10-23
 */
public class JdkSpiMain {

    public static void main(String[] args) {
        ServiceLoader<SpiInterface> load = ServiceLoader.load(SpiInterface.class);
        load.forEach(spiInterface -> {
            String content = "content";
            String str = spiInterface.sayHello(content);
            System.out.println(str);
        });
    }

}
