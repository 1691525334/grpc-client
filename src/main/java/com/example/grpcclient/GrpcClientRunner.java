package com.example.grpcclient;

import com.example.grpcclient.HelloReply;
import com.example.grpcclient.HelloRequest;
import com.example.grpcclient.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GrpcClientRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.1.0.1", 9090)
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);

        HelloReply response = stub.sayHello(HelloRequest.newBuilder().setName("王剑舟").build());

        System.out.println("收到服务端响应: " + response.getMessage());

        channel.shutdown();
    }
}
