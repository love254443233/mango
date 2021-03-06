package mango.rpc;

import mango.common.URL;
import mango.common.URLParam;
import mango.core.extension.ExtensionLoader;
import mango.exception.RpcFrameworkException;
import mango.registry.Registry;
import mango.registry.RegistryFactory;

import java.util.List;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class DefaultConfigHandler implements ConfigHandler {

    @Override
    public <T> Invoker<T> refer(Class<T> clz, URL url, URL serviceUrl) {
        String protocolName = serviceUrl.getParameter(URLParam.protocol.getName(), URLParam.protocol.getValue());
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName);
        return protocol.refer(clz, url, serviceUrl);
    }

    @Override
    public <T> Exporter<T> export(Class<T> interfaceClass, T ref, URL serviceUrl, List<URL> registryUrls) {

        String protocolName = serviceUrl.getParameter(URLParam.protocol.getName(), URLParam.protocol.getValue());
        Provider<T> provider = new DefaultProvider<T>(ref, serviceUrl, interfaceClass);
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName);
        Exporter<T> exporter = protocol.export(provider, serviceUrl);

        // register service
        register(registryUrls, serviceUrl);

        return exporter;
    }

    private void register(List<URL> registryUrls, URL serviceUrl) {

        for (URL registryUrl : registryUrls) {
            // 根据check参数的设置，register失败可能会抛异常，上层应该知晓
            RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(registryUrl.getProtocol());
            if (registryFactory == null) {
                throw new RpcFrameworkException("register error! Could not find extension for registry protocol:" + registryUrl.getProtocol()
                                + ", make sure registry module for " + registryUrl.getProtocol() + " is in classpath!");
            }
            try {
                Registry registry = registryFactory.getRegistry(registryUrl);
                registry.register(serviceUrl);
            } catch (Exception e) {
                throw new RpcFrameworkException("register error! Could not registry service:" + serviceUrl.getPath()
                        + " for " + registryUrl.getProtocol());
            }
        }
    }
}
