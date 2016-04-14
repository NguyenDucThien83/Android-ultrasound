package com.ndt.io.androidultrasound;

/**
 *
 */
public interface OnUsReceiveListener {

    /**
     * Called for incremental progress updates. 0 - 100 in percentage
     *
     * @param receiveId  Id you passed while initiating the receiving
     * @param percentDone percentage of signal received
     */
    void onReceiveProgress(long receiveId, int percentDone, String info, UsChannelState usChannelState);

    /**
     * Called when receive successfully
     *
     * @param receiveId Id you passed while initiating the receiving
     * @param recMessage    extra content.
     */
    void onReceiveSuccess(long receiveId, Message recMessage);

    /**
     * When receive failed for some reason
     *
     * @param receiveId Id you passed while initiating the receiving
     * @param exp  Exception which contains details of cause of failure
     */
    void onReceiveFailed(long receiveId, Exception exp);
    void onReceiveTimeout(long receiveId);
    void onReceiveCancel(long receiveId);
}
