import 'package:freezed_annotation/freezed_annotation.dart';

part 'pattern_stats.freezed.dart';
part 'pattern_stats.g.dart';

@freezed
class PatternStats with _$PatternStats {
  const factory PatternStats({
    required List<OddEvenStat> oddEvenStats,
    required List<SumRangeStat> sumRangeStats,
    required int totalDraws,
  }) = _PatternStats;

  factory PatternStats.fromJson(Map<String, dynamic> json) =>
      _$PatternStatsFromJson(json);
}

@freezed
class OddEvenStat with _$OddEvenStat {
  const factory OddEvenStat({
    required int oddCount,
    required int evenCount,
    required int drawCount,
    required double percent,
    required int maxDrawCount,
    required double theoreticalPercent,
  }) = _OddEvenStat;

  factory OddEvenStat.fromJson(Map<String, dynamic> json) =>
      _$OddEvenStatFromJson(json);
}

@freezed
class SumRangeStat with _$SumRangeStat {
  const factory SumRangeStat({
    required int rangeStart,
    required int rangeEnd,
    required int drawCount,
    required double percent,
    required int maxDrawCount,
    required double theoreticalPercent,
  }) = _SumRangeStat;

  factory SumRangeStat.fromJson(Map<String, dynamic> json) =>
      _$SumRangeStatFromJson(json);
}
