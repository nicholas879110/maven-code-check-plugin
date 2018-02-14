package com.gome.maven.openapi.application.impl;

import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.Condition;

/**
 * @author zhangliewei
 * @date 2018/1/15 16:20
 * @opyright(c) gome inc Gome Co.,LTD
 */
public  class RunnableInfo {
         private final Runnable runnable;
         private final ModalityState modalityState;
         private final Condition<Object> expired;
         private final ActionCallback callback;

        public RunnableInfo( Runnable runnable,
                             ModalityState modalityState,
                             Condition<Object> expired,
                             ActionCallback callback) {
            this.runnable = runnable;
            this.modalityState = modalityState;
            this.expired = expired;
            this.callback = callback;
        }

        @Override

        public String toString() {
            return "[runnable: " + runnable + "; state=" + modalityState + (expired.value(null) ? "; expired" : "")+"] ";
        }

    public Runnable getRunnable() {
        return runnable;
    }

    public ModalityState getModalityState() {
        return modalityState;
    }

    public Condition<Object> getExpired() {
        return expired;
    }

    public ActionCallback getCallback() {
        return callback;
    }
}
