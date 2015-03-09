package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.view.View;
import android.widget.ProgressBar;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;

public class ProgressFilter implements ServiceFilter {


    private Activity mActivity;
    private View mProgressBar;

    public ProgressFilter(Activity activity) {
        mActivity = activity;
    }

    @Override
    public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

        final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();


        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.VISIBLE);
            }
        });

        ListenableFuture<ServiceFilterResponse> future = nextServiceFilterCallback.onNext(request);

        Futures.addCallback(future, new FutureCallback<ServiceFilterResponse>() {
            @Override
            public void onFailure(Throwable e) {
                dismissProgressBar();
                resultFuture.setException(e);
            }

            @Override
            public void onSuccess(ServiceFilterResponse response) {
                dismissProgressBar();
                resultFuture.set(response);
            }
        });

        return resultFuture;
    }
    private void dismissProgressBar() {
        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.GONE);
            }
        });
    }
    public void setProgressBar(View mProgressBar) {
        this.mProgressBar = mProgressBar;
    }
}
