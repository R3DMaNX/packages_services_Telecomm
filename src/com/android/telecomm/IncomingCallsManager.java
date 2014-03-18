/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.telecomm;

import android.os.Bundle;
import android.telecomm.CallInfo;
import android.telecomm.CallService;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Used to retrieve details about an incoming call. This is invoked after an incoming call intent.
 */
final class IncomingCallsManager {

    private final Switchboard mSwitchboard;

    /** Maps call ID to the call. */
    private final Map<String, Call> mPendingIncomingCalls = Maps.newHashMap();

    /**
     * Persists the specified parameters.
     *
     * @param switchboard The switchboard.
     */
    IncomingCallsManager(Switchboard switchboard) {
        mSwitchboard = switchboard;
    }

    /**
     * Retrieves details of an incoming call through its associated call service.
     *
     * @param call The call object.
     * @param extras The optional extras passed with the incoming call intent (to be returned to
     *     the call service via {@link CallService#setIncomingCallId(String, android.os.Bundle)}).
     */
    void retrieveIncomingCall(final Call call, Bundle extras) {
        ThreadUtil.checkOnMainThread();
        Log.d(this, "retrieveIncomingCall");

        final String callId = call.getId();
        // Just to be safe, lets make sure we're not already processing this call.
        Preconditions.checkState(!mPendingIncomingCalls.containsKey(callId));

        mPendingIncomingCalls.put(callId, call);

        Runnable errorCallback = new Runnable() {
            @Override public void run() {
                handleFailedIncomingCall(callId);
            }
        };

        call.getCallService().setIncomingCallId(callId, extras, errorCallback);
    }

    /**
     * Notifies the switchboard of a successful incoming call after removing it from the pending
     * list.
     *
     * @param callInfo The details of the call.
     */
    void handleSuccessfulIncomingCall(CallInfo callInfo) {
        ThreadUtil.checkOnMainThread();

        Call call = mPendingIncomingCalls.remove(callInfo.getId());
        if (call != null) {
            Log.d(this, "Incoming call %s found.", call.getId());
            mSwitchboard.handleSuccessfulIncomingCall(call, callInfo);
        }
    }

    /**
     * Notifies switchboard of the failed incoming call after removing it from the pending list.
     *
     * @param callId The ID of the call.
     */
    void handleFailedIncomingCall(String callId) {
        ThreadUtil.checkOnMainThread();

        Call call = mPendingIncomingCalls.remove(callId);
        if (call != null) {
            Log.i(this, "Failed to get details for incoming call %s", call);
            // The call was found still waiting for details. Consider it failed.
            mSwitchboard.handleFailedIncomingCall(call);
        }
    }
}