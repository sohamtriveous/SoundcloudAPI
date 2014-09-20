package com.soundcloud.utils;

public class Likes {
	
	private Integer id;
    private String created_at;
    private Integer user_id;
    private Integer track_id;
    private Integer timestamp;
    private String body;
    private String uri;
    private User user;

    public Likes(String body, Integer timestamp)
    {
        this.setBody(body);
        this.setTimestamp(timestamp);
    }
    public Likes(String body)
    {
        this(body, null);
    }

    public Integer getId() {
        return id;
    }
    
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
    public String getCreatedAt() {
        return created_at;
    }
    
    public Integer getTrackId() {
        return track_id;
    }

    public void setTimestamp(Integer secs) {
        this.timestamp = secs;
    }
    
    public Integer getTimestamp() {
        return timestamp;
    }
    
    public String getUri() {
        return uri;
    }

    public Integer getUserId() {
        return user_id;
    }
    
    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "Like [id=" + id + ", created_at=" + created_at
                + ", user_id=" + user_id + ", track_id=" + track_id
                + ", timestamp=" + timestamp + ", body=" + body + ", uri="
                + uri + ", user=" + user + "]";
    }
    
}



