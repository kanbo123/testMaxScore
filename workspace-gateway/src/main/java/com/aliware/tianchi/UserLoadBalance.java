package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import com.aliware.tianchi.comm.ServerLoadInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author daofeng.xjf
 *
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance{
    
    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        
        int size = invokers.size();
        // 总权重
        int totalWeight = 0;
        List<Integer> hasPermitArr = new ArrayList<>();
        // 首先获取invoker对应的服务端耗时最大的索引
        //String loadInfo = "";
        for(int index=0;index<size;index++){
            Invoker<T> invoker = invokers.get(index);
            ServerLoadInfo serverLoadInfo = UserLoadBalanceService.getServerLoadInfo(invokers.get(index));
            AtomicInteger limiter = UserLoadBalanceService.getAtomicInteger(invoker);
            
            if(serverLoadInfo != null){
                int permits = limiter.get();
                if(permits > 0 ){
                    //loadInfo  = loadInfo+index+","+permits+":";
                    hasPermitArr.add(index);
                    totalWeight = totalWeight+serverLoadInfo.getWeight();
                }
                
            }
            
        }
        //System.out.println(System.currentTimeMillis()+"-"+loadInfo);
        // 服务都被打满了,随机选一个
        if(hasPermitArr.size() == 0){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String nowStr = sdf.format(new Date());
            System.out.println(nowStr+",服务器满负载");
            return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        }
        // 根据服务端配置和平均耗时计算权重
        int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        
        for(int i=0;i<hasPermitArr.size();i++){
            int index = hasPermitArr.get(i);
            ServerLoadInfo serverLoadInfo = UserLoadBalanceService.getServerLoadInfo(invokers.get(index));
            int currentWeight = serverLoadInfo.getWeight();
            offsetWeight  = offsetWeight - currentWeight;
            if (offsetWeight < 0) {
                return invokers.get(index);
            }
        }
        
        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }

//    @Override
//    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
//        
//        int size = invokers.size();
//        int avgSpendMaxIndex = 0;
//        int avgSpendMaxTime = -1;
//        // 总权重
//        int totalWeight = 0;
//        int serverCallbackCount = 0;
//        // 首先获取invoker对应的服务端耗时最大的索引
//        for(int index=0;index<size;index++){
//            
//            ServerLoadInfo serverLoadInfo = UserLoadBalanceService.getServerLoadInfo(invokers.get(index));
//            if(serverLoadInfo != null){
//                serverCallbackCount++;
//                if(serverLoadInfo.getAvgSpendTime()>avgSpendMaxTime){
//                    avgSpendMaxTime = serverLoadInfo.getAvgSpendTime();
//                    avgSpendMaxIndex = index;
//                }
//                totalWeight = totalWeight+serverLoadInfo.getWeight();
//            }
//             
//        }
//        // 服务端未推送消息,随机选一个
//        if(serverCallbackCount == 0){
//            return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
//        }
//        // 耗时排名前两位的权重都加1
////        totalWeight = totalWeight+serverCallbackCount - 1;
//        
//        // 根据服务端配置和平均耗时计算权重
//        int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
//        
//        for(int index=0;index<size;index++){
//            ServerLoadInfo serverLoadInfo = UserLoadBalanceService.getServerLoadInfo(invokers.get(index));
//            if(serverLoadInfo != null){
//                int currentWeight = serverLoadInfo.getWeight();
////                if(index != avgSpendMaxIndex){
////                    currentWeight++;
////                }
//                offsetWeight  = offsetWeight - currentWeight;
//                if (offsetWeight < 0) {
//                    return invokers.get(index);
//                }
//            }
//        }
//        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
//    }

}
