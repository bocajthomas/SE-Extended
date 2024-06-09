package me.rhunk.snapenhance.bridge.logger;

import me.rhunk.snapenhance.bridge.logger.BridgeLoggedMessage;
import me.rhunk.snapenhance.bridge.logger.LoggedChatEdit;

interface LoggerInterface {
    /**
     * Get the ids of the messages that are logged
     * @return message ids that are logged
     */
    long[] getLoggedIds(in String[] conversationIds, int limit);

    /**
     * Get the content of a logged message from the database
     */
    @nullable byte[] getMessage(String conversationId, long id);

    /**
     * Add a message to the message logger database if it is not already there
     */
    oneway void addMessage(in BridgeLoggedMessage message);

    /**
     * Delete a message from the message logger database
     */
    oneway void deleteMessage(String conversationId, long id);

    /**
    * Add a story to the message logger database if it is not already there
    */
    boolean addStory(String userId, String url, long postedAt, long createdAt, in byte[] key, in byte[] iv);

    oneway void logTrackerEvent(
        String conversationId,
        String conversationTitle,
        boolean isGroup,
        String username,
        String userId,
        String eventType,
        String data
    );

    List<LoggedChatEdit> getChatEdits(String conversationId, long messageId);
}