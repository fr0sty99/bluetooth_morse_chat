package ch.joris.morseapp;

public class MessageListItem {
    String id;
    String messageTime;

    public MessageListItem(String id, String messageTime) {
        this.id = id;
        this.messageTime = messageTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(String messageTime) {
        this.messageTime = messageTime;
    }


}
