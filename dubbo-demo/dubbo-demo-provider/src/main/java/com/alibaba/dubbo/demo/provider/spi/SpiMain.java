package com.alibaba.dubbo.demo.provider.spi;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Protocol;

/**
 * @author: doudou
 * @since: 2023-01-01
 */
public class SpiMain {

    public static void main(String[] args) {
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        System.out.println(protocol);
    }

}
