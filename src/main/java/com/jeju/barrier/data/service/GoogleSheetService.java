package com.jeju.barrier.data.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.jeju.barrier.data.dto.AccessibilityDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoogleSheetService {
    private static final String SPREADSHEET_ID = "1DbZ7G7mrWEVPiaA-x7CU-LOV0_huNMFz5o_moGtROQ8";

    private static final String RANGE = "시트1!A2:AI";
    private static final String CREDENTIALS_FILE_PATH = "/gs.json";

    private final Sheets sheetsService;

    public GoogleSheetService() throws GeneralSecurityException, IOException {
        // Google Credentials 로드
        GoogleCredentials credentials;
        try (InputStream is = getClass().getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
        }

        // HTTP 요청에 credentials 추가
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        // Sheets 서비스 생성
        sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Jeju Barrier Free")
                .build();
    }

    public List<AccessibilityDTO> getSheetData() throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, RANGE)
                .execute();

        log.info("Received data from Google Sheets. Row count: {}",
                response.getValues() != null ? response.getValues().size() : 0);


        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        if (!values.isEmpty()) {
            log.info("First row sample: {}", values.get(0));
        }

        return values.stream()
                .map(this::convertRowToDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AccessibilityDTO convertRowToDTO(List<Object> row) {
        if (row.size() < 35) return null; // 필요한 모든 컬럼이 있는지 확인

        AccessibilityDTO dto = new AccessibilityDTO();
        try {
            dto.setId(getStringValue(row, 0));
            dto.setDescription(getStringValue(row, 1));
            dto.setAddress(getStringValue(row, 2));
            dto.setPhone(getStringValue(row, 3));
            dto.setSchedule(getStringValue(row, 4));
            dto.setThumbnails(getStringValue(row, 5));
            dto.setThumb(getStringValue(row, 6));
            dto.setLat(getStringValue(row, 7));
            dto.setLon(getStringValue(row, 8));
            dto.setHints(getStringValue(row, 9));
            dto.setCat(getStringValue(row, 10));

            // BarrierFree 필드 설정
            for (int i = 1; i <= 16; i++) {
                setBarrierFreeField(dto, i, getStringValue(row, 10 + i));
            }

            dto.setSlope(getStringValue(row, 27));
            dto.setSlopeScale(getStringValue(row, 28));
            dto.setElevator(getStringValue(row, 29));
            dto.setToilet(getStringValue(row, 30));
            dto.setParking(getStringValue(row, 31));
            dto.setTable(getStringValue(row, 32));
            dto.setTotal(getStringValue(row, 33));
            dto.setAccessibility(getStringValue(row, 34));
            dto.setTitle(getStringValue(row, 35));

            return dto;
        } catch (Exception e) {
            log.error("Error converting row to DTO: {}", e.getMessage());
            return null;
        }
    }

    private String getStringValue(List<Object> row, int index) {
        try {
            if (row.size() <= index || row.get(index) == null) {
                return "";
            }
            String value = String.valueOf(row.get(index)).trim();
            return value.equals("null") ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }
    private void setBarrierFreeField(AccessibilityDTO dto, int index, String value) {
        try {
            String methodName = "setBarrierFree_" + index;
            dto.getClass().getMethod(methodName, String.class).invoke(dto, value);
        } catch (Exception e) {
            log.error("Error setting barrierFree_{}: {}", index, e.getMessage());
        }
    }
}