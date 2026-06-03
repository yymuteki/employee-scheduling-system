package com.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequirementRequest {
    @NotBlank(message = "月份不能为空")
    private String yearMonth;
    @Size(max = 2000, message = "输入内容不能超过2000字")
    private String naturalInput;

    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public String getNaturalInput() { return naturalInput; }
    public void setNaturalInput(String naturalInput) { this.naturalInput = naturalInput; }
}
