package com.mktiti.fsearch.util

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

inline fun <reified T : Any> T.logger(): Logger = LogManager.getLogger(T::class.java)

fun Logger.logTrace(messageSupplier: () -> String) = log(Level.TRACE, messageSupplier)
fun Logger.logTrace(throwable: Throwable, messageSupplier: () -> String) = log(Level.TRACE, messageSupplier, throwable)

fun Logger.logDebug(messageSupplier: () -> String) = log(Level.DEBUG, messageSupplier)
fun Logger.logDebug(throwable: Throwable, messageSupplier: () -> String) = log(Level.DEBUG, messageSupplier, throwable)

fun Logger.logInfo(messageSupplier: () -> String) = log(Level.INFO, messageSupplier)
fun Logger.logInfo(throwable: Throwable, messageSupplier: () -> String) = log(Level.INFO, messageSupplier, throwable)

fun Logger.logWarning(messageSupplier: () -> String) = log(Level.WARN, messageSupplier)
fun Logger.logWarning(throwable: Throwable, messageSupplier: () -> String) = log(Level.WARN, messageSupplier, throwable)

fun Logger.logError(messageSupplier: () -> String) = log(Level.ERROR, messageSupplier)
fun Logger.logError(throwable: Throwable, messageSupplier: () -> String) = log(Level.ERROR, messageSupplier, throwable)