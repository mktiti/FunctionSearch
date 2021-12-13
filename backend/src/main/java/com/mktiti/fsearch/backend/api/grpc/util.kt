package com.mktiti.fsearch.backend.api.grpc

import io.grpc.stub.StreamObserver

fun <T : Any> StreamObserver<T>.response(entity: T) {
    onNext(entity)
    onCompleted()
}