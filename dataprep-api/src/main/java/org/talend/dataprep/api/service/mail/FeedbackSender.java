package org.talend.dataprep.api.service.mail;

/**
 * Sends a feedback to Talend.
 */
public interface FeedbackSender {

    /**
     * Sends a mail with given subject and body.
     * 
     * @param subject A subject to be included in sent email.
     * @param body A body to be included in sent email.
     * @param sender Email adress of sender.
     */
    void send(String subject, String body, String sender);

}
