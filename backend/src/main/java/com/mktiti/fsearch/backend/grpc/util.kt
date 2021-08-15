package com.mktiti.fsearch.backend.grpc

import io.grpc.stub.StreamObserver

fun <T : Any> StreamObserver<T>.response(entity: T) {
    onNext(entity)
    onCompleted()
}