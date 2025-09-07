package com.example.real_estate_crm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeveloperAnnouncementRequest {
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    @JsonProperty("scope")
    private String scope; // ALL_COMPANIES, ONE_COMPANY, SPECIFIC_COMPANIES
    
    @JsonProperty("companyId")
    private Long companyId; // For ONE_COMPANY scope
    
    @JsonProperty("companyIds")
    private java.util.List<Long> companyIds; // For SPECIFIC_COMPANIES scope
    
    @JsonProperty("audience")
    private String audience; // ALL_USERS, DIRECTOR_ONLY
    
    // Constructors
    public DeveloperAnnouncementRequest() {}
    
    public DeveloperAnnouncementRequest(String content, String imageUrl, String scope, 
                                      Long companyId, java.util.List<Long> companyIds, String audience) {
        this.content = content;
        this.imageUrl = imageUrl;
        this.scope = scope;
        this.companyId = companyId;
        this.companyIds = companyIds;
        this.audience = audience;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public Long getCompanyId() {
        return companyId;
    }
    
    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
    
    public java.util.List<Long> getCompanyIds() {
        return companyIds;
    }
    
    public void setCompanyIds(java.util.List<Long> companyIds) {
        this.companyIds = companyIds;
    }
    
    public String getAudience() {
        return audience;
    }
    
    public void setAudience(String audience) {
        this.audience = audience;
    }
    
    @Override
    public String toString() {
        return "DeveloperAnnouncementRequest{" +
                "content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", scope='" + scope + '\'' +
                ", companyId=" + companyId +
                ", companyIds=" + companyIds +
                ", audience='" + audience + '\'' +
                '}';
    }
}
