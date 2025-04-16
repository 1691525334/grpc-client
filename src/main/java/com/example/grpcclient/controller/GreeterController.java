package com.example.grpcclient.controller;

import com.example.grpcclient.HelloReply;
import com.example.grpcclient.HelloRequest;
import com.example.grpcclient.HelloServiceGrpc;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.function.Supplier;
import io.vavr.control.Try;

import java.time.Duration;

@RestController
public class GreeterController {

    @Autowired
    private DiscoveryClient discoveryClient;

    private CircuitBreaker circuitBreaker;

    public GreeterController() {
        // 初始化熔断器配置
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 失败率50%
                .waitDurationInOpenState(Duration.ofSeconds(5)) // 熔断5秒
                .slidingWindowSize(10)
                .build();

        this.circuitBreaker = CircuitBreaker.of("grpcBreaker", config);
    }



    @GetMapping("/sayHello")
    public String sayHello(@RequestParam String name) {
        // 使用 Nacos 获取服务实例列表
        String grpcServerAddress = discoveryClient.getInstances("grpc-server")
                .stream()
                .findFirst()
                .map(serviceInstance -> serviceInstance.getHost() + ":" + serviceInstance.getPort())
                .orElse("localhost:90901");
        // 创建 gRPC 通道
        ManagedChannel channel = ManagedChannelBuilder.forTarget(grpcServerAddress)
                .usePlaintext() // 使用明文通信，生产环境可以使用 SSL
                .build();

        // 创建 gRPC 客户端 stub
        HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);

        // 构造请求
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        // 包装 gRPC 调用
        Supplier<String> grpcCall = () -> stub.sayHello(request).getMessage();
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, grpcCall);
        String result = Try.ofSupplier(decorated)
                .onFailure(ex -> System.out.println("gRPC 熔断/失败：" + ex.getMessage()))
                .getOrElse("服务暂时不可用，请稍后重试");
        // 关闭通道
        channel.shutdown();

        return result;
    }
}
