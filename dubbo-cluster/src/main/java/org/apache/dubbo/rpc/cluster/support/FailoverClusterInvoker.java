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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_RETRIES;
import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;

/**
 * 故障切换
 * 有重试
 *
 * When invoke fails, log the initial error and retry other invokers (retry n times, which means at most n different invokers will be invoked)
 * Note that retry causes latency.
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Failover">Failover</a>
 *
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    /**
     * AbstractClusterInvoker.invoke() 调用到这里 FailoverClusterInvoker.doInvoke
     *
     * @param invokers provider 列表
     * @param loadbalance 具体的负载均衡策略
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {

        List<Invoker<T>> copyInvokers = invokers;

        // 检查 invokers 不能为空
        checkInvokers(copyInvokers, invocation);

        // 获取调用的方法名称
        String methodName = RpcUtils.getMethodName(invocation);

        // 根据方法名获取 invoker 的 retries 次数
        int len = calculateInvokeTimes(methodName);

        // retry loop.
        RpcException le = null; // last exception.

        // 已调用的 Invoker 列表
        List<Invoker<T>> invoked = new ArrayList<Invoker<T>>(copyInvokers.size()); // invoked invokers.

        Set<String> providers = new HashSet<String>(len);

        for (int i = 0; i < len; i++) {
            //Reselect before retry to avoid a change of candidate `invokers`. (重试前重新选择，以避免候选“调用者”的更改)
            //NOTE: if `invokers` changed, then `invoked` also lose accuracy. (如果“调用者”改变了，那么“调用者”也会失去准确性。)
            // 第一次正常执行不会执行这里，所有重试都会执行到这里
            if (i > 0) {
                // 判断 consumer 是否还存在
                checkWhetherDestroyed();
                // 重新获取 Invoker 列表
                copyInvokers = list(invocation);
                // check again，重新检查所有的 Invoker
                checkInvokers(copyInvokers, invocation);
            }
            // 通过负载均衡，获取待执行的 Invoker
            Invoker<T> invoker = select(loadbalance, invocation, copyInvokers, invoked);
            // 添加到已执行的 invoked 列表
            invoked.add(invoker);
            RpcContext.getContext().setInvokers((List) invoked);
            try {
                // 根据负载均衡策略选择 Invoker 之后，再根据 Invoker 调用
                // InvokerWrapper.invoke(Invocation invocation)
                Result result = invoker.invoke(invocation);
                if (le != null && logger.isWarnEnabled()) {
                    logger.warn("Although retry the method " + methodName
                            + " in the service " + getInterface().getName()
                            + " was successful by the provider " + invoker.getUrl().getAddress()
                            + ", but there have been failed providers " + providers
                            + " (" + providers.size() + "/" + copyInvokers.size()
                            + ") from the registry " + directory.getUrl().getAddress()
                            + " on the consumer " + NetUtils.getLocalHost()
                            + " using the dubbo version " + Version.getVersion() + ". Last error is: "
                            + le.getMessage(), le);
                }
                // RpcException 为空就直接返回
                return result;
            } catch (RpcException e) {
                if (e.isBiz()) { // biz exception.
                    throw e;
                }
                le = e;
            } catch (Throwable e) {
                le = new RpcException(e.getMessage(), e);
            } finally {
                providers.add(invoker.getUrl().getAddress());
            }
        }
        throw new RpcException(le.getCode(), "Failed to invoke the method "
                + methodName + " in the service " + getInterface().getName()
                + ". Tried " + len + " times of the providers " + providers
                + " (" + providers.size() + "/" + copyInvokers.size()
                + ") from the registry " + directory.getUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version "
                + Version.getVersion() + ". Last error is: "
                + le.getMessage(), le.getCause() != null ? le.getCause() : le);
    }

    /** 获取 Invoker 重试次数 */
    private int calculateInvokeTimes(String methodName) {
        // 当前调用就是 1 次，再获取 2 次，相加总共就是 3 次
        int len = getUrl().getMethodParameter(methodName, RETRIES_KEY, DEFAULT_RETRIES) + 1;
        RpcContext rpcContext = RpcContext.getContext();
        Object retry = rpcContext.getObjectAttachment(RETRIES_KEY);
        if (null != retry && retry instanceof Number) {
            len = ((Number) retry).intValue() + 1;
            rpcContext.removeAttachment(RETRIES_KEY);
        }
        if (len <= 0) {
            len = 1;
        }

        return len;
    }

}
