package org.apache.dubbo.demo.provider.provider;

import org.apache.dubbo.demo.provider.spi.SpiInterface;

/**
 * @author: doudou
 * @since: 2022-10-23
 */
public class SpiImpl02 implements SpiInterface {

    @Override
    public String sayHello(String content) {
        return "SpiImpl02: " + content;
    }

}
