package com.ndt.io.androidultrasound;


/**
 *Created by Thien on 12/21/2015.
 */
public interface OnUsChannelListener {

      /**
     * Called for incremental progress updates. 0 - 100 in percentage
     *
     * @param channelId  Id you passed while initiating the channel
     * @param usChannelState progress usChannelState.
     */
    public void onUsProgress(long channelId, UsChannelState usChannelState);
    // just for testing
    //public void onUsProgress(long channelId, int percentDone, String info);




    /**
     * Called when exchange data successfully
     *
     * @param channelId  Id you passed while initiating the channel
     * @param receivedData the data which is received from the peer.
     */
    public void onUsSuccess(long channelId, byte[] receivedData);

    /**
     * When channel failed for some reasons
     *
     * @param channelId  Id you passed while initiating the channel
     * @param exception  Exception which contains details of cause of failure
     */
    public void onUsFailed(long channelId, Exception exception);
}
