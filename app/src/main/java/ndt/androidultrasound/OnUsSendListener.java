package ndt.androidultrasound;


public interface OnUsSendListener {

    /**
     * Called for incremental progress updates. 0 - 100 in percentage
     *
     * @param sendId  Id you passed while initiating the sending
     * @param percentDone percentage of signal sent
     */
    void onSendProgress(long sendId, int percentDone);

    /**
     * Called when sending Task is finish
     *
     * @param sendId Id you passed while initiating the sending
     */
    void onSendFinish(long sendId);

    /**
     * When send failed for some reason
     *
     * @param sendId Id you passed while initiating the sending
     * @param exp  Exception which contains details of cause of failure
     */
    void onSendFailed(long sendId, Exception exp);
}
