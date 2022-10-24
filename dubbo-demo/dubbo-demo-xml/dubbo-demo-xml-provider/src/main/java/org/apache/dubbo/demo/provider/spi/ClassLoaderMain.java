package org.apache.dubbo.demo.provider.spi;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;

import java.util.List;

/**
 * @author: doudou
 * @since: 2022-10-23
 */
public class ClassLoaderMain {

    private static final Protocol PROTOCOL = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    public static void main(String[] args) {
        /*Class<ClassLoaderMain> mainClass = ClassLoaderMain.class;
        ClassLoader classLoader = mainClass.getClassLoader();
        System.out.println(classLoader.getParent());*/

        List<ProtocolServer> servers = PROTOCOL.getServers();


    }

}
