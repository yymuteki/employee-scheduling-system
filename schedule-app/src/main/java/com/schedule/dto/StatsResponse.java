package com.schedule.dto;

public class StatsResponse {
    private long morningCount;
    private long eveningCount;
    private double avgWorkingDays;
    private int totalEmployees;

    public long getMorningCount() { return morningCount; }
    public void setMorningCount(long morningCount) { this.morningCount = morningCount; }
    public long getEveningCount() { return eveningCount; }
    public void setEveningCount(long eveningCount) { this.eveningCount = eveningCount; }
    public double getAvgWorkingDays() { return avgWorkingDays; }
    public void setAvgWorkingDays(double avgWorkingDays) { this.avgWorkingDays = avgWorkingDays; }
    public int getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
}
