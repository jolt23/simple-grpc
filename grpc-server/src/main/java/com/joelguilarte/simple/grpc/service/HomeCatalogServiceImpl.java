package com.joelguilarte.simple.grpc.service;

import com.joelguilarte.simple.grpc.home.catalog.Home;
import com.joelguilarte.simple.grpc.home.catalog.HomeCatalogGrpc;
import com.joelguilarte.simple.grpc.home.catalog.Point;
import com.joelguilarte.simple.grpc.home.catalog.Rectangle;
import io.grpc.stub.StreamObserver;

/**
 * Created by joel on 10/31/16.
 */
public class HomeCatalogServiceImpl extends HomeCatalogGrpc.HomeCatalogImplBase {

    @Override
    public void getHome(Point request, StreamObserver<Home> responseObserver) {

        Home reply = Home.newBuilder().setAddress("2253 SW 133rd CT Miami, FL 33175")
                .setLocation(Point.newBuilder().setLatitude(25).setLongitude(-80).build()).build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void listHomes(Rectangle request, StreamObserver<Home> responseObserver) {

        Home reply = Home.newBuilder().setAddress("2253 SW 133rd CT Miami, FL 33175")
                .setLocation(Point.newBuilder().setLatitude(25).setLongitude(-80).build()).build();

        responseObserver.onNext(reply);
        responseObserver.onNext(reply);
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
