import 'package:freezed_annotation/freezed_annotation.dart';

part 'frequency_stats.freezed.dart';
part 'frequency_stats.g.dart';

@freezed
class FrequencyStats with _$FrequencyStats {
  const factory FrequencyStats({
    required List<BallFrequency> frequencies,
    int? periodRounds,
  }) = _FrequencyStats;

  factory FrequencyStats.fromJson(Map<String, dynamic> json) =>
      _$FrequencyStatsFromJson(json);
}

@freezed
class BallFrequency with _$BallFrequency {
  const factory BallFrequency({
    required int number,
    required int count,
    required double ratio,
  }) = _BallFrequency;

  factory BallFrequency.fromJson(Map<String, dynamic> json) =>
      _$BallFrequencyFromJson(json);
}
