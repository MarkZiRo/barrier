package com.jeju.barrier.OSRM.controller;


import com.jeju.barrier.OSRM.Service.OpenRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final OpenRouteService routeService;

    // '/api/routes/accessible' 엔드포인트로 GET 요청을 받아서 경로를 계산
    @GetMapping("/walking")
    public Object getAccessibleWalking(
            @RequestParam("start") String start,
            @RequestParam("end") String end) {
        // start와 end 좌표를 "경도,위도" 형식으로 받아 파싱
        return routeService.getAccessibleWalking(start, end);
    }

    @GetMapping("/wheelchair")
    public Object getAccessibleWheelchair(
            @RequestParam("start") String start,
            @RequestParam("end") String end) {
        // start와 end 좌표를 "경도,위도" 형식으로 받아 파싱
        return routeService.getAccessibleWheelchair(start, end);
    }
}
