package com.xcoder.tasks;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * AsyncTask is a convenience class for running asynchronous tasks.
 * This is java port of google play services Tasks api. This can be used in both android
 * and non-android projects. However, in android , targetSdkVersion should be >= 28.
 *
 * @param <T> The type of the result.
 */
public class AsyncTask<T> {
    public Exception exception;
    public T result;
    public boolean isSuccessful;
    private CompletableFuture<T> future;
    private OnCompleteCallback<T> completeCallback = call -> {};
    private OnErrorCallback errorCallback = e -> {};
    private OnSuccessCallback<T> successCallback = result -> {};

    /**
     * Executes the task asynchronously. This method returns immediately. It takes a callable which
     * if returns the value, the task is considered successful. If the callable throws an exception,
     * the task is considered failed.
     *
     * @param callable the task to execute
     * @param <T>      the type of the result
     * @return the call that can be used to get the result in future
     */
    public static <T> AsyncTask<T> callAsync(Callable<T> callable) {
        AsyncTask<T> call = new AsyncTask<>();
        call.future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10);
                return callable.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).whenComplete((t, throwable) -> {
            if (throwable == null) {
                call.result = t;
                call.isSuccessful = true;
                call.successCallback.onSuccess(t);
            } else {
                call.exception = (Exception) throwable;
                call.isSuccessful = false;
                call.errorCallback.onError((Exception) throwable);
            }
            call.completeCallback.onComplete(call);
        });
        return call;
    }

    /**
     * Joins all the tasks and build a single task that is resultant of all the tasks.
     *
     * @param tasks the tasks to join
     * @return the joined task
     */
    public static AsyncTask<?> allOf(AsyncTask<?>... tasks) {
        AsyncTask<?> task = new AsyncTask<>();
        CompletableFuture<?>[] array = Arrays.stream(tasks)
                .map((Function<AsyncTask<?>, CompletableFuture<?>>) asyncTask -> asyncTask.future)
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(array).whenComplete((unused, throwable) -> {
            task.completeCallback.onComplete(null);
            if (throwable == null) {
                task.isSuccessful = true;
                task.successCallback.onSuccess(null);
            } else {
                task.exception = (Exception) throwable;
                task.isSuccessful = false;
                task.errorCallback.onError((Exception) throwable);
            }
        });
        return task;
    }

    /**
     * Executes the task asynchronously and sequentially one after another. If you have multiple nested tasks,
     * you can use this method to execute them sequentially.
     * @param function the function to execute
     * @param <R> the type of the result
     * @return the call that can be used to get the result in future
     */
    public <R> AsyncTask<R> thenPerform(AsyncFunction<T, R> function) {
        AsyncTask<R> task = new AsyncTask<>();
        task.future = future.thenApplyAsync(t -> {
            try {
                return function.task(t);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).whenComplete((r, throwable) -> {
            if (throwable == null) {
                task.result = r;
                task.isSuccessful = true;
                task.successCallback.onSuccess(r);
            } else {
                task.exception = (Exception) throwable;
                task.isSuccessful = false;
                task.errorCallback.onError((Exception) throwable);
            }
            task.completeCallback.onComplete(task);
        });
        return task;
    }

    /**
     * Returns the result of the task or wait if it is not yet completed.
     *
     * @param timeoutSeconds the maximum time (in seconds) to wait for the task to complete
     * @return The result
     * @throws Exception if the task failed or the timeout was reached.
     */
    public T getResult(int timeoutSeconds) throws Exception {
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
    }


    public AsyncTask<T> setOnSuccessCallback(OnSuccessCallback<T> onSuccessCallback) {
        this.successCallback = onSuccessCallback;
        return this;
    }

    public AsyncTask<T> setOnErrorCallback(OnErrorCallback onErrorCallback) {
        this.errorCallback = onErrorCallback;
        return this;
    }

    public void setOnCompleteCallback(OnCompleteCallback<T> onCompleteCallback) {
        this.completeCallback = onCompleteCallback;
    }


    public interface OnCompleteCallback<T> {
        void onComplete(AsyncTask<T> call);
    }

    public interface OnSuccessCallback<T> {
        void onSuccess(T result);
    }

    public interface OnErrorCallback {
        void onError(Exception e);
    }

    /**
     * A function used to perform an asynchronous task in chain. To be used with {@link #thenPerform(AsyncFunction)} method
     * @param <T> the type of the input
     * @param <R> the type of the output
     */
    @FunctionalInterface
    public interface AsyncFunction<T,R> {
        R task(T t) throws Exception;
    }
}
