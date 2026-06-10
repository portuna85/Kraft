import 'package:freezed_annotation/freezed_annotation.dart';

part 'companion_stats.freezed.dart';
part 'companion_stats.g.dart';

@freezed
class CompanionStats with _$CompanionStats {
  const factory CompanionStats({
    required int targetNumber,
    required List<CompanionEntry> companions,
  }) = _CompanionStats;

  factory CompanionStats.fromJson(Map<String, dynamic> json) =>
      _$CompanionStatsFromJson(json);
}

@freezed
class CompanionEntry with _$CompanionEntry {
  const factory CompanionEntry({
    required int number,
    required int count,
    required double ratio,
  }) = _CompanionEntry;

  factory CompanionEntry.fromJson(Map<String, dynamic> json) =>
      _$CompanionEntryFromJson(json);
}
