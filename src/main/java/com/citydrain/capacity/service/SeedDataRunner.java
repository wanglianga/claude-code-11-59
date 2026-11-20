package com.citydrain.capacity.service;

import com.citydrain.capacity.domain.Observation;
import com.citydrain.capacity.domain.ParamVersion;
import com.citydrain.capacity.repo.EventRepo;
import com.citydrain.capacity.repo.ExternalRefRepo;
import com.citydrain.capacity.repo.NetworkVersionRepo;
import com.citydrain.capacity.repo.ObservationRepo;
import com.citydrain.capacity.repo.ParamVersionRepo;
import com.citydrain.capacity.repo.SegmentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 种子数据：覆盖全部验收场景（断面局部缩减、淤积测量区间、糙率缺失、暴雨边界变化、
 * 闸门状态冲突、维修记录、检查覆盖/缺陷只读引用）。仅在数据库为空时执行。
 * 时间线：参数版本 2026-01 登记；事件 E1=2026-07-14 暴雨；E2=2026-08-02 暴雨（无水位观测）；
 * 维修验收 2026-09-01。迟到补录在验收验证时实时入库。
 */
@Component
@Profile("!test")
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private static final ZoneOffset CST = ZoneOffset.ofHours(8);

    private final NetworkVersionRepo nvRepo;
    private final SegmentRepo segmentRepo;
    private final ParamVersionRepo paramRepo;
    private final ObservationRepo obsRepo;
    private final EventRepo eventRepo;
    private final ExternalRefRepo extRepo;
    private final boolean seedEnabled;

    public SeedDataRunner(NetworkVersionRepo nvRepo, SegmentRepo segmentRepo,
                          ParamVersionRepo paramRepo, ObservationRepo obsRepo,
                          EventRepo eventRepo, ExternalRefRepo extRepo,
                          @Value("${app.seed.enabled:true}") boolean seedEnabled) {
        this.nvRepo = nvRepo;
        this.segmentRepo = segmentRepo;
        this.paramRepo = paramRepo;
        this.obsRepo = obsRepo;
        this.eventRepo = eventRepo;
        this.extRepo = extRepo;
        this.seedEnabled = seedEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled || !nvRepo.listCodes().isEmpty()) {
            return;
        }
        log.info("写入种子数据...");

        long nvId = nvRepo.insert("NV-2024A", "2024 版竣工管网（主干线 N1→B1）");

        // 管段：主干线 N1→S1→N2→S2→N3→S3→N4→S4→B1(出口)
        long s1 = segmentRepo.insert(nvId, "S1", "N1", "N2", 180, 1000, 12.36, 12.00, 900);
        long s2 = segmentRepo.insert(nvId, "S2", "N2", "N3", 150, 1000, 12.00, 11.70, 900);
        long s3 = segmentRepo.insert(nvId, "S3", "N3", "N4", 200, 1200, 11.70, 11.30, 1400);
        long s4 = segmentRepo.insert(nvId, "S4", "N4", "B1", 160, 1200, 11.30, 10.98, 1400);

        // 参数版本 v1（2026-01-01 起有效，2026-01-05 登记入库）
        OffsetDateTime validFrom = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, CST);
        OffsetDateTime registered = OffsetDateTime.of(2026, 1, 5, 10, 0, 0, 0, CST);
        seedParam(s1, 1, "SURVEY", 990.0, 1000.0, 0.0019, 0.0021, 0.012, 0.014, 0.0, 0.05,
                validFrom, registered, "2026-01 全线测量确认");
        // S2：断面局部缩减（CCTV+声呐确认有效直径 [900,960]mm，名义 1000mm）
        seedParam(s2, 1, "SURVEY", 900.0, 960.0, 0.0019, 0.0021, 0.012, 0.014, 0.05, 0.10,
                validFrom, registered, "断面局部缩减确认（缺陷引用 D-1001 佐证）");
        // S3：淤积测量区间 [0.10,0.20]
        seedParam(s3, 1, "SURVEY", 1180.0, 1200.0, 0.0019, 0.0021, 0.013, 0.015, 0.10, 0.20,
                validFrom, registered, "淤积测量区间 [0.10,0.20]");
        // S4：糙率缺失（内衬资料不全）
        seedParam(s4, 1, "AS_BUILT", 1190.0, 1200.0, 0.0019, 0.0021, null, null, 0.0, 0.05,
                validFrom, registered, "竣工断面；糙率缺失（内衬资料不全）");

        // 水文事件
        long e1 = eventRepo.insert("E-2026-0714-STORM", "2026-07-14 暴雨（有完整水位/流量监测）",
                OffsetDateTime.of(2026, 7, 14, 2, 0, 0, 0, CST),
                OffsetDateTime.of(2026, 7, 14, 8, 0, 0, 0, CST));
        long e2 = eventRepo.insert("E-2026-0802-STORM", "2026-08-02 暴雨（仅降雨，无水位观测）",
                OffsetDateTime.of(2026, 8, 2, 14, 0, 0, 0, CST),
                OffsetDateTime.of(2026, 8, 2, 18, 0, 0, 0, CST));

        // E1 观测（2026-07-14 09:30 入库）
        OffsetDateTime ing1 = OffsetDateTime.of(2026, 7, 14, 9, 30, 0, 0, CST);
        // 降雨 mm/h
        obs("RAINFALL", "BOUNDARY", "B1", 12.0, null, "mm/h", t(2026, 7, 14, 3, 0), ing1);
        obs("RAINFALL", "BOUNDARY", "B1", 28.0, null, "mm/h", t(2026, 7, 14, 4, 0), ing1);
        obs("RAINFALL", "BOUNDARY", "B1", 15.0, null, "mm/h", t(2026, 7, 14, 5, 0), ing1);
        // 下游边界水位（04:00 壅水超过管顶 12.18m）
        obs("DOWNSTREAM_LEVEL", "BOUNDARY", "B1", 11.30, null, "m", t(2026, 7, 14, 3, 0), ing1);
        obs("DOWNSTREAM_LEVEL", "BOUNDARY", "B1", 12.40, null, "m", t(2026, 7, 14, 4, 0), ing1);
        obs("DOWNSTREAM_LEVEL", "BOUNDARY", "B1", 11.90, null, "m", t(2026, 7, 14, 5, 0), ing1);
        // 节点水位（与流量同刻对齐，用于校准）
        level("N1", 12.68, 13.56, 13.21, ing1);
        level("N2", 12.32, 13.20, 12.85, ing1);
        level("N3", 12.02, 12.90, 12.55, ing1);
        level("N4", 11.62, 12.55, 12.20, ing1);
        // 流量（与水位同刻；S2 另有一条 03:25 未对齐流量，校准时应被拒绝）
        flow("S1", 1008, 1023, 993, ing1);
        flow("S2", 785, 809, 791, ing1);
        obs("FLOW", "SEGMENT", "S2", 960.0, null, "L/s", t(2026, 7, 14, 3, 25), ing1);
        flow("S3", 1275, 1228, 1176, ing1);
        flow("S4", 1606, 1060, 1521, ing1);
        // 闸门 G-S3 状态冲突：02:30 OPEN 与 03:10 CLOSED
        obs("GATE_STATE", "GATE", "G-S3", null, "OPEN", null, t(2026, 7, 14, 2, 30), ing1);
        obs("GATE_STATE", "GATE", "G-S3", null, "CLOSED", null, t(2026, 7, 14, 3, 10), ing1);

        // E2 观测（仅降雨，2026-08-02 19:00 入库）→ 演示下游水位缺失扩大区间
        OffsetDateTime ing2 = OffsetDateTime.of(2026, 8, 2, 19, 0, 0, 0, CST);
        obs("RAINFALL", "BOUNDARY", "B1", 35.0, null, "mm/h", t(2026, 8, 2, 15, 0), ing2);

        // 外部只读引用
        extRepo.insert("DEFECT_OBSERVATION", "D-1001", "CCTV检测系统", "CCTV-2026.05", s2,
                OffsetDateTime.of(2026, 5, 20, 10, 0, 0, 0, CST),
                "{\"locationRange\":{\"startM\":45,\"endM\":62},\"category\":\"结构缺陷\",\"grade\":2,"
                        + "\"morphology\":{\"type\":\"变形/局部缩减\",\"reductionPct\":[8,12]},"
                        + "\"observedBy\":\"检测单位A\"}");
        extRepo.insert("INSPECTION_COVERAGE", "C-2001", "检测管理系统", "INS-2026.05", s2,
                OffsetDateTime.of(2026, 5, 18, 9, 0, 0, 0, CST),
                "{\"scope\":\"S2 全管段 CCTV\",\"coveredLengthM\":150,\"inspector\":\"检测单位A\"}");
        extRepo.insert("INSPECTION_COVERAGE", "C-2002", "检测管理系统", "INS-2026.05", s3,
                OffsetDateTime.of(2026, 5, 18, 9, 0, 0, 0, CST),
                "{\"scope\":\"S3 全管段 CCTV\",\"coveredLengthM\":200,\"inspector\":\"检测单位A\"}");
        // 维修：R-3001 有验收且实测可追溯（验收验证时派生新版本）；R-3002 无验收；R-3003 缺 measurementRef
        extRepo.insert("REPAIR_ACTION", "R-3001", "维修管理系统", "REP-2026.09", s2,
                OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, CST),
                "{\"scope\":\"S2 45-62m 不锈钢内衬修复\","
                        + "\"acceptance\":{\"result\":\"ACCEPTED\",\"acceptedAt\":\"2026-09-01T09:00:00+08:00\"},"
                        + "\"measuredChanges\":{\"diameterMm\":[968,985],\"siltRatio\":[0.0,0.02],"
                        + "\"roughness\":[0.011,0.013]},\"measurementRef\":\"POST-REHAB-SURVEY-2026-09\"}");
        extRepo.insert("REPAIR_ACTION", "R-3002", "维修管理系统", "REP-2026.09", s3,
                OffsetDateTime.of(2026, 9, 3, 9, 0, 0, 0, CST),
                "{\"scope\":\"S3 清淤\",\"acceptance\":{\"result\":\"PENDING\"},"
                        + "\"measuredChanges\":{\"siltRatio\":[0.02,0.05]},\"measurementRef\":\"X-1\"}");
        extRepo.insert("REPAIR_ACTION", "R-3003", "维修管理系统", "REP-2026.09", s1,
                OffsetDateTime.of(2026, 9, 5, 9, 0, 0, 0, CST),
                "{\"scope\":\"S1 检查井修复\","
                        + "\"acceptance\":{\"result\":\"ACCEPTED\",\"acceptedAt\":\"2026-09-05T09:00:00+08:00\"},"
                        + "\"measuredChanges\":{\"diameterMm\":[995,1000]}}");

        log.info("种子数据完成: networkVersionId={}, segments=[{},{},{},{}], events=[{},{}]",
                nvId, s1, s2, s3, s4, e1, e2);
    }

    private void seedParam(long segmentId, int versionNo, String source,
                           Double dMin, Double dMax, Double sMin, Double sMax,
                           Double nMin, Double nMax, Double siltMin, Double siltMax,
                           OffsetDateTime validFrom, OffsetDateTime createdAt, String note) {
        paramRepo.insert(new ParamVersion(0, segmentId, versionNo, source, dMin, dMax,
                sMin, sMax, nMin, nMax, siltMin, siltMax, validFrom, null, createdAt,
                "seed", null, note));
    }

    private OffsetDateTime t(int y, int mo, int d, int h, int mi) {
        return OffsetDateTime.of(y, mo, d, h, mi, 0, 0, CST);
    }

    private void level(String node, double v1, double v2, double v3, OffsetDateTime ing) {
        obs("WATER_LEVEL", "NODE", node, v1, null, "m", t(2026, 7, 14, 3, 0), ing);
        obs("WATER_LEVEL", "NODE", node, v2, null, "m", t(2026, 7, 14, 4, 0), ing);
        obs("WATER_LEVEL", "NODE", node, v3, null, "m", t(2026, 7, 14, 5, 0), ing);
    }

    private void flow(String seg, double v1, double v2, double v3, OffsetDateTime ing) {
        obs("FLOW", "SEGMENT", seg, v1, null, "L/s", t(2026, 7, 14, 3, 0), ing);
        obs("FLOW", "SEGMENT", seg, v2, null, "L/s", t(2026, 7, 14, 4, 0), ing);
        obs("FLOW", "SEGMENT", seg, v3, null, "L/s", t(2026, 7, 14, 5, 0), ing);
    }

    private void obs(String type, String targetType, String targetCode, Double valueNum,
                     String valueText, String unit, OffsetDateTime sampledAt, OffsetDateTime ingestedAt) {
        obsRepo.insert(new Observation(0, type, targetType, targetCode, valueNum, valueText,
                unit, sampledAt, ingestedAt, "监测中心", "MON-2026.07", "CALIBRATED"));
    }
}
