package com.jeju.barrier.data.service;

import com.jeju.barrier.data.dto.AccessibilityDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class AccessibilityService {
    private final GoogleSheetService googleSheetService;

    public AccessibilityService(GoogleSheetService googleSheetService) {
        this.googleSheetService = googleSheetService;
    }

    public List<AccessibilityDTO> getAllData() throws IOException {
        return googleSheetService.getSheetData();
    }
}