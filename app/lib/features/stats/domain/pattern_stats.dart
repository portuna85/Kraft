import 'package:freezed_annotation/freezed_annotation.dart';

part 'pattern_stats.freezed.dart';
part 'pattern_stats.g.dart';

@freezed
class PatternStats with _$PatternStats {
  const factory PatternStats({
    required double avgSum,
    required double avgOddCount,
    required Map<String, int> sumRangeDistribution,
    required Map<String, int> oddCountDistribution,
  }) = _PatternStats;

  factory PatternStats.fromJson(Map<String, dynamic> json) =>
      _$PatternStatsFromJson(json);
}
