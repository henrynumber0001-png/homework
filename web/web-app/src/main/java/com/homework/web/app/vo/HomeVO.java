package com.homework.web.app.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HomeVO {
    private List<Map<String, Object>> interviewHotBanks;
    private List<Map<String, Object>> certificationHotBanks;
    private List<Map<String, Object>> interviewHotSections;
    private List<Map<String, Object>> certificationHotSections;
    private List<Map<String, Object>> hotHits;
}
