package com.schedule.dto;

public class RequirementResponse {
    private Long id;
    private String yearMonth;
    private String naturalInput;
    private String parsedUnavailable;
    private String parsedPreferences;
    private String userName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public String getNaturalInput() { return naturalInput; }
    public void setNaturalInput(String naturalInput) { this.naturalInput = naturalInput; }
    public String getParsedUnavailable() { return parsedUnavailable; }
    public void setParsedUnavailable(String parsedUnavailable) { this.parsedUnavailable = parsedUnavailable; }
    public String getParsedPreferences() { return parsedPreferences; }
    public void setParsedPreferences(String parsedPreferences) { this.parsedPreferences = parsedPreferences; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
